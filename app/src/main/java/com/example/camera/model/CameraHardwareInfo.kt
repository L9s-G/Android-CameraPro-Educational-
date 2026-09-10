package com.example.camera.model

/**
 * 物理子摄像头专属特性模型 (Physical Sub-Camera Specs)
 *
 * 【教学核心点：物理摄像头 vs 逻辑摄像头】
 * 在 Android 9+ (API 28+) 的逻辑多摄系统 (Logical Multi-Camera) 中，
 * 一颗逻辑主摄底层往往包含多颗独立的物理 Sensor（如超广角、主摄、长焦）。
 *
 * @property physicalCameraId 物理 Sensor 的硬件 ID (如 "2", "3")
 * @property lensType 光学镜头类型 (超广角 / 广角主摄 / 长焦 / 微距辅助)
 * @property opticalZoomEquivalent 光学焦段等效变焦系数 (如 0.6x, 1.0x, 2.0x, 3.0x)
 * @property sensorResolutionMp 物理传感器像素（MP）
 * @property activeArraySize 物理像素阵列尺寸 (如 4000 x 3000)
 * @property focalLengths 物理镜头焦距列表 (mm)
 * @property equivalentFocalLength35mm 等效 35mm 相机焦距 (mm, 便于理解视角)
 * @property fovDegrees 水平视场角 (Field of View in Degrees)
 * @property apertures 可用光圈值 (如 f/1.8, f/2.2)
 * @property supportsOis 是否具备硬件光学防抖
 */
data class PhysicalSubCameraInfo(
    val physicalCameraId: String,
    val lensType: String,
    val opticalZoomEquivalent: Float,
    val sensorResolutionMp: Float,
    val activeArraySize: String,
    val focalLengths: List<Float>,
    val equivalentFocalLength35mm: Float,
    val fovDegrees: Float,
    val apertures: List<Float>,
    val supportsOis: Boolean
)

/**
 * 摄像头硬件与特性数据模型 (Camera Hardware Specifications)
 *
 * 用于教学展示 Android Camera2 HAL3 硬件层级的核心参数与传感器能力。
 * 通过解析 [android.hardware.camera2.CameraCharacteristics] 获得。
 *
 * @property cameraId 相机逻辑/物理 ID (如 "0", "1")
 * @property lensFacing 镜头朝向描述 (后置 BACK / 前置 FRONT)
 * @property hardwareLevel 硬件支持级别描述 (LEVEL_3, FULL, LIMITED, LEGACY)
 * @property hardwareLevelDescription 硬件级别对开发者的教学含义
 * @property sensorResolutionMp 传感器有效物理像素（百万像素 MP）
 * @property activeArraySize 传感器活动像素阵列尺寸 (如 4032 x 3024)
 * @property maxDigitalZoom 支持的最大数字变焦倍数
 * @property supportedFpsRanges 支持的自动曝光目标帧率区间 (AE Target FPS Ranges)
 * @property maxSupportedFps 支持的最高预览/捕获帧率 (如 30fps 或 60fps)
 * @property hasFlashUnit 是否拥有物理闪光灯硬件
 * @property supportsOis 是否支持光学防抖 (Optical Image Stabilization)
 * @property supportsManualSensor 是否支持全手动传感器控制 (MANUAL_SENSOR capability)
 * @property supportsRaw 是否具备 RAW 原生传感器格式捕获能力
 * @property isoRange ISO 感光度可调范围
 * @property exposureCompensationRange 曝光补偿区间及步长
 * @property focalLengths 物理镜头焦距 (mm)
 * @property isLogicalMultiCamera 是否为包含多颗物理摄像头的逻辑多摄系统
 * @property isOemHiddenCamera 是否为厂商隐藏未公开的独立摄像头 (探针发现)
 * @property physicalSubCameras 隶属于该逻辑摄像头的物理子镜头列表 (超广角/长焦等)
 */
data class CameraHardwareInfo(
    val cameraId: String,
    val lensFacing: String,
    val hardwareLevel: String,
    val hardwareLevelDescription: String,
    val sensorResolutionMp: Float,
    val activeArraySize: String,
    val maxDigitalZoom: Float,
    val supportedFpsRanges: List<String>,
    val maxSupportedFps: Int,
    val hasFlashUnit: Boolean,
    val supportsOis: Boolean,
    val supportsManualSensor: Boolean,
    val supportsRaw: Boolean,
    val isoRange: String,
    val exposureCompensationRange: String,
    val focalLengths: List<Float>,
    val isLogicalMultiCamera: Boolean = false,
    val isOemHiddenCamera: Boolean = false,
    val physicalSubCameras: List<PhysicalSubCameraInfo> = emptyList(),
    // 智能光学角色推断与指纹比对 (Smart Optical Heuristic & Fingerprinting)
    val opticalRole: String = "",
    val opticalRoleDescription: String = "",
    val equivalentFocalLength35mm: Float = 0f,
    val fovDegrees: Float = 0f,
    val isMirrorOfCamera0: Boolean = false
)
