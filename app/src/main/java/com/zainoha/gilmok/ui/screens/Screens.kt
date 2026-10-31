package com.zainoha.gilmok.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zainoha.gilmok.content.RouteData
import com.zainoha.gilmok.domain.GilmokViewModel
import com.zainoha.gilmok.domain.TourTheme
import com.zainoha.gilmok.domain.UiState
import com.zainoha.gilmok.ui.components.BigButton
import com.zainoha.gilmok.ui.components.DirectionBadge
import com.zainoha.gilmok.ui.components.GCard
import com.zainoha.gilmok.ui.components.IconButtonBig
import com.zainoha.gilmok.ui.components.LandmarkArt
import com.zainoha.gilmok.ui.components.ListeningOrb
import com.zainoha.gilmok.ui.components.MicIcon
import com.zainoha.gilmok.ui.components.NarrationBar
import com.zainoha.gilmok.ui.components.ReplayIcon
import com.zainoha.gilmok.ui.components.RouteMap
import com.zainoha.gilmok.ui.components.SkipIcon
import com.zainoha.gilmok.ui.theme.G
import kotlin.math.roundToInt

private val landmarkPins = RouteData.landmarks.map { it.name to it.anchor }

@Composable
private fun MiniMap(s: UiState, vm: GilmokViewModel, modifier: Modifier, labels: Boolean = false) {
    RouteMap(
        points = s.route.routePoints,
        cumulativeM = vm.cumulativeM,
        traveledM = s.route.traveledM,
        current = s.fix.point,
        headingDeg = s.fix.headingDeg,
        landmarks = landmarkPins,
        landmarkDistM = RouteData.landmarks.associate { it.name to vm.landmarkDistM.getValue(it.id) },
        waypoints = s.route.waypoints.map { it.point },
        modifier = modifier,
        showLabels = labels,
    )
}

// ------------------------------------------------------------------ 01 홈

