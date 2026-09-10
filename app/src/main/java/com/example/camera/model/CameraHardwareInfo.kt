package com.example.camera.model

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
    val focalLengths: List<Float>
)
