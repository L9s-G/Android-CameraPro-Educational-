package com.example.ui.components

import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.camera.model.CameraUiState

/**
 * 专业手动调节面板 (Manual Pro Control Panel)
 *
 * 【UI与交互设计优化】：
 * 1. 采用标准的卡片弹层设计，顶部配有面板标题、说明与显式的【关闭按钮 (X)】，交互明确。
 * 2. 针对竖屏自适应优化空间，控制项垂直紧凑堆叠，避免横向挤压变形。
 * 3. 曝光补偿 (EV)、无极变焦 (Zoom) 与画质策略清晰分层。
 */
@Composable
fun ManualControlPanel(
    uiState: CameraUiState,
    onZoomChanged: (Float) -> Unit,
    onExposureChanged: (Int) -> Unit,
    onCaptureModeChanged: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF141A23).copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 0. 面板头部：标题、副标题与显式关闭按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "手动专业参数调节",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "直连 ISP 曝光与变焦流水线",
                            color = Color(0xFF8B949E),
                            fontSize = 11.sp
                        )
                    }
                }

                // 显式关闭按钮
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF21262D))
                        .testTag("close_manual_panel_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭调节面板",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 分隔线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF21262D))
            )

            // 1. 变焦控制行 (Zoom)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "变焦",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // 变焦快捷档位与当前读数
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1.0f, 2.0f, 5.0f).forEach { preset ->
                            if (preset <= uiState.maxZoomRatio) {
                                val isSelected = kotlin.math.abs(uiState.zoomRatio - preset) < 0.1f
                                OutlinedButton(
                                    onClick = { onZoomChanged(preset) },
                                    modifier = Modifier
                                        .height(26.dp)
                                        .testTag("zoom_preset_${preset.toInt()}x"),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color.Transparent
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF00E5FF) else Color(0xFF38404A)
                                    )
                                ) {
                                    Text(
                                        text = "${preset.toInt()}x",
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White
                                    )
                                }
                            }
                        }

                        Text(
                            text = "%.1fx".format(uiState.zoomRatio),
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }

                Slider(
                    value = uiState.zoomRatio,
                    onValueChange = onZoomChanged,
                    valueRange = uiState.minZoomRatio..uiState.maxZoomRatio.coerceAtLeast(uiState.minZoomRatio + 0.1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color(0xFF2A313C)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .testTag("zoom_slider")
                )
            }

            // 2. 曝光补偿控制行 (Exposure Compensation)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = Color(0xFFFFCA28),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "曝光补偿 (EV)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 归零重置按钮
                        IconButton(
                            onClick = { onExposureChanged(0) },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("exposure_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "重置曝光至 0 EV",
                                tint = Color(0xFF8B949E),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        val ev = uiState.exposureIndex * uiState.exposureStep
                        Text(
                            text = (if (ev >= 0) "+%.1f" else "%.1f").format(ev) + " EV",
                            color = Color(0xFFFFCA28),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (uiState.minExposureIndex < uiState.maxExposureIndex) {
                    Slider(
                        value = uiState.exposureIndex.toFloat(),
                        onValueChange = { onExposureChanged(it.toInt()) },
                        valueRange = uiState.minExposureIndex.toFloat()..uiState.maxExposureIndex.toFloat(),
                        steps = (uiState.maxExposureIndex - uiState.minExposureIndex - 1).coerceAtLeast(0),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFCA28),
                            activeTrackColor = Color(0xFFFFCA28),
                            inactiveTrackColor = Color(0xFF2A313C)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .testTag("exposure_slider")
                    )
                } else {
                    Text(
                        text = "当前传感器固定自动曝光，不支持手动补偿调节",
                        color = Color(0xFF8B949E),
                        fontSize = 11.sp
                    )
                }
            }

            // 3. 画质策略切换 (Capture Mode Policy)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "捕获管线优化策略",
                    color = Color(0xFFC9D1D9),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.captureMode == ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
                        onClick = { onCaptureModeChanged(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY) },
                        label = { Text("最大化画质 (HQ多帧降噪)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF00E5FF),
                            labelColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("capture_mode_quality")
                    )

                    FilterChip(
                        selected = uiState.captureMode == ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                        onClick = { onCaptureModeChanged(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) },
                        label = { Text("极速零延迟 (抓拍)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFB300).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFFFB300),
                            labelColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("capture_mode_latency")
                    )
                }
            }
        }
    }
}
