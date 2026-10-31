package com.zainoha.gilmok.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 와이어프레임과 같은 다크 팔레트. 민트 포인트, 주황 방향 표시. */
object G {
    val Bg = Color(0xFF15181F)
    val Card = Color(0xFF1E222B)
    val CardAlt = Color(0xFF2A2F3A)
    val Line = Color(0xFF353B48)
    val Mint = Color(0xFF00C8AA)
    val MintDim = Color(0xFF0B6F60)
    val Orange = Color(0xFFFFAA3C)
    val Green = Color(0xFF4CD37B)
    val Text = Color(0xFFF2F4F7)
    val Muted = Color(0xFF9AA3B2)
    val OnMint = Color(0xFF07201B)
    val OnOrange = Color(0xFF2A1A00)
    val SkyTop = Color(0xFF1C2B4A)
    val SeaDeep = Color(0xFF0E3A5B)
}

@Composable
fun GilmokTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = G.Mint,
            onPrimary = G.OnMint,
            secondary = G.Orange,
            background = G.Bg,
            surface = G.Card,
            onBackground = G.Text,
            onSurface = G.Text,
            surfaceVariant = G.CardAlt,
            onSurfaceVariant = G.Muted,
        ),
        content = content,
    )
}
