package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.camera.model.CameraHardwareInfo
import com.example.camera.model.PhysicalSubCameraInfo

/**
 * 教学诊断弹窗：Android Camera2 / HAL3 硬件特性全景展示（折叠/手风琴列表）
 *
 * 【核心交互体验优化】：
 * 1. 宽度扩展：通过 usePlatformDefaultWidth = false 拓宽弹窗，彻底解决窄屏挤压与文字截断。
 * 2. 层次折叠：外层一屏纵览各镜头，内层通过 CollapseSection 模块化折叠（光学角色、硬件等级、关键规格、物理传感器）。
 */
@Composable
fun HardwareSpecsDialog(
    specs: List<CameraHardwareInfo>,
    currentSpec: CameraHardwareInfo?,
    onSwitchCamera: (CameraHardwareInfo) -> Unit = {},
    onDismiss: () -> Unit
) {
    // 默认展开当前激活的摄像头，其余默认收起以实现一屏纵览
    var expandedCameraIds by remember {
        mutableStateOf(setOfNotNull(currentSpec?.cameraId))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 680.dp)
                .fillMaxHeight(0.88f)
                .testTag("hardware_specs_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF141A23).copy(alpha = 0.98f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth()
            ) {
                // 弹窗顶部标题栏（更精炼的主副标题，杜绝过长折行）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                text = "摄像头探针",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Camera2公开 + 底层探针 (共 ${specs.size} 个通道)",
                                color = Color(0xFF8B949E),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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

                Spacer(modifier = Modifier.height(12.dp))

                // 分隔线
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF21262D))
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 设备相机列表（折叠式卡片）
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(specs) { spec ->
                        val isCurrentActive = spec.cameraId == currentSpec?.cameraId
                        val isExpanded = expandedCameraIds.contains(spec.cameraId)

                        CameraSpecCard(
                            spec = spec,
                            isActive = isCurrentActive,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedCameraIds = if (isExpanded) {
                                    expandedCameraIds - spec.cameraId
                                } else {
                                    expandedCameraIds + spec.cameraId
                                }
                            },
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
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSwitchClick: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "expand_rotation"
    )

    val roleColor = when {
        spec.opticalRole.contains("超广角") -> Color(0xFF7C4DFF)
        spec.opticalRole.contains("长焦") || spec.opticalRole.contains("潜望") -> Color(0xFFFF9800)
        spec.opticalRole.contains("微距") -> Color(0xFF00E676)
        spec.isMirrorOfCamera0 -> Color(0xFF00B0FF)
        spec.lensFacing.contains("FRONT", ignoreCase = true) || spec.lensFacing.contains("前置") -> Color(0xFFFF4081)
        else -> Color(0xFF00E5FF)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF161E2E) else Color(0xFF0F141C)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) Color(0xFF00E5FF).copy(alpha = 0.7f) else Color(0xFF252D38)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // ==================== 折叠卡片头部（始终可见，点击整行展开/收起） ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：镜头标识与核心极简摘要
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ID: ${spec.cameraId} · ${spec.lensFacing}",
                            color = Color.White,
                            fontSize = 13.sp,
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
                                    text = "私有",
                                    color = Color(0xFFFFB74D),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // 折叠态：只保留最核心规格（像素 · 等效焦距 · 帧率 · 防抖），单行清晰展示
                    val coreSummary = buildString {
                        append("${spec.sensorResolutionMp}MP")
                        if (spec.equivalentFocalLength35mm > 0) {
                            append(" · ${spec.equivalentFocalLength35mm}mm")
                        } else if (spec.focalLengths.isNotEmpty()) {
                            append(" · %.1fmm".format(spec.focalLengths.first()))
                        }
                        append(" · ${spec.maxSupportedFps}fps")
                        if (spec.supportsOis) append(" · OIS")
                    }

                    Text(
                        text = coreSummary,
                        color = Color(0xFF8B949E),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 右侧：状态徽章 / 快捷切换按钮 + 旋转折叠指示箭头
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isActive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "取景中",
                                color = Color(0xFF00E5FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // 未激活状态下的快捷一键切换
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF238636))
                                .clickable { onSwitchClick() }
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                                .testTag("quick_switch_to_camera_${spec.cameraId}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "切换",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 旋转折叠/展开指示箭头
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF21262D).copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "收起" else "展开",
                            tint = Color(0xFFC5D1DE),
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(rotationAngle)
                        )
                    }
                }
            }

            // ==================== 展开后的模块化手风琴折叠区（CollapseSection） ====================
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 内部分隔线
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF252D38))
                    )

                    // 1. [CollapseSection] 光学角色 (defaultExpanded = false)
                    val opticalRoleSummary = buildString {
                        if (spec.opticalRole.isNotEmpty()) append(spec.opticalRole)
                        if (spec.fovDegrees > 0f) {
                            if (isNotEmpty()) append(" | ")
                            append("%.0f° FOV".format(spec.fovDegrees))
                        }
                        if (spec.equivalentFocalLength35mm > 0) {
                            if (isNotEmpty()) append(" | ")
                            append("${spec.equivalentFocalLength35mm}mm等效")
                        }
                    }
                    CollapseSection(
                        title = "光学角色",
                        summary = opticalRoleSummary,
                        icon = Icons.Default.Sensors,
                        iconTint = roleColor,
                        defaultExpanded = false
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // 彩色横幅完整内容（三行垂直排版，杜绝左右挤压）
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(roleColor.copy(alpha = 0.12f))
                                    .border(1.dp, roleColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                // 第 1 行：角色名称
                                Text(
                                    text = spec.opticalRole.ifEmpty { "标准镜头" },
                                    color = roleColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // 第 2 行：视角与 35mm 等效焦距
                                if (spec.fovDegrees > 0f || spec.equivalentFocalLength35mm > 0) {
                                    val opticalParams = buildString {
                                        if (spec.fovDegrees > 0f) append("FOV %.0f°".format(spec.fovDegrees))
                                        if (spec.equivalentFocalLength35mm > 0) {
                                            if (isNotEmpty()) append(" · ")
                                            append("35mm等效: ${spec.equivalentFocalLength35mm}mm")
                                        }
                                    }
                                    Text(
                                        text = opticalParams,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                // 第 3 行：详细描述说明
                                if (spec.opticalRoleDescription.isNotEmpty()) {
                                    Text(
                                        text = spec.opticalRoleDescription,
                                        color = Color(0xFFB0BEC5),
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp
                                    )
                                }
                            }

                            // 逻辑多摄融合说明
                            if (spec.isLogicalMultiCamera) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF7C4DFF).copy(alpha = 0.15f))
                                        .border(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "逻辑多摄融合系统 (Logical Multi-Camera)",
                                        color = Color(0xFFD1C4E9),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "底层封装了多颗真实物理 Sensor，由 HAL3 驱动无缝调度变焦。",
                                        color = Color(0xFFB0BEC5),
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. [CollapseSection] 关键规格 (defaultExpanded = false)
                    val specsSummary = buildString {
                        append("${spec.sensorResolutionMp}MP · ${spec.maxSupportedFps}fps")
                        append(if (spec.supportsOis) " · OIS" else " · EIS")
                        append(if (spec.supportsRaw) " · RAW支持" else " · 无RAW")
                    }
                    CollapseSection(
                        title = "关键规格",
                        summary = specsSummary,
                        icon = Icons.Default.Info,
                        iconTint = Color(0xFF00E5FF),
                        defaultExpanded = false
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SpecMetricRow("硬件支持等级", spec.hardwareLevel)
                            SpecMetricRow("物理像素总数", "${spec.sensorResolutionMp} 百万像素 (MP)")
                            SpecMetricRow("有效感光阵列", spec.activeArraySize)
                            SpecMetricRow("支持最高帧率", "${spec.maxSupportedFps} FPS")
                            SpecMetricRow("帧率区间 (AE FPS)", spec.supportedFpsRanges.joinToString(", "))
                            SpecMetricRow("数字变焦上限", "%.1fx".format(spec.maxDigitalZoom))
                            SpecMetricRow("光学防抖 (OIS)", if (spec.supportsOis) "支持 (硬件光学防抖)" else "无 OIS (仅 EIS 电子防抖)")
                            SpecMetricRow("物理闪光灯", if (spec.hasFlashUnit) "配备物理闪光灯" else "无物理闪光灯")
                            SpecMetricRow("全手动控制能力", if (spec.supportsManualSensor) "支持 (可独立控制 ISO 与快门)" else "不支持纯手动控制")
                            SpecMetricRow("RAW 原生捕获", if (spec.supportsRaw) "支持 DNG/RAW 原生数据流" else "不支持 RAW")
                            SpecMetricRow("ISO 感光度范围", spec.isoRange)
                            SpecMetricRow("曝光补偿能力", spec.exposureCompensationRange)
                            if (spec.focalLengths.isNotEmpty()) {
                                SpecMetricRow("物理镜头焦距", spec.focalLengths.joinToString(", ") { "%.2fmm".format(it) })
                            }
                        }
                    }

                    // 4. [CollapseSection] 物理传感器 (仅当存在子摄时渲染，defaultExpanded = false)
                    if (spec.physicalSubCameras.isNotEmpty()) {
                        val subSensorsSummary = "↳ ${spec.physicalSubCameras.size}颗物理传感器 (${spec.physicalSubCameras.joinToString(" + ") { it.lensType }})"
                        CollapseSection(
                            title = "物理传感器",
                            summary = subSensorsSummary,
                            icon = Icons.Default.Layers,
                            iconTint = Color(0xFFB388FF),
                            defaultExpanded = false
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                spec.physicalSubCameras.forEach { subCamera ->
                                    PhysicalSubCameraCard(subCamera)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 模块化内部折叠面板 (CollapseSection)
 */
@Composable
private fun CollapseSection(
    title: String,
    summary: String = "",
    icon: ImageVector? = null,
    iconTint: Color = Color(0xFF00E5FF),
    defaultExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(defaultExpanded) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "collapse_section_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF161B22))
            .border(1.dp, Color(0xFF30363D), RoundedCornerShape(8.dp))
    ) {
        // Section 标题行（点击整行展开/收起）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (summary.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = summary,
                        color = Color(0xFF8B949E),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "收起" else "展开",
                tint = Color(0xFF8B949E),
                modifier = Modifier
                    .size(15.dp)
                    .rotate(rotationAngle)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                content()
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