@Composable
fun HomeScreen(s: UiState, vm: GilmokViewModel) {
    Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        GCard(Modifier.weight(1.6f).fillMaxHeight()) {
            Text("오늘의 경로", color = G.Muted, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text("부산 해운대 → 거제 외도", color = G.Text, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            val km = (s.route.totalM / 1000).roundToInt()
            Text("경로 길목 ${RouteData.landmarks.size}곳 · ${km}km · 추천 쉼표 ${RouteData.restStops.size - 1}곳", color = G.Muted, fontSize = 21.sp)
            Spacer(Modifier.height(22.dp))
            TourTheme.values().forEach { t ->
                val sel = s.theme == t
                Row(
                    Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(18.dp))
                        .background(if (sel) G.CardAlt else G.CardAlt.copy(alpha = 0.55f))
                        .clickable { vm.setTheme(t) }.padding(horizontal = 26.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(t.label, color = if (sel) G.Text else G.Muted, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(t.desc, color = G.Muted, fontSize = 18.sp)
                    }
                    if (sel) Box(Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)).background(G.Mint), contentAlignment = Alignment.Center) {
                        Text("✓", color = G.OnMint, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
        GCard(Modifier.weight(1f).fillMaxHeight()) {
            Text("경로 미리보기 (NaviHelper)", color = G.Muted, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(G.CardAlt)) {
                MiniMap(s, vm, Modifier.fillMaxSize(), labels = true)
            }
            Spacer(Modifier.height(18.dp))
            BigButton("출발하기", primary = true, height = 90.dp, fontSize = 28, modifier = Modifier.fillMaxWidth()) { vm.startTrip() }
            Spacer(Modifier.height(8.dp))
            Text("실제 차량에서는 출발하면 자동 시작 (Vehicle SDK 속도 > 10km/h)", color = G.Muted, fontSize = 14.sp)
        }
    }
}

// ------------------------------------------------------------------ 02·07 주행 중 (글자 최소화)

@Composable
fun DrivingScreen(s: UiState, vm: GilmokViewModel) {
    val lm = s.current ?: s.next
    Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        GCard(Modifier.width(390.dp).fillMaxHeight(), padding = 20.dp) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(G.CardAlt)) {
                MiniMap(s, vm, Modifier.fillMaxSize())
                if (s.current == null && s.next != null) {
                    Box(Modifier.align(Alignment.BottomEnd).padding(16.dp).clip(RoundedCornerShape(12.dp)).background(G.Bg).padding(horizontal = 18.dp, vertical = 10.dp)) {
                        Text(String.format("%.1f km", s.nextDistM / 1000), color = G.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        GCard(Modifier.weight(1f).fillMaxHeight()) {
            Box(Modifier.fillMaxWidth().height(272.dp)) {
                if (lm != null) LandmarkArt(lm.art, Modifier.fillMaxSize(), dim = s.current == null)
                if (lm != null) DirectionBadge(s.relBearingDeg, s.dirLabel, s.passingNow, Modifier.align(Alignment.TopEnd).padding(16.dp))
            }
            Spacer(Modifier.height(14.dp))
            if (s.current != null) {
                Text(s.current.name, color = G.Text, fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(s.current.summary.replace("{dir}", s.dirLabel), color = G.Muted, fontSize = 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text(lm?.let { "다음 길목 · ${it.name}" } ?: "길목을 찾는 중", color = G.Muted, fontSize = 40.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(if (lm != null) String.format("%.1f km 앞 · 가까워지면 15초 요약을 들려드려요", s.nextDistM / 1000) else "", color = G.Muted, fontSize = 22.sp, maxLines = 1)
            }
            Spacer(Modifier.height(6.dp))
            NarrationBar(s.narrationProgress, s.narrationSeconds, s.narrating, s.ttsAvailable)
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                IconButtonBig(Modifier.weight(1f), onClick = { vm.replay() }) { ReplayIcon() }
                IconButtonBig(Modifier.weight(1f), onClick = { vm.skip() }) { SkipIcon() }
                IconButtonBig(Modifier.weight(1.25f), primary = true, onClick = { vm.askContextual() }) { MicIcon() }
            }
        }
    }
}

// ------------------------------------------------------------------ 03 정차 상세

@Composable
fun ParkedScreen(s: UiState, vm: GilmokViewModel) {
    val lm = s.current ?: s.next ?: return
    Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        GCard(Modifier.weight(1f).fillMaxHeight()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LandmarkArt(lm.art, Modifier.fillMaxSize())
                DirectionBadge(s.relBearingDeg, s.dirLabel, s.passingNow, Modifier.align(Alignment.TopEnd).padding(14.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(lm.name, color = G.Text, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(lm.subtitle, color = G.Muted, fontSize = 21.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                BigButton("사진 스팟 저장", Modifier.weight(1f)) { vm.savePhotoSpot() }
                if (s.route.arrived) BigButton("여정 로그 보기", Modifier.weight(1f)) { vm.showLog() }
                else BigButton("경유지 추가", Modifier.weight(1f)) { vm.addCurrentAsWaypoint() }
            }
        }
        GCard(Modifier.weight(1.05f).fillMaxHeight()) {
            Text("자세한 이야기 (정차 중 2분 버전)", color = G.Mint, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                lm.detail.forEach { line ->
                    Row {
                        Text("•", color = G.Text, fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(line, color = G.Text, fontSize = 24.sp, lineHeight = 34.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(lm.sourceNote + " (데모용 요약 문장)", color = G.Muted, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            NarrationBar(s.narrationProgress, s.narrationSeconds, s.narrating, s.ttsAvailable)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                BigButton(if (s.narrating) "다시 들려주기" else "들려주기", Modifier.weight(1.4f), primary = true) { vm.replay() }
                IconButtonBig(Modifier.weight(0.6f), primary = false, onClick = { vm.askContextual() }) { MicIcon(color = G.Text) }
            }
        }
    }
}

// ------------------------------------------------------------------ 04 음성 Q&A

@Composable
fun QnaScreen(s: UiState, vm: GilmokViewModel) {
    Row(Modifier.fillMaxSize().padding(24.dp)) {
        GCard(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(320.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    ListeningOrb(s.listening, s.narrating)
                    Spacer(Modifier.height(26.dp))
                    Text(
                        when { s.listening -> "듣는 중..."; s.narrating -> "답변 중"; s.answer != null -> "답변 완료"; else -> "" },
                        color = G.Mint, fontSize = 24.sp,
                    )
                }
                Spacer(Modifier.width(28.dp))
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(G.CardAlt).padding(horizontal = 28.dp, vertical = 26.dp)) {
                        Text(s.question?.let { "“ $it ”" } ?: "“ … ”", color = G.Text, fontSize = 32.sp)
                    }
                    Spacer(Modifier.height(26.dp))
                    Text("길목의 답변", color = G.Mint, fontSize = 21.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(s.answer ?: (if (s.listening) "" else "질문을 기다리는 중"), color = G.Text, fontSize = 30.sp, lineHeight = 42.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(14.dp))
                    Text("위치·헤딩 기준 자동 계산 (FusedLocation) · 운전자 조작 없음", color = G.Muted, fontSize = 18.sp)
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        BigButton("다시 질문", Modifier.weight(1f), primary = true, height = 92.dp) { vm.askContextual() }
                        BigButton("주행 화면으로", Modifier.weight(1f), height = 92.dp) { vm.backFromQna() }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ 05 길목 쉼표

@Composable
fun RestScreen(s: UiState, vm: GilmokViewModel) {
    Box(Modifier.fillMaxSize().padding(24.dp)) {
        GCard(Modifier.fillMaxSize()) {
            Text("잠깐 쉬어 갈까요?", color = G.Text, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text("연속 주행 ${UiState.hm(s.vehicle.continuousDrivingSec)} · 다음 휴게 가능 지점 ${s.restCards.size}곳 (Vehicle SDK 주행시간 + NaviHelper 경로)",
                color = G.Muted, fontSize = 21.sp)
            Spacer(Modifier.height(22.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                s.restCards.forEachIndexed { i, c ->
                    val sel = i == s.restSelected
                    val bar = when (c.stop.colorIndex) { 1 -> G.Green; 2 -> G.Orange; else -> G.Mint }
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(G.CardAlt)
                            .then(if (sel) Modifier.border(3.dp, G.Mint, RoundedCornerShape(20.dp)) else Modifier)
                            .clickable { vm.selectRest(i) }.padding(22.dp),
                    ) {
                        Row(Modifier.fillMaxWidth().height(128.dp)) {
                            Box(Modifier.width(12.dp).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(bar))
                            Spacer(Modifier.width(14.dp))
                            LandmarkArt(c.stop.art, Modifier.weight(1f).fillMaxHeight())
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(c.stop.name, color = G.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(String.format("%.1fkm · 경유 +%d분", c.distanceKm, c.detourMin), color = G.Muted, fontSize = 20.sp)
                        Text(c.stop.tagline, color = G.Muted, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                val name = s.restCards.getOrNull(s.restSelected)?.stop?.name ?: ""
                BigButton("경유지 추가 · $name", Modifier.weight(1f), primary = true, height = 84.dp) { vm.addSelectedWaypoint() }
                BigButton("괜찮아요, 계속 갈게요", Modifier.weight(1f), height = 84.dp) { vm.dismissRest() }
            }
        }
    }
}

// ------------------------------------------------------------------ 06 여정 로그

@Composable
fun LogScreen(s: UiState, vm: GilmokViewModel) {
    Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        GCard(Modifier.weight(1.6f).fillMaxHeight()) {
            Text("오늘의 길목", color = G.Muted, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            val places = s.log.map { it.name }.distinct().size
            Text("${places}곳 지나옴 · 해설 ${(s.narrationTotalSec / 60).coerceAtLeast(1)}분 · 질문 ${s.questionCount}개",
                color = G.Text, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(14.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                s.log.forEachIndexed { i, e ->
                    Row(Modifier.fillMaxWidth().height(66.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.width(40.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.width(3.dp).weight(1f).background(if (i == 0) G.Card else G.Line))
                            Box(Modifier.size(22.dp).clip(CircleShape).background(G.Mint))
                            Box(Modifier.width(3.dp).weight(1f).background(if (i == s.log.lastIndex) G.Card else G.Line))
                        }
                        Spacer(Modifier.width(18.dp))
                        Text(e.time, color = G.Muted, fontSize = 20.sp, modifier = Modifier.width(80.dp))
                        Text(e.name, color = G.Text, fontSize = 27.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(280.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(e.note, color = G.Muted, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        GCard(Modifier.weight(1f).fillMaxHeight()) {
            Text("공유 · 저장", color = G.Muted, fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))
            BigButton("휴대폰으로 여정 보내기", Modifier.fillMaxWidth(), height = 90.dp) { vm.demoAction("여정 ${s.log.size}건을 휴대폰 앱으로 전송 (가상)") }
            Spacer(Modifier.height(18.dp))
            BigButton("이 코스 저장", Modifier.fillMaxWidth(), height = 90.dp) { vm.demoAction("코스 '해운대 → 외도' 저장 (가상)") }
            Spacer(Modifier.height(18.dp))
            val coupons = s.log.count { it.note.startsWith("쉼표") }
            BigButton("지역 쿠폰 ${coupons}장 보기", Modifier.fillMaxWidth(), height = 90.dp, tint = G.Orange) { vm.demoAction("경유한 지역 상점 쿠폰 ${coupons}장 (가상)") }
            Spacer(Modifier.height(26.dp))
            Text("사진 스팟·쿠폰은 정차 중에만 열립니다\n(주행 중 시각 정보 최소화 원칙)", color = G.Muted, fontSize = 17.sp, lineHeight = 24.sp)
            Spacer(Modifier.weight(1f))
            BigButton("처음으로", Modifier.fillMaxWidth(), height = 64.dp, fontSize = 20) { vm.resetAll() }
        }
    }
}
