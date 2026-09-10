package com.example.camera.model

/**
 * 实时图像分析管道高频遥测指标数据实体 (High-Frequency Realtime Metrics)
 *
 * 【架构核心设计模式：高频异构数据流与低频交互状态物理隔离 (Scope Isolation)】
 * 1. 为什么必须独立于 [CameraUiState]：
 *    ImageAnalysis 管道以 30Hz 高频产出帧率与测光信息。如果放入全局单一的 UIState，
 *    在 Jetpack Compose 下会导致每秒整屏触发 30 次无意义的重组（包括快门、前后摄按键、取景器包装层等），
 *    极大地浪费移动设备 CPU/GPU 算力并引发 UI 掉帧发热。
 * 2. 独立后收益：
 *    将高频遥测拆出独立的 StateFlow，仅允许顶部 HUD 局部的文字 Text 节点按需订阅更新，
 *    彻底阻断全屏重组，使 UI 线程重组频率下降 90% 以上。
 */
data class CameraRealtimeMetrics(
    val fps: Int = 0,
    val luma: Int = 0,
    val latencyMs: Long = 0
)
