package com.zainoha.gilmok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zainoha.gilmok.domain.GilmokViewModel
import com.zainoha.gilmok.ui.GilmokApp
import com.zainoha.gilmok.ui.theme.GilmokTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 차량 디스플레이처럼 시스템 바 없이 전체 화면
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            GilmokTheme {
                val vm: GilmokViewModel = viewModel()
                GilmokApp(vm)
            }
        }
    }
}
