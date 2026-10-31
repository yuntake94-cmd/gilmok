package com.zainoha.gilmok.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zainoha.gilmok.content.RouteData
import com.zainoha.gilmok.domain.GilmokViewModel
import com.zainoha.gilmok.domain.UiState
import com.zainoha.gilmok.ui.components.BigButton
import com.zainoha.gilmok.ui.theme.G
import kotlin.math.roundToInt

/** 화면 구석의 숨김 토글로 여는 데모 조작 패널. 실제 차량에서는 Vehicle/FusedLocation 신호가 이 역할을 한다. */
@Composable
fun DemoPanel(s: UiState, vm: GilmokViewModel) {
    Column(
        Modifier.width(360.dp).fillMaxHeight().background(Color(0xF2141820)).padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("데모 조작 패널", color = G.Orange, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("닫기 ✕", color = G.Muted, fontSize = 17.sp, modifier = Modifier.clickable { vm.toggleDemo() }.padding(8.dp))
        }
        Text("차량 신호 시뮬레이터 (Vehicle · FusedLocation 대체)", color = G.Muted, fontSize = 13.sp)
        Text("TTS: ${if (s.ttsAvailable) "한국어 준비됨" else "음성 데이터 없음(자막만)"} · 배속 x${s.timeScale}", color = G.Muted, fontSize = 13.sp)

        Spacer(Modifier.height(4.dp))
        Text("속도 ${s.targetSpeed.roundToInt()} km/h (실제 ${s.vehicle.speedKmh.roundToInt()})", color = G.Text, fontSize = 17.sp)
        Slider(
            value = s.targetSpeed, onValueChange = { vm.setSpeed(it) }, valueRange = 0f..110f,
            colors = SliderDefaults.colors(thumbColor = G.Mint, activeTrackColor = G.Mint, inactiveTrackColor = G.CardAlt),
        )
        ToggleRow("정차 (기어 P)", s.isParked && s.started) { vm.setParked(it) }
        ToggleRow("길목 사이 빨리감기 ×60", s.fastForward) { vm.toggleFastForward() }

        Spacer(Modifier.height(4.dp))
        SmallButton("다음 길목으로 점프") { vm.jumpNext() }
        SmallButton("쉼표 제안 지금 띄우기") { vm.forceRest() }
        SmallButton("도착 지점으로 점프") { vm.jumpToArrival() }
        SmallButton("여정 로그 보기") { vm.showLog() }

        Spacer(Modifier.height(4.dp))
        Text("샘플 음성 질문 (STT 대체)", color = G.Text, fontSize = 17.sp)
        RouteData.sampleQuestions.forEach { q -> SmallButton(q, accent = true) { vm.ask(q) } }

        Spacer(Modifier.height(8.dp))
        SmallButton("처음으로 (리셋)") { vm.resetAll() }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = G.Text, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = G.OnMint, checkedTrackColor = G.Mint))
    }
}

@Composable
private fun SmallButton(text: String, accent: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp))
            .background(if (accent) G.MintDim else G.CardAlt).clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Text(text, color = G.Text, fontSize = 16.sp, maxLines = 1) }
}
