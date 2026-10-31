package com.zainoha.gilmok.sdk

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/**
 * 길목이 차량·플랫폼에서 받아 쓰는 신호를 5개 인터페이스로 추상화한다.
 * 지금은 sdk/sim 의 시뮬레이터 구현이 꽂혀 있고, Pleos Connect SDK 가 손에 들어오면
 * sdk/pleos 의 어댑터로 바꿔 끼운다 (README "실제 SDK 로 바꾸기" 참고).
 */

data class GeoPoint(val lat: Double, val lon: Double)

enum class Gear { P, R, N, D }

/** Vehicle SDK 대응: 속도, 기어, 연속 주행 시간(멈추지 않고 달린 시간). */
data class VehicleState(
    val speedKmh: Float = 0f,
    val gear: Gear = Gear.P,
    val continuousDrivingSec: Long = 0L,
)

/** FusedLocation SDK 대응: 위치, 진행 방향(0~360, 북=0), 속도. */
data class LocationFix(
    val point: GeoPoint,
    val headingDeg: Float,
    val speedKmh: Float,
)

data class RoutePlace(val id: String, val name: String, val point: GeoPoint)

/** NaviHelper SDK 대응: 목적지, 경유지, 경로선, 진행 거리. */
data class RouteState(
    val destination: RoutePlace,
    val waypoints: List<RoutePlace>,
    val routePoints: List<GeoPoint>,
    val traveledM: Double,
    val totalM: Double,
    val arrived: Boolean,
) {
    val remainingM: Double get() = (totalM - traveledM).coerceAtLeast(0.0)
    fun remainingMin(avgKmh: Double = 60.0): Int = (remainingM / 1000.0 / avgKmh * 60.0).roundToInt()
}

interface VehicleProvider {
    val state: StateFlow<VehicleState>
}

interface LocationProvider {
    val fix: StateFlow<LocationFix>
}

interface NaviProvider {
    val route: StateFlow<RouteState>
    /** 경유지 추가. 성공 시 true. 실제 SDK 에서는 NaviHelper 경유지 API 호출로 바뀐다. */
    fun addWaypoint(place: RoutePlace): Boolean
    fun removeWaypoint(id: String)
}

interface TtsProvider {
    /** 한국어 음성 엔진 준비 여부. */
    val available: StateFlow<Boolean>
    val speaking: StateFlow<Boolean>
    fun speak(text: String, utteranceId: String = "gilmok")
    fun stop()
    fun shutdown()
}

interface SttProvider {
    val listening: StateFlow<Boolean>
    /** 인식된 문장. */
    val transcripts: SharedFlow<String>
    fun startListening()
    fun stopListening()
}
