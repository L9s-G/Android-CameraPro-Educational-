package com.example.camera.model

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.ui.geometry.Offset

/**
 * 教学相机 UI 状态实体 (UI State)
 *
 * 遵循现代 Android MVI / MVVM 单向数据流架构，集中管理相机界面与管道状态。
 */
data class CameraUiState(
    // 权限状态
    val hasCameraPermission: Boolean = false,

    // 镜头与闪光控制
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val isTorchEnabled: Boolean = false,

    // 变焦控制 (Zoom)
    val zoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 5.0f,

    // 曝光补偿 (EV Compensation)
    val exposureIndex: Int = 0,
    val minExposureIndex: Int = -4,
    val maxExposureIndex: Int = 4,
    val exposureStep: Float = 0.5f,

    // 拍照模式策略 (高画质 vs 低延迟)
    val captureMode: Int = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,

    // 硬件探测信息
    val hardwareSpecs: List<CameraHardwareInfo> = emptyList(),
    val currentHardwareSpec: CameraHardwareInfo? = null,

    // 拍照与产物状态
    val isCapturing: Boolean = false,
    val lastCapturedPhotoUri: String? = null,
    val userNotice: String? = null,

    // 交互弹窗与面板
    val showSpecsDialog: Boolean = false,
    val showManualControls: Boolean = false,
    val showPreviewGalleryDialog: Boolean = false,

    // 对焦点视觉反馈坐标 (取景器内触摸点)
    val focusPoint: Offset? = null
)
