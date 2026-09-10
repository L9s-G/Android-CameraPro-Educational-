package com.example.camera.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 教学级相机引擎控制器 (CameraEngine)
 *
 * 【教学核心知识点：CameraX 现代相机流水线架构】
 * 1. 【生命周期绑定 (Lifecycle Binding)】：
 *    通过 [ProcessCameraProvider.bindToLifecycle]，CameraX 会自动响应宿主 Activity/Fragment 的生命周期。
 *    在后台自动切断传感器数据流释放电源，在前台瞬间恢复，从根本上消除了 Camera2 常见的锁屏/切后台崩溃泄露。
 *
 * 2. 【多用例并发流 (Use Cases Pipeline)】：
 *    将相机数据流划分为三个解耦的独立用例：
 *    - [Preview]：负责渲染实时画面到 [PreviewView]。
 *    - [ImageCapture]：负责高质量拍照，调用传感器最高分辨率与 ISP 优化管线。
 *    - [ImageAnalysis]：负责逐帧算法分析（FPS、测光、CV 检测），完全独立于捕获管道。
 *
 * 3. 【硬件渲染性能：SurfaceView vs TextureView】：
 *    配置 [PreviewView.ImplementationMode.PERFORMANCE] 时，底层采用 SurfaceView，
 *    由硬件合成器 (SurfaceFlinger) 独立图层直接送显，绕过 UI 视图树的绘制消耗，帧率最稳、发热最低。
 */
class CameraEngine(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private val tag = "CameraEngine"

    // 单线程分析调度器，保证图像处理不阻塞主线程
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var previewUseCase: Preview? = null
    private var imageCaptureUseCase: ImageCapture? = null
    private var imageAnalysisUseCase: ImageAnalysis? = null

    // 当前相机的控制接口与信息接口
    val cameraControl: CameraControl?
        get() = camera?.cameraControl

    val cameraInfo: CameraInfo?
        get() = camera?.cameraInfo

    /**
     * 初始化并启动相机流水线
     *
     * @param previewView 取景器视图
     * @param lensFacing 镜头朝向 (后置/前置)
     * @param captureMode 拍照模式 (高画质 / 低延迟)
     * @param flashMode 闪光模式
     * @param analyzer 图像帧分析回调
     * @param onCameraReady 相机启动完成回调（传递曝光与缩放参数范围）
     */
    fun startCamera(
        previewView: PreviewView,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
        captureMode: Int = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
        flashMode: Int = ImageCapture.FLASH_MODE_OFF,
        analyzer: ImageAnalysis.Analyzer,
        onCameraReady: (Camera) -> Unit = {}
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                // 1. 设置取景器硬件性能模式 (优先 SurfaceView 模式保证 60fps 最高流畅度)
                previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE

                // 2. 构建 Preview 用例
                previewUseCase = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                // 3. 构建 ImageCapture 用例（配置画质模式与闪光灯策略）
                imageCaptureUseCase = ImageCapture.Builder()
                    .setCaptureMode(captureMode)
                    .setFlashMode(flashMode)
                    .build()

                // 4. 构建 ImageAnalysis 用例（配置非阻塞背压策略与异步调度器）
                imageAnalysisUseCase = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build().also {
                        it.setAnalyzer(analysisExecutor, analyzer)
                    }

                // 5. 选择目标摄像头
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                // 6. 解绑旧用例并重新绑定到生命周期
                provider.unbindAll()
                val boundCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    previewUseCase,
                    imageCaptureUseCase,
                    imageAnalysisUseCase
                )

                camera = boundCamera
                onCameraReady(boundCamera)
                Log.d(tag, "Camera successfully bound to lifecycle with lensFacing: $lensFacing")

            } catch (e: Exception) {
                Log.e(tag, "Failed to start camera: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * 触摸对焦与测光 (Tap to Focus & Metering)
     *
     * 【教学点】：触发 ISP 的 3A (Auto Focus, Auto Exposure, Auto WhiteBalance) 区域调整。
     * [FocusMeteringAction] 可以设置自动取消对焦的超时时间，保证对焦后恢复连续自动对焦 (CAF)。
     */
    fun focusAndMeter(previewView: PreviewView, x: Float, y: Float) {
        val control = cameraControl ?: return
        try {
            val factory = previewView.meteringPointFactory
            val point = factory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
            )
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()

            control.startFocusAndMetering(action)
        } catch (e: Exception) {
            Log.e(tag, "Error performing tap-to-focus", e)
        }
    }

    /**
     * 调节变焦比例 (Zoom Ratio)
     */
    fun setZoomRatio(ratio: Float) {
        cameraControl?.setZoomRatio(ratio)
    }

    /**
     * 调节曝光补偿指数 (Exposure Compensation Index)
     */
    fun setExposureCompensation(index: Int) {
        cameraControl?.setExposureCompensationIndex(index)
    }

    /**
     * 开关手电筒/常亮补光 (Torch)
     */
    fun setTorchEnabled(enabled: Boolean) {
        cameraControl?.enableTorch(enabled)
    }

    /**
     * 动态设置拍照闪光灯模式
     */
    fun setFlashMode(flashMode: Int) {
        imageCaptureUseCase?.flashMode = flashMode
    }

    /**
     * 执行高解析度静态照片拍照并保存至系统公共 DCIM/Camera 目录
     *
     * 【教学点：现代 Android 分区存储的最佳工程实践】：
     * 1. 采用 MediaStore API 构建输出选项 [ImageCapture.OutputFileOptions]。
     * 2. 显式配置 [MediaStore.Images.Media.RELATIVE_PATH] 为 "DCIM/Camera"，使手机自带相册、
     *    Google Photos 能立即扫描并展示新照片。
     * 3. 在 Android 10+ (API 29+) 标记 IS_PENDING 状态，写入完成后置为 0，防止其他相册读取到未写完的半截文件。
     */
    fun capturePhoto(
        onSuccess: (Uri) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        val capture = imageCaptureUseCase ?: return

        val fileName = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    if (savedUri != null) {
                        // 在 Android 10+ 释放 IS_PENDING 标志位，使相册立即公开索引可见
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val updateValues = ContentValues().apply {
                                put(MediaStore.Images.Media.IS_PENDING, 0)
                            }
                            try {
                                context.contentResolver.update(savedUri, updateValues, null, null)
                            } catch (e: Exception) {
                                Log.w(tag, "Failed to update IS_PENDING: ${e.message}")
                            }
                        }
                        Log.d(tag, "Photo captured successfully to DCIM/Camera: $savedUri")
                        onSuccess(savedUri)
                    } else {
                        onError(ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "保存的 URI 为空", null))
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(tag, "Photo capture failed: ${exception.message}", exception)
                    onError(exception)
                }
            }
        )
    }

    /**
     * 释放后台分析线程池
     */
    fun release() {
        try {
            cameraProvider?.unbindAll()
            analysisExecutor.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
