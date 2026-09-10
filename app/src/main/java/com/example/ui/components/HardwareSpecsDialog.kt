package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.window.Dialog
import com.example.camera.model.CameraHardwareInfo

/**
 * 教学诊断弹窗：Android Camera2 / HAL3 硬件特性全景展示
 *
 * 【教学核心价值】：
 * 直观向学生与开发者展示底层 [android.hardware.camera2.CameraCharacteristics]
 * 如何决定了一台设备的相机性能天花板（如是否支持 60fps、RAW 数据输出、全分辨率高速连拍等）。
 */
@Composable
fun HardwareSpecsDialog(
    specs: List<CameraHardwareInfo>,
    currentSpec: CameraHardwareInfo?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .height(580.dp)
                .testTag("hardware_specs_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF141A23).copy(alpha = 0.96f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth()
            ) {
                // 弹窗统一顶部栏
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
                                imageVector = Icons.Default.DeveloperBoard,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Camera2 硬件能力探测",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "HAL3 驱动特性与性能天花板解析",
                                color = Color(0xFF8B949E),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // 统一风格圆形关闭按钮
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF21262D))
                            .testTag("close_specs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 分隔线
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF21262D))
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 设备相机列表卡片
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(specs) { spec ->
                        val isCurrentActive = spec.cameraId == currentSpec?.cameraId
                        CameraSpecCard(spec = spec, isActive = isCurrentActive)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraSpecCard(
    spec: CameraHardwareInfo,
    isActive: Boolean
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF161B22) else Color(0xFF0F141C)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) Color(0xFF00E5FF).copy(alpha = 0.6f) else Color(0xFF21262D)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 卡片头部
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ${spec.cameraId} - ${spec.lensFacing}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                if (isActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "当前取景中",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 硬件支持等级 (重点教学说明)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF21262D).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = spec.hardwareLevel,
                    color = Color(0xFFFFD54F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = spec.hardwareLevelDescription,
                    color = Color(0xFFC9D1D9),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // 参数指标网格
            SpecMetricRow("物理像素总数", "${spec.sensorResolutionMp} 百万像素 (MP)")
            SpecMetricRow("有效感光阵列", spec.activeArraySize)
            SpecMetricRow("支持最高帧率", "${spec.maxSupportedFps} FPS")
            SpecMetricRow("帧率区间 (AE FPS)", spec.supportedFpsRanges.joinToString(", "))
            SpecMetricRow("数字变焦上限", "%.1fx".format(spec.maxDigitalZoom))
            SpecMetricRow("光学防抖 (OIS)", if (spec.supportsOis) "支持 (硬件光学防抖)" else "无 OIS (仅 EIS 电子防抖)")
            SpecMetricRow("物理闪光灯", if (spec.hasFlashUnit) "配备物理闪光灯" else "无物理闪光灯")
            SpecMetricRow("全手动控制能力", if (spec.supportsManualSensor) "支持 (可独立控制 ISO 与快门曝光)" else "不支持纯手动控制")
            SpecMetricRow("RAW 原生捕获", if (spec.supportsRaw) "支持 DNG/RAW 原生数据流" else "不支持 RAW")
            SpecMetricRow("ISO 感光度范围", spec.isoRange)
            SpecMetricRow("曝光补偿能力", spec.exposureCompensationRange)
            if (spec.focalLengths.isNotEmpty()) {
                SpecMetricRow("物理镜头焦距", spec.focalLengths.joinToString(", ") { "%.2fmm".format(it) })
            }
        }
    }
}

@Composable
private fun SpecMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = Color(0xFF8B949E),
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = Color(0xFFF0F6FC),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1.3f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
