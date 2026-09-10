package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.CameraScreen
import com.example.ui.CameraViewModel
import com.example.ui.theme.MyApplicationTheme

/**
 * 教学相机主入口 Activity
 *
 * 【教学点】：
 * 1. 使用 [enableEdgeToEdge] 沉浸式处理系统状态栏与导航栏，给相机取景提供完整的视网膜级全屏体验。
 * 2. 状态提升 (State Hoisting) 到 [CameraViewModel]，利用 AndroidX ViewModel 在配置变更（如屏幕旋转）期间持久化状态。
 */
class MainActivity : ComponentActivity() {

    private val cameraViewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    CameraScreen(viewModel = cameraViewModel)
                }
            }
        }
    }
}

/**
 * 保持兼容单元测试基础组件
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
