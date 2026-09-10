package com.example.camera.engine

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.Range
import android.util.SizeF
import com.example.camera.model.CameraHardwareInfo
import com.example.camera.model.PhysicalSubCameraInfo
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.roundToInt

/**
 * 相机硬件能力探测与教学分析器 (Camera Hardware Inspector)
 *
 * 【教学核心知识点】
 * 1. 在 Android 5.0 (API 21) 引入 Camera2 架构及 HAL3 (硬件抽象层第 3 版) 后，
 *    Android 设备被划分为不同的硬件支持等级 (Hardware Level)。
 * 2. 逻辑多摄系统 (Logical Multi-Camera, API 28+):
 *    现代多镜头手机（如搭载超广角、主摄、长焦的 3/4 摄手机），厂商通常将多个物理传感器编组
 *    为一个逻辑相机（如 Camera ID "0"），通过 `physicalCameraIds` 挂载各物理传感器。
 * 3. OEM 隐藏独立相机探针：
 *    针对部分厂商未向常规 `cameraIdList` 暴露的特殊硬件 ID，通过盲扫探针进行深度安全探测。
 */
object CameraHardwareInspector {

    /**
     * 遍历并探测当前设备所有可用摄像头的硬件特性（包括逻辑多摄与底层物理子镜头）
     *
     * @param context 应用程序上下文
     * @return 包含所有摄像头详细硬件参数的列表
     */
    fun inspectAllCameras(context: Context): List<CameraHardwareInfo> {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return emptyList()

        val results = mutableListOf<CameraHardwareInfo>()
        val probedIds = mutableSetOf<String>()

        try {
            val publicCameraIds = cameraManager.cameraIdList
            probedIds.addAll(publicCameraIds)

            // 1. 解析常规公开 Camera ID
            for (id in publicCameraIds) {
                try {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    val info = parseCharacteristics(id, chars, cameraManager, isHidden = false, null)
                    results.add(info)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val camera0Info = results.firstOrNull { it.cameraId == "0" }

            // 2. 探针深度探测：发掘厂商可能隐藏的物理相机 ID (0 ~ 9 盲扫)
            // 某些机型将超广角、微距或黑白副摄作为独立非公开 ID 暴露在 HAL 层
            for (candidateId in 0..9) {
                val candidateStr = candidateId.toString()
                if (!probedIds.contains(candidateStr)) {
                    try {
                        val hiddenChars = cameraManager.getCameraCharacteristics(candidateStr)
                        val hiddenInfo = parseCharacteristics(candidateStr, hiddenChars, cameraManager, isHidden = true, camera0Info)
                        results.add(hiddenInfo)
                        probedIds.add(candidateStr)
                    } catch (_: Exception) {
                        // 该 ID 不存在，忽略
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results
    }

    /**
     * 解析单个 CameraCharacteristics 结构体，支持解构逻辑多摄旗下的底层物理 Sensor
     */
    private fun parseCharacteristics(
        id: String,
        chars: CameraCharacteristics,
        cameraManager: CameraManager,
        isHidden: Boolean,
        referenceCamera0: CameraHardwareInfo?
    ): CameraHardwareInfo {
        // 1. 镜头朝向 (Lens Facing)
        val lensFacingCode = chars.get(CameraCharacteristics.LENS_FACING)
        val lensFacingStr = when (lensFacingCode) {
            CameraMetadata.LENS_FACING_BACK -> "后置镜头 (BACK)"
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

        // 4. 数字变焦上限
        val maxDigitalZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f

        // 5. 帧率区间
        val fpsRanges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) ?: emptyArray()
        var maxFps = 30
        val fpsRangeDescriptions = fpsRanges.map { range: Range<Int> ->
            if (range.upper > maxFps) maxFps = range.upper
            "[${range.lower} ~ ${range.upper} fps]"
        }

        // 6. 闪光灯单元
        val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

        // 7. 光学防抖支持
        val oisModes = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
        val supportsOis = oisModes?.contains(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON) ?: false

        // 8. 扩展能力能力集
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

        // 10. 曝光补偿
        val expRange = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
        val expStep = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
        val expStr = if (expRange != null && expStep != null) {
            "${expRange.lower} ~ ${expRange.upper} (步长: ${expStep.numerator}/${expStep.denominator} EV)"
        } else {
            "不支持"
        }

        // 11. 物理焦距与光学视场角 (FOV & 35mm Equivalent Focal Length)
        val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()
        val primaryFocal = focalLengths.firstOrNull() ?: 4.0f
        val sensorPhysicalSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: SizeF(5.0f, 3.75f)

        // 水平 FOV = 2 * arctan(sensorWidth / (2 * focalLength))
        val hFovRad = 2.0 * atan((sensorPhysicalSize.width / (2.0 * primaryFocal)).toDouble())
        val fovDeg = (Math.toDegrees(hFovRad).toFloat() * 10).roundToInt() / 10f

        // 35mm 全画幅传感器标准宽 36mm
        val eqFocal = if (sensorPhysicalSize.width > 0) {
            (primaryFocal * (36.0f / sensorPhysicalSize.width) * 10).roundToInt() / 10f
        } else 26.0f

        // 12. 最小对焦距离 (屈光度: 1/米) 与对焦能力推断微距镜头
        val minFocusDistance = chars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0.0f

        // 13. 与 ID 0 硬件指纹比对 (解决厂商私有 ID 是主摄 Sensor 裸映射镜像的问题)
        var isMirror = false
        if (referenceCamera0 != null && id != "0") {
            val sameResolution = abs(referenceCamera0.sensorResolutionMp - sensorResolutionMp) < 0.2f
            val sameArray = referenceCamera0.activeArraySize == activeArraySizeStr
            val sameFocal = if (referenceCamera0.focalLengths.isNotEmpty() && focalLengths.isNotEmpty()) {
                abs(referenceCamera0.focalLengths.first() - focalLengths.first()) < 0.1f
            } else false

            if (sameResolution && (sameArray || sameFocal)) {
                isMirror = true
            }
        }

        // 14. 智能光学角色推断 (Smart Optical Heuristics)
        val (roleTitle, roleDesc) = when {
            lensFacingCode == CameraMetadata.LENS_FACING_FRONT -> {
                "前置自拍摄像头" to "用于自拍与人脸识别，广角定焦/人像优化镜头"
            }
            isMirror -> {
                "主摄硬件直通镜像 (Raw Direct)" to "参数与系统主摄(ID 0)高度吻合。通常是绕过系统算法管道的裸Sensor硬件映射，常用于产线校准或底座专用算子"
            }
            eqFocal < 20f || fovDeg > 95f -> {
                "超广角镜头 (Ultra-Wide)" to "等效约 ${eqFocal}mm, FOV达 ${fovDeg}°。带来震撼大视野，适合风景与大合影"
            }
            eqFocal in 45f..80f -> {
                "长焦人像镜头 (Telephoto 2x~3x)" to "等效约 ${eqFocal}mm。具备光学望远放大能力，兼具人像浅景深虚化压缩感"
            }
            eqFocal > 80f -> {
                "潜望超长焦镜头 (Periscope Telephoto)" to "等效高达 ${eqFocal}mm。折叠光路超远距离拍摄镜头"
            }
            minFocusDistance >= 25f || sensorResolutionMp in 1.8f..5.2f && primaryFocal < 3.0f -> {
                "微距微摄镜头 (Macro Lens)" to "超短对焦距离，适合微观特写拍摄"
            }
            minFocusDistance == 0.0f && sensorResolutionMp in 1.5f..2.5f -> {
                "景深测距/虚化副摄 (Depth Sensor)" to "辅助计算双摄视差景深信息，通常不直接参与成像画面输出"
            }
            else -> {
                "广角主摄/标准相机 (Wide Main)" to "等效约 ${eqFocal}mm，日常最核心的高解析力取景主通道"
            }
        }

        // 15. 逻辑多摄与物理子摄像头解构 (Logical Multi-Camera 解码)
        var isMultiCamera = false
        val physicalSubCameras = mutableListOf<PhysicalSubCameraInfo>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val isLogicalCapability = capabilities.contains(
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
            )
            val physicalCameraIds = chars.physicalCameraIds
            if (isLogicalCapability || physicalCameraIds.isNotEmpty()) {
                isMultiCamera = true
                for (physId in physicalCameraIds) {
                    try {
                        val physChars = cameraManager.getCameraCharacteristics(physId)
                        val subInfo = parsePhysicalSubCamera(physId, physChars)
                        physicalSubCameras.add(subInfo)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // 依等效焦距由小到大排序 (超广角 -> 主摄 -> 长焦)
        physicalSubCameras.sortBy { it.equivalentFocalLength35mm }

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
            focalLengths = focalLengths,
            isLogicalMultiCamera = isMultiCamera,
            isOemHiddenCamera = isHidden,
            physicalSubCameras = physicalSubCameras,
            opticalRole = roleTitle,
            opticalRoleDescription = roleDesc,
            equivalentFocalLength35mm = eqFocal,
            fovDegrees = fovDeg,
            isMirrorOfCamera0 = isMirror
        )
    }

    /**
     * 解析单个物理子相机的硬件与光学参数 (焦距、视场角 FOV、等效 35mm 焦距、镜头类型推断)
     */
    private fun parsePhysicalSubCamera(physId: String, chars: CameraCharacteristics): PhysicalSubCameraInfo {
        val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()
        val primaryFocal = focalLengths.firstOrNull() ?: 4.0f

        val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: SizeF(5.0f, 3.75f)
        val pixelArraySize = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val activeArray = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)

        val resolutionMp = if (pixelArraySize != null) {
            ((pixelArraySize.width.toLong() * pixelArraySize.height.toLong()) / 1_000_000f * 10).roundToInt() / 10f
        } else 0f

        val activeArrayStr = if (activeArray != null) {
            "${activeArray.width()} x ${activeArray.height()}"
        } else {
            pixelArraySize?.let { "${it.width} x ${it.height}" } ?: "未知"
        }

        // 光圈
        val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.toList() ?: emptyList()

        // OIS
        val oisModes = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
        val hasOis = oisModes?.contains(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON) ?: false

        // 计算水平 FOV 与 35mm 等效焦距
        // 水平 FOV = 2 * arctan(sensorWidth / (2 * focalLength))
        val hFovRad = 2.0 * atan((sensorSize.width / (2.0 * primaryFocal)).toDouble())
        val fovDeg = Math.toDegrees(hFovRad).toFloat()

        // 35mm 全画幅传感器标准宽度为 36mm: eqFocal = primaryFocal * (36.0 / sensorSize.width)
        val eqFocal = if (sensorSize.width > 0) {
            (primaryFocal * (36.0f / sensorSize.width) * 10).roundToInt() / 10f
        } else 26.0f

        // 光学镜头类型与等效变焦倍率推断
        val (lensType, zoomEquivalent) = when {
            eqFocal < 20f || fovDeg > 95f -> "超广角镜头 (Ultra-Wide)" to 0.6f
            eqFocal in 20f..35f -> "广角主摄 (Wide Main)" to 1.0f
            eqFocal in 36f..75f -> "中长焦镜头 (Telephoto 2x~3x)" to 2.0f
            eqFocal > 75f -> "潜望超长焦 (Periscope Telephoto)" to 5.0f
            else -> "辅助/景深镜头 (Auxiliary)" to 1.0f
        }

        return PhysicalSubCameraInfo(
            physicalCameraId = physId,
            lensType = lensType,
            opticalZoomEquivalent = zoomEquivalent,
            sensorResolutionMp = resolutionMp,
            activeArraySize = activeArrayStr,
            focalLengths = focalLengths,
            equivalentFocalLength35mm = eqFocal,
            fovDegrees = (fovDeg * 10).roundToInt() / 10f,
            apertures = apertures,
            supportsOis = hasOis
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

