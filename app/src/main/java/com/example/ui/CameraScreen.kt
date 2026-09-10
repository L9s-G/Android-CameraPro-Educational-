package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.camera.engine.CameraEngine
import com.example.camera.engine.CameraFrameAnalyzer
import com.example.ui.components.CameraHud
import com.example.ui.components.FocusRing
import com.example.ui.components.HardwareSpecsDialog
import com.example.ui.components.ManualControlPanel
import com.example.ui.components.PhotoPreviewDialog

/**
 * 主相机屏幕 (CameraScreen)
 *
 * 【教学核心知识点：Compose 与底层硬件取景器的深度融合】
 * 1. 使用 [AndroidView] 高性能嵌入 CameraX 的 [PreviewView]。
 * 2. 结合手势检测器 [Modifier.pointerInput]，将屏幕触摸二维坐标转换为对焦测光点。
 * 3. 实现了快门闪烁白光、HUD 专业性能指示器、前后摄平滑无缝切换与参数实时联动。
 */
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var showFlashEffect by remember { mutableStateOf(false) }

    // 初始化相机控制器
    val cameraEngine = remember(lifecycleOwner) {
        CameraEngine(context, lifecycleOwner)
    }

    // 释放资源
    DisposableEffect(cameraEngine) {
        onDispose {
            cameraEngine.release()
        }
    }

    // 动态申请相机运行时权限
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setPermissionGranted(isGranted)
    }

    LaunchedEffect(Unit) {
        val currentPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setPermissionGranted(currentPermission)
    }

    // 监听通知提示
    LaunchedEffect(uiState.userNotice) {
        uiState.userNotice?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotice()
        }
    }

    // 启动/切换相机用例流水线
    LaunchedEffect(
        uiState.hasCameraPermission,
        uiState.lensFacing,
        uiState.captureMode,
        previewViewRef
    ) {
        val pView = previewViewRef
        if (uiState.hasCameraPermission && pView != null) {
            val frameAnalyzer = CameraFrameAnalyzer { fps, luma, latency ->
                viewModel.updateRealtimeMetrics(fps, luma, latency)
            }

            cameraEngine.startCamera(
                previewView = pView,
                lensFacing = uiState.lensFacing,
                captureMode = uiState.captureMode,
                flashMode = uiState.flashMode,
                analyzer = frameAnalyzer,
                onCameraReady = { camera ->
                    viewModel.onCameraBound(camera)
                }
            )
        }
    }

    // 动态同步手动参数
    LaunchedEffect(uiState.zoomRatio) {
        cameraEngine.setZoomRatio(uiState.zoomRatio)
    }

    LaunchedEffect(uiState.exposureIndex) {
        cameraEngine.setExposureCompensation(uiState.exposureIndex)
    }

    LaunchedEffect(uiState.isTorchEnabled) {
        cameraEngine.setTorchEnabled(uiState.isTorchEnabled)
    }

    LaunchedEffect(uiState.flashMode) {
        cameraEngine.setFlashMode(uiState.flashMode)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_screen_root")
    ) {
        if (!uiState.hasCameraPermission) {
            // 权限请求引导界面
            CameraPermissionRationale(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        } else {
            // 1. 底层硬件加速取景器 (SurfaceView 模式)
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        previewViewRef = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camera_preview_view")
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            previewViewRef?.let { pView ->
                                cameraEngine.focusAndMeter(pView, offset.x, offset.y)
                                viewModel.onFocusTapped(offset)
                            }
                        }
                    }
            )

            // 2. 触摸对焦框动画反馈
            uiState.focusPoint?.let { point ->
                FocusRing(point = point)
            }

            // 3. 快门白光瞬闪特效 (视觉捕获反馈)
            AnimatedVisibility(
                visible = showFlashEffect,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.7f))
                )
            }

            // 4. 顶部专业抬头 HUD
            CameraHud(
                uiState = uiState,
                onFlashToggle = { viewModel.cycleFlashMode() },
                onTorchToggle = { viewModel.toggleTorch() },
                onOpenSpecs = { viewModel.toggleSpecsDialog(true) },
                onToggleControls = { viewModel.toggleManualControls(!uiState.showManualControls) },
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.TopCenter)
            )

            // 5. 中间可折叠专业调节面板
            AnimatedVisibility(
                visible = uiState.showManualControls,
                enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 8.dp)
            ) {
                ManualControlPanel(
                    uiState = uiState,
                    onZoomChanged = { viewModel.updateZoomRatio(it) },
                    onExposureChanged = { viewModel.updateExposureIndex(it) },
                    onCaptureModeChanged = { viewModel.setCaptureMode(it) }
                )
            }

            // 6. 底部控制台：照片缩略图、快门触发按钮、前后摄切换
            CameraBottomBar(
                uiState = uiState,
                onCaptureClick = {
                    if (!uiState.isCapturing) {
                        viewModel.setCapturing(true)
                        showFlashEffect = true
                        cameraEngine.capturePhoto(
                            onSuccess = { uri, _ ->
                                showFlashEffect = false
                                viewModel.onPhotoCaptured(uri.toString())
                            },
                            onError = { err ->
                                showFlashEffect = false
                                viewModel.onCaptureFailed(err.message ?: "拍照异常")
                            }
                        )
                    }
                },
                onSwitchCameraClick = { viewModel.toggleCameraFacing() },
                onThumbnailClick = { viewModel.togglePreviewGallery(true) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }

        // 底部 Toast / Snackbar 通知
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        )

        // 硬件能力教学弹窗
        if (uiState.showSpecsDialog) {
            HardwareSpecsDialog(
                specs = uiState.hardwareSpecs,
                currentSpec = uiState.currentHardwareSpec,
                onDismiss = { viewModel.toggleSpecsDialog(false) }
            )
        }

        // 拍照产物大图预览弹窗
        if (uiState.showPreviewGalleryDialog && uiState.lastCapturedPhotoUri != null) {
            PhotoPreviewDialog(
                photoUri = uiState.lastCapturedPhotoUri!!,
                onDismiss = { viewModel.togglePreviewGallery(false) }
            )
        }
    }
}

