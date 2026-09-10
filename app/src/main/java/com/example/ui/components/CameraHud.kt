package com.example.ui.components

import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.model.CameraRealtimeMetrics
import com.example.camera.model.CameraUiState

/**
 * 顶部专业抬头显示器 (Camera HUD - Heads Up Display)
 *
 * 【教学架构精髓：高频遥测状态局部重组隔离】：
 * [metrics] 作为独立的 [CameraRealtimeMetrics] 传入。
 * 当 3~4Hz 测光或帧率更新时，重组范围严格被局限在 CameraHud 内部的遥测 Surface，
 * 完全不波及宿主 CameraScreen、底部操作栏、手势对焦层和取景器包装层。
 */
@Composable
fun CameraHud(
    uiState: CameraUiState,
    metrics: CameraRealtimeMetrics,
    onFlashToggle: () -> Unit,
    onTorchToggle: () -> Unit,
    onOpenSpecs: () -> Unit,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. 上部：遥测性能状态条 (FPS、Luma、EV、质量模式) - 提升到最顶端
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 实时 FPS (以绿色/青色显示流畅度)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (metrics.fps >= 24) Color(0xFF00E676) else Color(0xFFFFAB00)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${metrics.fps} FPS",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Y 通道亮度采样 (隔离刷新)
                Text(
                    text = "Luma: ${metrics.luma}",
                    color = Color(0xFFE0E0E0),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                // 曝光补偿指示
                val evValue = uiState.exposureIndex * uiState.exposureStep
                val evStr = if (evValue >= 0) "+%.1f".format(evValue) else "%.1f".format(evValue)
                Text(
                    text = "EV: $evStr",
                    color = Color(0xFFFFD54F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )

                // 画质策略标签
                Text(
                    text = if (uiState.captureMode == ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY) "HQ画质" else "极速连拍",
                    color = Color(0xFF80D8FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                // 当前激活镜头标识 (带智能角色提示)
                val currentCameraLabel = uiState.currentHardwareSpec?.let { spec ->
                    "ID:${spec.cameraId}" + if (spec.opticalRole.isNotEmpty()) "·${spec.opticalRole.take(4)}" else ""
                } ?: "ID:0"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = currentCameraLabel,
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 2. 下部：四个操作功能按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：闪光与手电筒快捷键
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 闪光灯切换
                IconButton(
                    onClick = onFlashToggle,
                    modifier = Modifier
                        .testTag("flash_toggle_button")
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    val (icon, tint) = when (uiState.flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn to Color(0xFFFFD54F)
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto to Color(0xFF81D4FA)
                        else -> Icons.Default.FlashOff to Color.White.copy(alpha = 0.7f)
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "切换闪光灯模式",
                        tint = tint
                    )
                }

                // 常亮手电筒 (Torch)
                IconButton(
                    onClick = onTorchToggle,
                    modifier = Modifier
                        .testTag("torch_toggle_button")
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (uiState.isTorchEnabled) Color(0xFFFFB300).copy(alpha = 0.3f)
                            else Color.Black.copy(alpha = 0.5f)
                        )
                ) {
                    Icon(
                        imageVector = if (uiState.isTorchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "开关常亮补光",
                        tint = if (uiState.isTorchEnabled) Color(0xFFFFB300) else Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // 右侧：专业参数调节开关 & 教学硬件诊断面板入口
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 专业参数滑块展开开关
                IconButton(
                    onClick = onToggleControls,
                    modifier = Modifier
                        .testTag("manual_controls_toggle")
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (uiState.showManualControls) Color(0xFF00E5FF).copy(alpha = 0.3f)
                            else Color.Black.copy(alpha = 0.5f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "展开手动调节面板",
                        tint = if (uiState.showManualControls) Color(0xFF00E5FF) else Color.White
                    )
                }

                // 硬件教学诊断面板入口
                IconButton(
                    onClick = onOpenSpecs,
                    modifier = Modifier
                        .testTag("hardware_specs_button")
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "查看相机硬件参数与教学解析",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
