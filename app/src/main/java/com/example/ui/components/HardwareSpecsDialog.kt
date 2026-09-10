package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.camera.model.CameraHardwareInfo
import com.example.camera.model.PhysicalSubCameraInfo

/**
 * 教学诊断弹窗：Android Camera2 / HAL3 硬件特性全景展示
 *
 * 【教学核心价值】：
 * 直观向学生与开发者展示底层 [android.hardware.camera2.CameraCharacteristics]
 * 如何决定了一台设备的相机性能天花板（如是否支持 60fps、RAW 数据输出、全分辨率高速连拍等）。
 *
 * 【进阶突破：解构 Android 逻辑多摄体系】：
 * 清晰展示现代手机“一个公开逻辑相机 (如 ID 0) 背后由超广角、主摄、长焦等多颗物理 Sensor 协同驱动”的真实架构，
 * 解决 3/4 摄手机在普通 API 下“只能看见主摄和前摄”的生态困惑。
 */
@Composable
fun HardwareSpecsDialog(
    specs: List<CameraHardwareInfo>,
    currentSpec: CameraHardwareInfo?,
    onSwitchCamera: (CameraHardwareInfo) -> Unit = {},
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .height(620.dp)
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
                // 弹窗顶部标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
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
                                text = "Camera2 硬件能力与多摄探测",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "HAL3 驱动特性 · 逻辑多摄 · 物理传感器解构",
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

                Spacer(modifier = Modifier.height(8.dp))

                // 安全沙箱提示横幅 (讲解厂商私有 ID 与安全回退机制)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E2638))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "支持点击「切换至此镜头」直接预览物理副摄或私有通道。配备硬件级异常沙箱与自动回退，防崩溃防黑屏。",
                        color = Color(0xFFC5D1DE),
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 设备相机列表卡片
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(specs) { spec ->
                        val isCurrentActive = spec.cameraId == currentSpec?.cameraId
                        CameraSpecCard(
                            spec = spec,
                            isActive = isCurrentActive,
                            onSwitchClick = {
                                onSwitchCamera(spec)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraSpecCard(
    spec: CameraHardwareInfo,
    isActive: Boolean,
    onSwitchClick: () -> Unit
) {
    var isSubCamerasExpanded by remember { mutableStateOf(true) }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ID: ${spec.cameraId} - ${spec.lensFacing}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // 厂商私有隐藏相机标签
                    if (spec.isOemHiddenCamera) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFF9800).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "厂商私有",
                                color = Color(0xFFFFB74D),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 状态徽章与直接切换取景按钮
                if (isActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
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
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // 切换取景试验按钮 (支持安全沙箱与失败自动回滚)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF238636))
                            .clickable { onSwitchClick() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("switch_to_camera_${spec.cameraId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "切换至此镜头",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 智能光学指纹与角色推断横幅 (Smart Optical Heuristic Role)
            if (spec.opticalRole.isNotEmpty()) {
                val roleColor = when {
                    spec.opticalRole.contains("超广角") -> Color(0xFF7C4DFF)
                    spec.opticalRole.contains("长焦") || spec.opticalRole.contains("潜望") -> Color(0xFFFF9800)
                    spec.opticalRole.contains("微距") -> Color(0xFF00E676)
                    spec.isMirrorOfCamera0 -> Color(0xFF00B0FF)
                    else -> Color(0xFF00E5FF)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(roleColor.copy(alpha = 0.12f))
                        .border(1.dp, roleColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = roleColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = spec.opticalRole,
                                color = roleColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (spec.fovDegrees > 0f) {
                            Text(
                                text = "FOV ${spec.fovDegrees}° | 等效 ${spec.equivalentFocalLength35mm}mm",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    if (spec.opticalRoleDescription.isNotEmpty()) {
                        Text(
                            text = spec.opticalRoleDescription,
                            color = Color(0xFFB0BEC5),
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
            }

            // 逻辑多摄专属高阶横幅 (如果包含多颗物理子摄)
            if (spec.isLogicalMultiCamera) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF7C4DFF).copy(alpha = 0.25f), Color(0xFF00E5FF).copy(alpha = 0.12f))
                            )
                        )
                        .border(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = Color(0xFFB388FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "逻辑多摄融合系统 (Logical Multi-Camera)",
                                color = Color(0xFFD1C4E9),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (spec.physicalSubCameras.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { isSubCamerasExpanded = !isSubCamerasExpanded }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSubCamerasExpanded) "收起子镜头" else "展开 (${spec.physicalSubCameras.size})",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 10.sp
                                )
                                Icon(
                                    imageVector = if (isSubCamerasExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "系统向应用暴露单一逻辑节点，底层封装了超广角、主摄、长焦等多颗物理 Sensor，由 HAL3 驱动无缝调度变焦。",
                        color = Color(0xFFB0BEC5),
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
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

            // 物理子镜头解构树 (挂载在逻辑相机下方)
            if (spec.physicalSubCameras.isNotEmpty()) {
                AnimatedVisibility(
                    visible = isSubCamerasExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        ) {
                            Text(
                                text = "↳ 底层真实物理传感器 (Physical Sensors x${spec.physicalSubCameras.size})：",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        spec.physicalSubCameras.forEach { subCamera ->
                            PhysicalSubCameraCard(subCamera)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 物理子镜头展示卡片 (树状缩进微卡片)
 */
@Composable
private fun PhysicalSubCameraCard(sub: PhysicalSubCameraInfo) {
    // 依据镜头类型给予高辨识度徽章色
    val (badgeBg, badgeTextColor) = when {
        sub.lensType.contains("超广角") -> Color(0xFF3F51B5).copy(alpha = 0.35f) to Color(0xFF8C9EFF)
        sub.lensType.contains("主摄") -> Color(0xFF2E7D32).copy(alpha = 0.35f) to Color(0xFF81C784)
        sub.lensType.contains("长焦") -> Color(0xFFE65100).copy(alpha = 0.35f) to Color(0xFFFFB74D)
        else -> Color(0xFF37474F).copy(alpha = 0.4f) to Color(0xFFCFD8DC)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF131922))
            .border(1.dp, Color(0xFF2A3441), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "物理 ID: ${sub.physicalCameraId}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(badgeBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = sub.lensType,
                            color = badgeTextColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 光学等效倍率徽章
                Text(
                    text = "等效 %.1fx".format(sub.opticalZoomEquivalent),
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 参数细节
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "视场角 (FOV): ${sub.fovDegrees}° | 35mm等效: ${sub.equivalentFocalLength35mm}mm",
                    color = Color(0xFF9E9E9E),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${sub.sensorResolutionMp} MP",
                    color = Color(0xFFE0E0E0),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val aperturesStr = if (sub.apertures.isNotEmpty()) {
                    sub.apertures.joinToString("/") { "f/%.1f".format(it) }
                } else "未知光圈"
                Text(
                    text = "阵列: ${sub.activeArraySize} | 光圈: $aperturesStr",
                    color = Color(0xFF78909C),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (sub.supportsOis) {
                    Text(
                        text = "硬件 OIS 防抖",
                        color = Color(0xFF69F0AE),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
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
