package com.example.ui.components

import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
 * 【教学点】：
 * 1. 曝光补偿 (EV)：通过调整底层 AE 目标偏置，控制画面明暗，单位为 EV (Exposure Value)。
 * 2. 变焦 (Zoom)：CameraX 统一管理物理广角/长焦镜头无缝切换与高保真数字裁剪。
 * 3. 画质策略：MAXIMIZE_QUALITY 启用多帧降噪与高动态范围融合；MINIMIZE_LATENCY 优化零延迟连拍。
 */
@Composable
fun ManualControlPanel(
    uiState: CameraUiState,
    onZoomChanged: (Float) -> Unit,
    onExposureChanged: (Int) -> Unit,
    onCaptureModeChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF161B22).copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. 变焦控制行 (Zoom)
            Column {
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
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "变焦 (Zoom)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 当前数值与快捷倍率
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
                                        .height(28.dp)
                                        .testTag("zoom_preset_${preset.toInt()}x"),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color.Transparent
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF00E5FF) else Color(0xFF484F58)
                                    )
                                ) {
                                    Text(
                                        text = "${preset.toInt()}x",
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White
                                    )
                                }
                            }
                        }

                        Text(
                            text = "%.1fx".format(uiState.zoomRatio),
                            color = Color(0xFF00E5FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 4.dp)
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
                        inactiveTrackColor = Color(0xFF30363D)
                    ),
                    modifier = Modifier.testTag("zoom_slider")
                )
            }

            // 2. 曝光补偿控制行 (Exposure Compensation)
            Column {
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
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "曝光补偿 (EV)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
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
                                .size(28.dp)
                                .testTag("exposure_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "重置曝光至 0 EV",
                                tint = Color(0xFF8B949E),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        val ev = uiState.exposureIndex * uiState.exposureStep
                        Text(
                            text = (if (ev >= 0) "+%.1f" else "%.1f").format(ev) + " EV",
                            color = Color(0xFFFFCA28),
                            fontSize = 13.sp,
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
                            inactiveTrackColor = Color(0xFF30363D)
                        ),
                        modifier = Modifier.testTag("exposure_slider")
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "捕获管线优化策略",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.captureMode == ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
                        onClick = { onCaptureModeChanged(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY) },
                        label = { Text("最大化画质", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF00E5FF),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.testTag("capture_mode_quality")
                    )

                    FilterChip(
                        selected = uiState.captureMode == ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                        onClick = { onCaptureModeChanged(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) },
                        label = { Text("极速零延迟", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFB300).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFFFB300),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.testTag("capture_mode_latency")
                    )
                }
            }
        }
    }
}
