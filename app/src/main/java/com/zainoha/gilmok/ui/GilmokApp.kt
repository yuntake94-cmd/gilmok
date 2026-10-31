package com.zainoha.gilmok.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zainoha.gilmok.domain.GilmokViewModel
import com.zainoha.gilmok.domain.Screen
import com.zainoha.gilmok.ui.components.TopBar
import com.zainoha.gilmok.ui.screens.DrivingScreen
import com.zainoha.gilmok.ui.screens.HomeScreen
import com.zainoha.gilmok.ui.screens.LogScreen
import com.zainoha.gilmok.ui.screens.ParkedScreen
import com.zainoha.gilmok.ui.screens.QnaScreen
import com.zainoha.gilmok.ui.screens.RestScreen
import com.zainoha.gilmok.ui.theme.G
import kotlin.math.roundToInt

@Composable
fun GilmokApp(vm: GilmokViewModel) {
    val s by vm.ui.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(G.Bg)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                status = s.statusText, clock = s.clock, speedKmh = s.vehicle.speedKmh.roundToInt(),
                gps = true, timeScale = s.timeScale, onLogoClick = { vm.toggleDemo() },
            )
            Box(Modifier.weight(1f)) {
                when (s.screen) {
                    Screen.HOME -> HomeScreen(s, vm)
                    Screen.DRIVING -> DrivingScreen(s, vm)
                    Screen.PARKED -> ParkedScreen(s, vm)
                    Screen.QNA -> QnaScreen(s, vm)
                    Screen.REST -> RestScreen(s, vm)
                    Screen.LOG -> LogScreen(s, vm)
                }
            }
        }

        // 상단 배너 (경유지 추가 등 결과 알림)
        AnimatedVisibility(visible = s.banner != null, modifier = Modifier.align(Alignment.TopCenter).padding(top = 70.dp), enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(G.Mint).padding(horizontal = 26.dp, vertical = 14.dp)) {
                Text(s.banner ?: "", color = G.OnMint, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 숨김 데모 토글 (오른쪽 아래 구석, 반투명)
        if (!s.demoOpen) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(6.dp).alpha(0.35f)
                    .clip(RoundedCornerShape(8.dp)).background(G.CardAlt).clickable { vm.toggleDemo() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) { Text("DEMO", color = G.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
        if (s.demoOpen) {
            Box(Modifier.align(Alignment.CenterEnd).padding(top = 57.dp)) { DemoPanel(s, vm) }
        }
    }
}
