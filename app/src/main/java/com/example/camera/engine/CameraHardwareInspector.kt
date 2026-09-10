package com.example.camera.engine

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.util.Range
import com.example.camera.model.CameraHardwareInfo
import kotlin.math.roundToInt

/**
 * 相机硬件能力探测与教学分析器 (Camera Hardware Inspector)
 *
 * 【教学核心知识点】
 * 1. 在 Android 5.0 (API 21) 引入 Camera2 架构及 HAL3 (硬件抽象层第 3 版) 后，
 *    Android 设备被划分为不同的硬件支持等级 (Hardware Level)。
 * 2. 只有掌握设备的真实硬件等级和能力边界，才能在不抛出运行时异常的前提下，
 *    最大化挖掘设备摄像头的极限性能（如最高分辨率、RAW采集、高帧率 60fps、OIS 光学防抖等）。
 */
object CameraHardwareInspector {

    /**
     * 遍历并探测当前设备所有可用摄像头的硬件特性
     *
     * @param context 应用程序上下文
     * @return 包含所有摄像头详细硬件参数的列表
     */
    fun inspectAllCameras(context: Context): List<CameraHardwareInfo> {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return emptyList()

        val results = mutableListOf<CameraHardwareInfo>()

        try {
            val cameraIds = cameraManager.cameraIdList
            for (id in cameraIds) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val info = parseCharacteristics(id, chars)
                results.add(info)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results
    }

    /**
     * 解析单个 CameraCharacteristics 结构体
     *
     * 【深入剖析】：CameraCharacteristics 是 Camera2 架构中最重要的数据字典，
     * 包含了传感器物理特性、镜头光学参数以及 ISP 图像信号处理器的能力集。
     */
    private fun parseCharacteristics(id: String, chars: CameraCharacteristics): CameraHardwareInfo {
        // 1. 镜头朝向 (Lens Facing)
        val lensFacingCode = chars.get(CameraCharacteristics.LENS_FACING)
        val lensFacingStr = when (lensFacingCode) {
            CameraMetadata.LENS_FACING_BACK -> "后置主摄 (BACK)"
            CameraMetadata.LENS_FACING_FRONT -> "前置自拍 (FRONT)"
            CameraMetadata.LENS_FACING_EXTERNAL -> "外接摄像头 (EXTERNAL)"
            else -> "未知朝向 (UNKNOWN)"
        }

        // 2. 硬件支持级别 (INFO_SUPPORTED_HARDWARE_LEVEL)
        val hardwareLevelCode = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            ?: CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
        val (levelName, levelDesc) = resolveHardwareLevel(hardwareLevelCode)

        // 3. 传感器物理像素尺寸与百万像素计算
        val pixelArraySize = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val activeArray = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val sensorResolutionMp = if (pixelArraySize != null) {
            ((pixelArraySize.width.toLong() * pixelArraySize.height.toLong()) / 1_000_000f * 10).roundToInt() / 10f
        } else {
            0f
        }
        val activeArraySizeStr = if (activeArray != null) {
            "${activeArray.width()} x ${activeArray.height()}"
        } else {
            pixelArraySize?.let { "${it.width} x ${it.height}" } ?: "未知"
        }

        // 4. 数字变焦上限 (Max Digital Zoom)
        val maxDigitalZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f

        // 5. 帧率区间 (AE Target FPS Ranges)
        val fpsRanges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) ?: emptyArray()
        var maxFps = 30
        val fpsRangeDescriptions = fpsRanges.map { range: Range<Int> ->
            if (range.upper > maxFps) maxFps = range.upper
            "[${range.lower} ~ ${range.upper} fps]"
        }

        // 6. 闪光灯单元
        val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

        // 7. 光学防抖支持 (OIS: Optical Image Stabilization)
        val oisModes = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
        val supportsOis = oisModes?.contains(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON) ?: false

        // 8. 扩展能力能力集 (Capabilities)
        val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
        val supportsManualSensor = capabilities.contains(
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
        )
        val supportsRaw = capabilities.contains(
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW
        )

        // 9. ISO 感光度范围
        val isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val isoRangeStr = if (isoRange != null) {
            "ISO ${isoRange.lower} ~ ${isoRange.upper}"
        } else {
            "自动控制 (不支持手动范围)"
        }

        // 10. 曝光补偿区间与步长
        val expRange = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
        val expStep = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
        val expStr = if (expRange != null && expStep != null) {
            "${expRange.lower} ~ ${expRange.upper} (步长: ${expStep.numerator}/${expStep.denominator} EV)"
        } else {
            "不支持"
        }

        // 11. 物理焦距
        val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()

        return CameraHardwareInfo(
            cameraId = id,
            lensFacing = lensFacingStr,
            hardwareLevel = levelName,
            hardwareLevelDescription = levelDesc,
            sensorResolutionMp = sensorResolutionMp,
            activeArraySize = activeArraySizeStr,
            maxDigitalZoom = maxDigitalZoom,
            supportedFpsRanges = fpsRangeDescriptions,
            maxSupportedFps = maxFps,
            hasFlashUnit = hasFlash,
            supportsOis = supportsOis,
            supportsManualSensor = supportsManualSensor,
            supportsRaw = supportsRaw,
            isoRange = isoRangeStr,
            exposureCompensationRange = expStr,
            focalLengths = focalLengths
        )
    }

    /**
     * 将硬件级别代码转换为便于教学理解的标签与详尽解析
     */
    private fun resolveHardwareLevel(level: Int): Pair<String, String> {
        return when (level) {
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> {
                "LEVEL_3 (专业级极限硬件)" to
                        "设备具备最高级别的 Camera2 支持！支持 RAW 传感器捕获、YUV 零拷贝像素重映射以及任意帧动态流输出。"
            }
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> {
                "FULL (完整标准硬件支持)" to
                        "支持全分辨率 30fps/60fps 连拍、逐帧精确手动参数控制（ISO、曝光时间、对焦距离）以及高性能流水线。"
            }
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> {
                "LIMITED (受限硬件级别)" to
                        "设备具备基本 Camera2 功能，但部分高级控制（如 RAW 格式输出、高分辨率连拍速率）存在硬件限制。"
            }
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> {
                "LEGACY (旧版 Camera1 兼容层)" to
                        "底层仅使用 Camera1 兼容驱动，不支持逐帧参数配置与连续高帧率捕获，性能发挥受限。"
            }
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> {
                "EXTERNAL (外接 USB/UVC 设备)" to
                        "外接即插即用摄像头，特性取决于外接硬件与 USB 传输带宽。"
            }
            else -> {
                "UNKNOWN ($level)" to "未知硬件等级"
            }
        }
    }
}