/**
 * 底部相机操作控制条 (Bottom Bar)
 */
@Composable
private fun CameraBottomBar(
    uiState: com.example.camera.model.CameraUiState,
    onCaptureClick: () -> Unit,
    onSwitchCameraClick: () -> Unit,
    onThumbnailClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // 左侧：最近拍摄缩略图
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFF21262D))
                .border(2.dp, Color(0xFF30363D), CircleShape)
                .clickable(enabled = uiState.lastCapturedPhotoUri != null) {
                    onThumbnailClick()
                }
                .testTag("photo_thumbnail_button"),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.lastCapturedPhotoUri != null) {
                AsyncImage(
                    model = uiState.lastCapturedPhotoUri,
                    contentDescription = "查看上一张高清照片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = Color(0xFF8B949E),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 中间：大型快门按钮
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(80.dp)
                .clip(CircleShape)
                .border(4.dp, Color.White, CircleShape)
                .padding(6.dp)
                .clip(CircleShape)
                .background(if (uiState.isCapturing) Color(0xFFFF5252) else Color.White)
                .clickable(enabled = !uiState.isCapturing) {
                    onCaptureClick()
                }
                .testTag("shutter_button"),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isCapturing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp
                )
            }
        }

        // 右侧：镜头前后切换按钮
        IconButton(
            onClick = onSwitchCameraClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFF21262D).copy(alpha = 0.8f))
                .border(1.dp, Color(0xFF30363D), CircleShape)
                .testTag("switch_camera_button")
        ) {
            Icon(
                imageVector = Icons.Default.FlipCameraAndroid,
                contentDescription = "切换前后置摄像头",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * 权限引导请求视图
 */
@Composable
private fun CameraPermissionRationale(
    onRequestPermission: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0D1117)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "开启相机权限以发挥硬件潜能",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "本项目作为 Android 8+ 相机高性能调优教学演示，需要相机硬件调用权限以探测 HAL3 硬件层级、获取 YUV 实时分析流及高解析度照片捕获。",
                color = Color(0xFF8B949E),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("request_permission_button")
            ) {
                Icon(imageVector = Icons.Default.Security, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "授予相机权限", fontWeight = FontWeight.Bold)
            }
        }
    }
}
