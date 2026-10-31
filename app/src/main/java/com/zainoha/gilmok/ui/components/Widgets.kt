package com.zainoha.gilmok.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zainoha.gilmok.ui.theme.G

@Composable
fun TopBar(status: String, clock: String, speedKmh: Int, gps: Boolean, timeScale: Int, onLogoClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(G.Bg).padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("길목", color = G.Mint, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.clickable(onClick = onLogoClick))
        Spacer(Modifier.width(28.dp))
        Text(status, color = G.Muted, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (timeScale > 1) {
            Text("▶▶ x$timeScale", color = G.Orange, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(18.dp))
        }
        Text(clock, color = G.Text, fontSize = 19.sp)
        Spacer(Modifier.width(20.dp))
        Text("$speedKmh km/h", color = G.Text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(16.dp))
        Box(Modifier.width(1.dp).height(22.dp).background(G.Line))
        Spacer(Modifier.width(16.dp))
        Text("GPS", color = if (gps) G.Text else G.Muted, fontSize = 18.sp)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(G.Line))
}

@Composable
fun GCard(modifier: Modifier = Modifier, color: Color = G.Card, padding: Dp = 28.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(color).padding(padding), content = content)
}

/** 큰 터치 타깃 버튼 (최소 높이 72dp). primary 는 민트 채움. */
@Composable
fun BigButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    tint: Color? = null,
    height: Dp = 76.dp,
    fontSize: Int = 24,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val bg = tint ?: if (primary) G.Mint else G.CardAlt
    val fg = when {
        tint == G.Orange -> G.OnOrange
        primary || tint != null -> G.OnMint
        else -> G.Text
    }
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontSize = fontSize.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 아이콘 전용 큰 버튼 (주행 화면용). */
@Composable
fun IconButtonBig(modifier: Modifier = Modifier, primary: Boolean = false, onClick: () -> Unit, icon: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .height(84.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (primary) G.Mint else G.CardAlt)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = icon,
    )
}

@Composable
fun ReplayIcon(color: Color = G.Text, size: Dp = 40.dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension * 0.36f
        val c = Offset(this.size.width / 2, this.size.height / 2)
        drawArc(color, startAngle = -60f, sweepAngle = 300f, useCenter = false,
            topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2), style = Stroke(width = r * 0.28f, cap = StrokeCap.Round))
        val p = Path().apply {
            moveTo(c.x + r * 0.35f, c.y - r * 1.25f); lineTo(c.x + r * 0.95f, c.y - r * 0.55f); lineTo(c.x + r * 0.1f, c.y - r * 0.45f); close()
        }
        drawPath(p, color)
    }
}

@Composable
fun SkipIcon(color: Color = G.Text, size: Dp = 40.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val p = Path().apply { moveTo(w * 0.2f, h * 0.22f); lineTo(w * 0.68f, h * 0.5f); lineTo(w * 0.2f, h * 0.78f); close() }
        drawPath(p, color)
        drawRoundRect(color, topLeft = Offset(w * 0.72f, h * 0.22f), size = Size(w * 0.1f, h * 0.56f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f))
    }
}

@Composable
fun MicIcon(color: Color = G.OnMint, size: Dp = 44.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        drawRoundRect(color, topLeft = Offset(w * 0.37f, h * 0.1f), size = Size(w * 0.26f, h * 0.48f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.13f, w * 0.13f))
        drawArc(color, 0f, 180f, false, topLeft = Offset(w * 0.25f, h * 0.22f), size = Size(w * 0.5f, h * 0.5f), style = Stroke(w * 0.07f, cap = StrokeCap.Round))
        drawLine(color, Offset(w * 0.5f, h * 0.72f), Offset(w * 0.5f, h * 0.88f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.34f, h * 0.9f), Offset(w * 0.66f, h * 0.9f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
    }
}

@Composable
fun SpeakerIcon(active: Boolean, size: Dp = 44.dp) {
    val color = if (active) G.Mint else G.Muted
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        drawCircle(color, radius = w * 0.48f, style = Stroke(w * 0.06f))
        val p = Path().apply {
            moveTo(w * 0.3f, h * 0.42f); lineTo(w * 0.42f, h * 0.42f); lineTo(w * 0.56f, h * 0.3f)
            lineTo(w * 0.56f, h * 0.7f); lineTo(w * 0.42f, h * 0.58f); lineTo(w * 0.3f, h * 0.58f); close()
        }
        drawPath(p, color)
        drawArc(color, -40f, 80f, false, topLeft = Offset(w * 0.5f, h * 0.36f), size = Size(w * 0.28f, h * 0.28f), style = Stroke(w * 0.05f, cap = StrokeCap.Round))
    }
}

/** 해설 진행 막대: 스피커 아이콘 + 진행률 + 남은 초. */
@Composable
fun NarrationBar(progress: Float, seconds: Int, speaking: Boolean, ttsAvailable: Boolean, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(if (!ttsAvailable) "TTS 음성 데이터 없음 · 자막만" else "${seconds}s",
                color = if (ttsAvailable) G.Muted else G.Orange, fontSize = 16.sp)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SpeakerIcon(active = speaking)
            Spacer(Modifier.width(16.dp))
            Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(G.CardAlt)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).clip(RoundedCornerShape(7.dp)).background(G.Mint))
            }
        }
    }
}

/** 주황 방향 화살표 배지. relDeg 는 진행 방향 기준 상대 방위(시계방향). */
@Composable
fun DirectionBadge(relDeg: Float, label: String, passing: Boolean, modifier: Modifier = Modifier, large: Boolean = false) {
    val sz = if (large) 110.dp else 90.dp
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(sz).clip(RoundedCornerShape(18.dp)).background(Color(0xCC0E1116)), contentAlignment = Alignment.Center) {
            if (passing) {
                Text("지나는 중", color = G.Orange, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            } else {
                Canvas(Modifier.size(sz * 0.62f).rotate(relDeg)) {
                    val w = size.width; val h = size.height
                    val p = Path().apply { moveTo(w / 2, 0f); lineTo(w, h); lineTo(w / 2, h * 0.72f); lineTo(0f, h); close() }
                    drawPath(p, G.Orange)
                }
            }
        }
        if (!passing && label.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(label, color = G.Orange, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** 음성 질문 화면의 듣는 중 원. */
@Composable
fun ListeningOrb(listening: Boolean, speaking: Boolean) {
    val t = rememberInfiniteTransition(label = "orb")
    val pulse by t.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")
    val scale = if (listening || speaking) pulse else 1f
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(200.dp).border(4.dp, G.Mint, CircleShape))
        Box(Modifier.size((100 * scale).dp).clip(CircleShape).background(if (listening) G.Mint else G.MintDim))
    }
}
