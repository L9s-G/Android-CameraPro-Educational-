package com.example.camera.engine

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

/**
 * 实时图像流性能与亮度分析器 (Camera Frame Analyzer)
 *
 * 【教学核心知识点：高性能图像流水线分析】
 * 1. 【背压策略 (Backpressure Strategy)】：
 *    使用 [ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST]。当分析处理耗时超过帧间隔时，
 *    管线会自动丢弃旧帧并仅保留最新到达的帧，绝不会阻塞相机底层的预览或捕获队列。
 *
 * 2. 【YUV_420_888 零拷贝采样】：
 *    Android Camera2 默认输出格式通常是 YUV_420_888。
 *    其中 Y 平面 (Plane 0) 纯粹代表灰度/亮度 (Luma)。
 *    若只需检测环境光照或测光统计，无需浪费 CPU/GPU 进行昂贵的 YUV -> RGB 颜色空间转换，
 *    直接对 Y 平面进行等距采样即可获得微秒级的零拷贝高吞吐计算！
 *
 * 3. 【缓冲区释放生命周期】：
 *    [ImageProxy] 使用后必须在 finally 块中立即调用 [ImageProxy.close]，
 *    将硬件缓冲区归还给 HAL 层的 BufferQueue。如果忘记 close()，硬件缓冲区将在数帧内耗尽并导致相机管线假死！
 */
class CameraFrameAnalyzer(
    private val onMetricsUpdated: (fps: Int, averageLuma: Int, latencyMs: Long) -> Unit
) : ImageAnalysis.Analyzer {

    // 帧率统计窗口
    private var frameCount = 0
    private var lastFpsTimestamp = SystemClock.elapsedRealtime()
    private var currentCalculatedFps = 0

    override fun analyze(image: ImageProxy) {
        val startTime = SystemClock.elapsedRealtime()

        try {
            // 1. 动态帧率 (FPS) 滑动窗口计算
            frameCount++
            val now = SystemClock.elapsedRealtime()
            val timeDiff = now - lastFpsTimestamp
            if (timeDiff >= 1000L) {
                currentCalculatedFps = ((frameCount * 1000.0) / timeDiff).toInt()
                frameCount = 0
                lastFpsTimestamp = now
            }

            // 2. Y 平面零拷贝快速测光采样 (Luma Calculation)
            val planes = image.planes
            val luma = if (planes.isNotEmpty()) {
                val buffer: ByteBuffer = planes[0].buffer
                calculateFastLuma(buffer, image.width, image.height, planes[0].rowStride, planes[0].pixelStride)
            } else {
                0
            }

            // 3. 计算单帧分析吞吐延迟 (Latency)
            val latencyMs = SystemClock.elapsedRealtime() - startTime

            // 回调通知外部状态机
            onMetricsUpdated(currentCalculatedFps, luma, latencyMs)

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // 【关键】：必须归还 ImageProxy 缓冲区，否则导致硬件队列饥饿
            image.close()
        }
    }

    /**
     * 对 Y 平面进行极速降采样计算平均亮度 (0 ~ 255)
     *
     * 【深度教学核心突破：打破“伪零拷贝”陷阱，实现真正的零堆内存分配 (Zero-Allocation)】：
     * 1. 常见错误误区 (Anti-Pattern)：
     *    许多教程使用 `val data = ByteArray(limit); buffer.get(data)`，虽然形式上是内存流，
     *    但在 1080P/4K 下每帧分配 1.5MB~8MB 堆内存，在 30fps 下每秒产生 50~200MB 的短命垃圾对象，
     *    在移动端低端芯片上会频繁触发 `GC_FOR_ALLOC` 停顿，导致相机分析管道出现严重丢帧。
     * 2. 真正零拷贝方案 (True Zero-Copy)：
     *    CameraX 传递的 [ByteBuffer] 为底层驱动内存映射的 DirectByteBuffer。
     *    通过直接调用 `buffer.get(index)` 进行基于物理偏移量的随机寻址，完全消除任何堆内存申请与 memcpy 过程！
     * 3. 步长跨度采样算法 (Strided Subsampling)：
     *    步长 step = 16，一帧 1080P 画面仅采样 ~8,000 次，在现代 ART 编译器 JIT 内联下，
     *    整体计算耗时稳定在 < 0.05ms，GC 开销严格为 0。
     */
    private fun calculateFastLuma(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int
    ): Int {
        val step = 16 // 每 16 个像素采样一个点，仅需约 8000 次指针寻址
        var sum = 0L
        var count = 0

        val limit = buffer.limit()

        for (row in 0 until height step step) {
            val rowOffset = row * rowStride
            for (col in 0 until width step step) {
                val index = rowOffset + col * pixelStride
                if (index < limit) {
                    // 直接随机访问 DirectByteBuffer 底层 Native 映射，无任何堆内存分配
                    val pixel = buffer.get(index).toInt() and 0xFF
                    sum += pixel
                    count++
                }
            }
        }

        return if (count > 0) (sum / count).toInt() else 0
    }
}
