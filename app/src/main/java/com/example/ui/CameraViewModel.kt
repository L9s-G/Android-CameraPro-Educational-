package com.example.ui

import android.app.Application
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.engine.CameraHardwareInspector
import com.example.camera.model.CameraUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 相机业务与状态管理 ViewModel (CameraViewModel)
 *
 * 【教学核心知识点：MVVM 与响应式状态分发】
 * 1. 采用 StateFlow 暴露不可变的 [CameraUiState]，保证状态从单一数据源单向流向 Jetpack Compose UI。
 * 2. 避免将 Context、View 或 CameraControl 强引用持久化在 ViewModel 中，防止 Activity 重建导致内存泄漏。
 */
class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    init {
        loadHardwareCharacteristics()
    }

    /**
     * 异步探测设备所有相机的硬件特性
     */
    fun loadHardwareCharacteristics() {
        viewModelScope.launch {
            try {
                val specs = CameraHardwareInspector.inspectAllCameras(getApplication())
                val current = specs.firstOrNull {
                    if (_uiState.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
                        it.lensFacing.contains("BACK")
                    } else {
                        it.lensFacing.contains("FRONT")
                    }
                } ?: specs.firstOrNull()

                _uiState.update {
                    it.copy(
                        hardwareSpecs = specs,
                        currentHardwareSpec = current
                    )
                }
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error reading hardware characteristics", e)
            }
        }
    }

    /**
     * 更新权限状态
     */
    fun setPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = granted) }
        if (granted) {
            loadHardwareCharacteristics()
        }
    }

    /**
     * 当相机与生命周期绑定成功，同步底层相机的动态曝光与变焦限制
     */
    fun onCameraBound(camera: Camera) {
        val zoomState = camera.cameraInfo.zoomState.value
        val exposureState = camera.cameraInfo.exposureState

        val minZoom = zoomState?.minZoomRatio ?: 1.0f
        val maxZoom = zoomState?.maxZoomRatio ?: 5.0f
        val currentZoom = zoomState?.zoomRatio ?: 1.0f

        val minExp = exposureState.exposureCompensationRange.lower
        val maxExp = exposureState.exposureCompensationRange.upper
        val step = exposureState.exposureCompensationStep.toFloat()
        val currentExp = exposureState.exposureCompensationIndex

        _uiState.update {
            it.copy(
                minZoomRatio = minZoom,
                maxZoomRatio = maxZoom,
                zoomRatio = currentZoom,
                minExposureIndex = minExp,
                maxExposureIndex = maxExp,
                exposureStep = if (step > 0) step else 0.5f,
                exposureIndex = currentExp
            )
        }
    }

    /**
     * 切换前后置摄像头
     */
    fun toggleCameraFacing() {
        val newFacing = if (_uiState.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        val matchingSpec = _uiState.value.hardwareSpecs.firstOrNull {
            if (newFacing == CameraSelector.LENS_FACING_BACK) {
                it.lensFacing.contains("BACK")
            } else {
                it.lensFacing.contains("FRONT")
            }
        }

        _uiState.update {
            it.copy(
                lensFacing = newFacing,
                currentHardwareSpec = matchingSpec,
                isTorchEnabled = false,
                zoomRatio = 1.0f,
                exposureIndex = 0
            )
        }
    }

    /**
     * 切换闪光灯模式 (OFF -> AUTO -> ON)
     */
    fun cycleFlashMode() {
        val nextMode = when (_uiState.value.flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
            else -> ImageCapture.FLASH_MODE_OFF
        }
        _uiState.update { it.copy(flashMode = nextMode) }
    }

    /**
     * 开关手电筒/常亮补光
     */
    fun toggleTorch() {
        val newTorch = !_uiState.value.isTorchEnabled
        _uiState.update { it.copy(isTorchEnabled = newTorch) }
    }

    /**
     * 更新变焦倍率
     */
    fun updateZoomRatio(ratio: Float) {
        val clamped = ratio.coerceIn(_uiState.value.minZoomRatio, _uiState.value.maxZoomRatio)
        _uiState.update { it.copy(zoomRatio = clamped) }
    }

    /**
     * 更新曝光补偿档位
     */
    fun updateExposureIndex(index: Int) {
        val clamped = index.coerceIn(_uiState.value.minExposureIndex, _uiState.value.maxExposureIndex)
        _uiState.update { it.copy(exposureIndex = clamped) }
    }

    /**
     * 切换拍照画质策略 (高画质 vs 低延迟)
     */
    fun setCaptureMode(mode: Int) {
        _uiState.update { it.copy(captureMode = mode) }
    }

    /**
     * 触摸对焦触点触发与视觉框定时消失
     */
    fun onFocusTapped(offset: Offset) {
        _uiState.update { it.copy(focusPoint = offset) }
        viewModelScope.launch {
            delay(2500)
            _uiState.update { current ->
                if (current.focusPoint == offset) current.copy(focusPoint = null) else current
            }
        }
    }

    /**
     * 实时接收分析器汇报的 FPS 与测光信息
     */
    fun updateRealtimeMetrics(fps: Int, luma: Int, latencyMs: Long) {
        _uiState.update {
            it.copy(
                realtimeFps = fps,
                realtimeLuma = luma,
                frameProcessingLatencyMs = latencyMs
            )
        }
    }

    /**
     * 拍照状态控制
     */
    fun setCapturing(capturing: Boolean) {
        _uiState.update { it.copy(isCapturing = capturing) }
    }

    /**
     * 拍照成功回调
     */
    fun onPhotoCaptured(uri: String) {
        _uiState.update {
            it.copy(
                isCapturing = false,
                lastCapturedPhotoUri = uri,
                userNotice = "照片拍摄成功！已保存至应用安全沙箱"
            )
        }
    }

    /**
     * 拍照失败回调
     */
    fun onCaptureFailed(errorMsg: String) {
        _uiState.update {
            it.copy(
                isCapturing = false,
                userNotice = "拍照失败: $errorMsg"
            )
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }

    fun toggleSpecsDialog(show: Boolean) {
        _uiState.update { it.copy(showSpecsDialog = show) }
    }

    fun toggleManualControls(show: Boolean) {
        _uiState.update { it.copy(showManualControls = show) }
    }

    fun togglePreviewGallery(show: Boolean) {
        _uiState.update { it.copy(showPreviewGalleryDialog = show) }
    }
}
