package com.zainoha.gilmok.domain

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zainoha.gilmok.content.ArtKind
import com.zainoha.gilmok.content.Landmark
import com.zainoha.gilmok.content.RestStop
import com.zainoha.gilmok.content.RouteData
import com.zainoha.gilmok.sdk.Gear
import com.zainoha.gilmok.sdk.LocationFix
import com.zainoha.gilmok.sdk.LocationProvider
import com.zainoha.gilmok.sdk.NaviProvider
import com.zainoha.gilmok.sdk.RoutePlace
import com.zainoha.gilmok.sdk.RouteState
import com.zainoha.gilmok.sdk.SttProvider
import com.zainoha.gilmok.sdk.TtsProvider
import com.zainoha.gilmok.sdk.VehicleProvider
import com.zainoha.gilmok.sdk.VehicleState
import com.zainoha.gilmok.sdk.sim.AndroidTtsProvider
import com.zainoha.gilmok.sdk.sim.DriveSimulator
import com.zainoha.gilmok.sdk.sim.SampleSttProvider
import com.zainoha.gilmok.sdk.sim.SimLocationProvider
import com.zainoha.gilmok.sdk.sim.SimNaviProvider
import com.zainoha.gilmok.sdk.sim.SimVehicleProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

enum class Screen { HOME, DRIVING, PARKED, QNA, REST, LOG }

enum class TourTheme(val label: String, val desc: String) {
    FAMILY("가족 여행", "아이 눈높이 설명, 퀴즈"),
    HISTORY("역사 산책", "지명 유래·근현대사"),
    SCENERY("풍경 감상", "짧은 해설, 음악 위주"),
}

data class LogEntry(val time: String, val name: String, val note: String)

data class RestCard(val stop: RestStop, val distanceKm: Double, val detourMin: Int)

data class UiState(
    val screen: Screen = Screen.HOME,
    val theme: TourTheme = TourTheme.FAMILY,
    val vehicle: VehicleState = VehicleState(),
    val fix: LocationFix,
    val route: RouteState,
    val current: Landmark? = null,
    val next: Landmark? = null,
    val nextDistM: Double = 0.0,
    val relBearingDeg: Float = 0f,
    val dirLabel: String = "",
    val passingNow: Boolean = false,
    val narrating: Boolean = false,
    val narrationProgress: Float = 0f,
    val narrationSeconds: Int = 15,
    val question: String? = null,
    val answer: String? = null,
    val listening: Boolean = false,
    val restCards: List<RestCard> = emptyList(),
    val restSelected: Int = 0,
    val log: List<LogEntry> = emptyList(),
    val narrationTotalSec: Int = 0,
    val banner: String? = null,
    val ttsAvailable: Boolean = false,
    val demoOpen: Boolean = false,
    val fastForward: Boolean = true,
    val timeScale: Int = 1,
    val targetSpeed: Float = 70f,
    val started: Boolean = false,
    val clock: String = "",
) {
    val isParked: Boolean get() = vehicle.gear == Gear.P
    val statusText: String
        get() = when (screen) {
            Screen.HOME -> "출발 전 · 정차 중"
            Screen.DRIVING -> "주행 중 · 요약 모드"
            Screen.PARKED -> "정차 중 · 상세 모드 (Vehicle SDK: 기어 P)"
            Screen.QNA -> "동승자 질문 · STT → 답변 TTS"
            Screen.REST -> "길목 쉼표 · 연속 주행 ${hm(vehicle.continuousDrivingSec)}"
            Screen.LOG -> "도착 · 정차 중 · 여정 로그"
        }
    val questionCount: Int get() = log.count { it.note.startsWith("질문") }

    companion object {
        fun hm(sec: Long): String {
            val h = sec / 3600
            val m = (sec % 3600) / 60
            return if (h > 0) "${h}시간 ${m}분" else "${m}분"
        }
    }
}

/**
 * 길목 엔진. 프로바이더 5개를 읽어 화면 상태(UiState)를 만든다.
 * 시뮬레이터 → 실제 SDK 교체는 아래 생성 부분만 바꾸면 된다.
 */
class GilmokViewModel(app: Application) : AndroidViewModel(app) {

    private val sim = DriveSimulator(viewModelScope, RouteData.routePoints)

    // ---- 프로바이더 (여기서 Sim* → Pleos* 로 교체) ----
    private val vehicle: VehicleProvider = SimVehicleProvider(sim)
    private val location: LocationProvider = SimLocationProvider(sim)
    private val navi: NaviProvider = SimNaviProvider(viewModelScope, sim, RouteData.destination)
    private val tts: TtsProvider = AndroidTtsProvider(app)
    private val stt: SampleSttProvider = SampleSttProvider(viewModelScope)
    private val sttIface: SttProvider get() = stt

    val landmarkDistM: Map<String, Double> = RouteData.landmarks.associate { it.id to sim.routeDistanceOf(it.anchor) }
    val restDistM: Map<String, Double> = RouteData.restStops.associate { it.id to sim.routeDistanceOf(it.anchor) }
    val cumulativeM: DoubleArray get() = sim.cumulativeM

    private val _ui = MutableStateFlow(UiState(fix = location.fix.value, route = navi.route.value))
    val ui: StateFlow<UiState> = _ui

    private var lastTriggeredId: String? = null
    private var restShown = false
    private var narrationStart = 0L
    private var narrationEstMs = 1L
    private var narrationActive = false
    private var pendingRestAfterAnswer = false
    private var arrivedParkedAt = 0L
    private var bannerJob: Job? = null
    private val clockFmt = DateTimeFormatter.ofPattern("HH:mm")

    init {
        sim.slowZones = RouteData.landmarks.map { lm ->
            val d = landmarkDistM.getValue(lm.id)
            (d - lm.triggerBeforeM - 250.0)..(d + lm.keepAfterM)
        }
        viewModelScope.launch {
            while (true) {
                tick()
                delay(100)
            }
        }
        viewModelScope.launch { sttIface.transcripts.collect { onTranscript(it) } }
        viewModelScope.launch { sttIface.listening.collect { l -> _ui.update { it.copy(listening = l) } } }
        viewModelScope.launch { tts.available.collect { a -> _ui.update { it.copy(ttsAvailable = a) } } }
        viewModelScope.launch {
            tts.speaking.collect { sp ->
                if (!sp && narrationActive && System.currentTimeMillis() - narrationStart > 1500) finishNarration()
            }
        }
    }

    // ------------------------------------------------------------------ 엔진 루프

    private fun tick() {
        val v = vehicle.state.value
        val f = location.fix.value
        val r = navi.route.value
        val traveled = r.traveledM
        val now = System.currentTimeMillis()
        val s = _ui.value

        val cur = RouteData.landmarks.firstOrNull { lm ->
            val d = landmarkDistM.getValue(lm.id)
            traveled >= d - lm.triggerBeforeM && traveled <= d + lm.keepAfterM
        }
        val next = RouteData.landmarks.firstOrNull { lm -> landmarkDistM.getValue(lm.id) > traveled + 50 && lm.id != cur?.id }
        val nextDist = next?.let { landmarkDistM.getValue(it.id) - traveled } ?: 0.0
        val target = cur ?: next
        val (relDeg, dirLabel) = target?.let { Geo.relative(f.point, f.headingDeg, it.viewPoint) } ?: (0f to "")
        val passing = cur != null && Geo.distanceM(f.point, cur.viewPoint) < 400.0

        var progress = s.narrationProgress
        if (narrationActive) {
            progress = ((now - narrationStart).toFloat() / narrationEstMs).coerceIn(0f, 1f)
            if (progress >= 1f && !tts.speaking.value) finishNarration()
        }

        var screen = s.screen
        val started = sim.started.value

        if (started && cur != null && cur.id != lastTriggeredId) {
            lastTriggeredId = cur.id
            if (screen == Screen.HOME) screen = Screen.DRIVING
            if (screen == Screen.DRIVING) {
                addLog(cur.name, "해설 15초")
                narrate(cur.summary.replace("{dir}", dirLabel))
            }
        }

        if (started) {
            if (screen == Screen.HOME && v.speedKmh > 10f) screen = Screen.DRIVING
            if (screen == Screen.DRIVING && v.gear == Gear.P && cur != null) {
                screen = Screen.PARKED
                addLog(cur.name, "정차 상세 2분")
                narrate(cur.detail.joinToString(" "))
            }
            if (screen == Screen.PARKED && v.gear != Gear.P && v.speedKmh > 3f && !r.arrived) {
                screen = Screen.DRIVING
                stopNarration()
            }
            if (!restShown && screen == Screen.DRIVING && !narrationActive && cur == null &&
                v.continuousDrivingSec >= RouteData.REST_THRESHOLD_SEC
            ) {
                screen = showRest(traveled, v)
            }
            if (r.arrived && screen == Screen.PARKED && !narrationActive) {
                if (arrivedParkedAt == 0L) arrivedParkedAt = now
                else if (now - arrivedParkedAt > 2500) {
                    screen = Screen.LOG
                    addLog(RouteData.landmarks.last().name, "도착")
                }
            }
            // 추가한 경유지에 닿으면 알림
            r.waypoints.firstOrNull { wp -> (restDistM[wp.id] ?: Double.MAX_VALUE) <= traveled }?.let { wp ->
                navi.removeWaypoint(wp.id)
                addLog(wp.name, "쉼표 · 경유")
                showBanner("경유지 도착: ${wp.name}")
            }
        }

        _ui.update {
            it.copy(
                screen = screen, vehicle = v, fix = f, route = r,
                current = cur, next = next, nextDistM = nextDist,
                relBearingDeg = relDeg, dirLabel = dirLabel, passingNow = passing,
                narrating = narrationActive, narrationProgress = progress,
                fastForward = sim.fastForward.value, timeScale = sim.currentScale.roundToInt(),
                targetSpeed = sim.targetSpeedKmh.value, started = started,
                clock = LocalTime.now().format(clockFmt),
            )
        }
    }

    // ------------------------------------------------------------------ 해설(TTS)

    private fun narrate(text: String) {
        Log.d("GilmokVM", "narrate|${_ui.value.screen}|$text")
        tts.stop()
        narrationStart = System.currentTimeMillis()
        narrationEstMs = (text.length * 200L + 600L).coerceIn(6000L, 70000L) // 한국어 TTS 실측 약 0.2초/글자
        narrationActive = true
        _ui.update { it.copy(narrating = true, narrationProgress = 0f, narrationSeconds = (narrationEstMs / 1000).toInt(),
            narrationTotalSec = it.narrationTotalSec + (narrationEstMs / 1000).toInt()) }
        tts.speak(text)
    }

    private fun finishNarration() {
        narrationActive = false
        _ui.update { it.copy(narrating = false, narrationProgress = 1f) }
        if (pendingRestAfterAnswer) {
            pendingRestAfterAnswer = false
            viewModelScope.launch {
                delay(600)
                val s = _ui.value
                val screen = showRest(s.route.traveledM, s.vehicle)
                _ui.update { it.copy(screen = screen) }
            }
        }
    }

    private fun stopNarration() {
        Log.d("GilmokVM", "stopNarration|${_ui.value.screen}")
        tts.stop()
        narrationActive = false
        _ui.update { it.copy(narrating = false) }
    }

    fun replay() {
        val s = _ui.value
        val lm = s.current ?: s.next ?: return
        when (s.screen) {
            Screen.PARKED -> narrate(lm.detail.joinToString(" "))
            Screen.QNA -> s.answer?.let { narrate(it) }
            else -> narrate(lm.summary.replace("{dir}", s.dirLabel))
        }
    }

    fun skip() {
        stopNarration()
        jumpNext()
    }

    // ------------------------------------------------------------------ 질문(STT → 답변 TTS)

    fun askContextual() {
        val s = _ui.value
        val q = (s.current ?: s.next)?.sampleQuestion ?: RouteData.sampleQuestions.last()
        ask(q)
    }

    fun ask(question: String) {
        stopNarration()
        _ui.update { it.copy(screen = Screen.QNA, question = null, answer = null) }
        sttIface.startListening()
        stt.simulateUtterance(question)
    }

    private fun onTranscript(q: String) {
        val s = _ui.value
        val answer = resolveAnswer(q, s)
        val about = nearestLandmark(s.fix)?.name ?: "길목"
        addLog(about, "질문: ${q.removeSuffix("?")}")
        _ui.update { it.copy(question = q, answer = answer) }
        narrate(answer)
    }

    private fun nearestLandmark(f: LocationFix, pred: (Landmark) -> Boolean = { true }): Landmark? =
        RouteData.landmarks.filter(pred).minByOrNull { Geo.distanceM(f.point, it.viewPoint) }

    private fun resolveAnswer(q: String, s: UiState): String {
        val f = s.fix
        fun dir(lm: Landmark) = Geo.relative(f.point, f.headingDeg, lm.viewPoint).second
        fun answerOf(lm: Landmark) = lm.sampleAnswer.replace("{dir}", dir(lm))
        return when {
            "섬" in q -> nearestLandmark(f) { it.art == ArtKind.ISLANDS || it.art == ArtKind.GARDEN }?.let { answerOf(it) } ?: fallback(s)
            "다리" in q || "길이" in q -> nearestLandmark(f) { it.art == ArtKind.SUSPENSION_BRIDGE || it.art == ArtKind.CABLE_BRIDGE }?.let { answerOf(it) } ?: fallback(s)
            "터널" in q || "바다 밑" in q || "미터" in q -> RouteData.landmarks.first { it.id == "tunnel" }.let { answerOf(it) }
            "성" in q -> RouteData.landmarks.first { it.id == "maemi" }.let { answerOf(it) }
            "쉬" in q -> { pendingRestAfterAnswer = true; "네, 앞쪽에 쉬어 갈 곳을 찾았어요. 화면에 띄울게요." }
            "남았" in q || "얼마나" in q || "외도" in q -> {
                val km = (s.route.remainingM / 100.0).roundToInt() / 10.0
                "외도 선착장까지 ${km}킬로미터, 약 ${s.route.remainingMin()}분 남았어요."
            }
            else -> fallback(s)
        }
    }

    private fun fallback(s: UiState): String {
        val lm = s.current ?: s.next ?: return "지금은 길목이 없는 구간이에요."
        return "${lm.name} 근처예요. ${lm.subtitle}."
    }

    fun backFromQna() {
        stopNarration()
        val s = _ui.value
        val screen = if (s.route.arrived) Screen.LOG else if (s.isParked && s.current != null) Screen.PARKED else Screen.DRIVING
        _ui.update { it.copy(screen = screen) }
    }

    // ------------------------------------------------------------------ 쉼표(경유지 제안)

    private fun showRest(traveled: Double, v: VehicleState): Screen {
        val cards = RouteData.restStops
            .filter { restDistM.getValue(it.id) > traveled + 200 }
            .take(3)
            .map { RestCard(it, (restDistM.getValue(it.id) - traveled) / 1000.0, it.detourMin) }
        if (cards.isEmpty()) return _ui.value.screen
        restShown = true
        _ui.update { it.copy(restCards = cards, restSelected = 0) }
        val first = cards.first()
        narrate("잠깐 쉬어 갈까요? 연속 주행 ${UiState.hm(v.continuousDrivingSec)}째예요. " +
            "${String.format("%.1f", first.distanceKm)}킬로미터 앞 ${first.stop.name}을 포함해 쉬어 갈 곳 ${cards.size}곳을 찾았어요.")
        return Screen.REST
    }

    fun forceRest() {
        sim.addContinuousDrivingSec(RouteData.REST_THRESHOLD_SEC)
        restShown = false
        val s = _ui.value
        if (!sim.started.value) startTrip()
        val screen = showRest(s.route.traveledM, vehicle.state.value.copy(continuousDrivingSec = vehicle.state.value.continuousDrivingSec + RouteData.REST_THRESHOLD_SEC))
        _ui.update { it.copy(screen = screen) }
    }

    fun selectRest(i: Int) = _ui.update { it.copy(restSelected = i) }

    fun addSelectedWaypoint() {
        val s = _ui.value
        val card = s.restCards.getOrNull(s.restSelected) ?: return
        addWaypoint(card.stop)
    }

    fun addWaypoint(stop: RestStop) {
        val ok = navi.addWaypoint(RoutePlace(stop.id, stop.name, stop.anchor))
        showBanner(if (ok) "경유지 추가됨 · ${stop.name} (NaviHelper)" else "이미 경유지에 있어요 · ${stop.name}")
        if (ok) addLog(stop.name, "쉼표 · 경유지 추가")
        val s = _ui.value
        val km = ((restDistM.getValue(stop.id) - s.route.traveledM) / 100.0).roundToInt() / 10.0
        _ui.update { it.copy(screen = if (it.isParked && it.current != null) Screen.PARKED else Screen.DRIVING) }
        narrate("${stop.name}을 경유지로 추가했어요. ${km}킬로미터 앞이에요.")
    }

    fun addCurrentAsWaypoint() {
        val s = _ui.value
        val lm = s.current ?: s.next ?: return
        val stop = RouteData.restStops.firstOrNull { it.anchor == lm.anchor }
            ?: RestStop(lm.id, lm.name, lm.anchor, lm.art, lm.subtitle, "", 0, 0)
        if (!restDistM.containsKey(stop.id)) {
            showBanner("경유지 추가됨 · ${lm.name} (NaviHelper)")
            addLog(lm.name, "경유지 추가")
            navi.addWaypoint(RoutePlace(stop.id, stop.name, stop.anchor))
        } else addWaypoint(stop)
    }

    fun dismissRest() {
        stopNarration()
        _ui.update { it.copy(screen = Screen.DRIVING) }
    }

    fun savePhotoSpot() {
        val s = _ui.value
        val lm = s.current ?: return
        addLog(lm.name, "사진 스팟 저장")
        showBanner("사진 스팟 저장됨 · ${lm.name} (위치·시각 기록)")
    }

    fun demoAction(label: String) = showBanner("데모: $label")

    // ------------------------------------------------------------------ 데모 패널 · 조작

    fun setTheme(t: TourTheme) = _ui.update { it.copy(theme = t) }

    fun startTrip() {
        if (sim.started.value) return
        sim.start()
        _ui.update { it.copy(screen = Screen.DRIVING) }
    }

    fun setSpeed(v: Float) {
        sim.targetSpeedKmh.value = v
        if (v > 0f && !sim.started.value) startTrip()
        if (v > 0f) sim.parked.value = false
    }

    fun setParked(p: Boolean) {
        if (!sim.started.value) startTrip()
        sim.parked.value = p
    }

    fun toggleFastForward() { sim.fastForward.value = !sim.fastForward.value }

    fun jumpNext() {
        if (!sim.started.value) startTrip()
        val traveled = sim.traveledM.value
        // 이미 접근 구간에 들어간 길목은 건너뛰고 그 다음 길목의 접근 지점으로 이동
        val next = RouteData.landmarks.firstOrNull { landmarkDistM.getValue(it.id) - it.triggerBeforeM > traveled + 200 } ?: return
        lastTriggeredId = null
        sim.jumpTo(landmarkDistM.getValue(next.id) - next.triggerBeforeM + 10.0) // 트리거 지점 바로 뒤 → 해설 즉시 시작
    }

    fun jumpToArrival() {
        if (!sim.started.value) startTrip()
        lastTriggeredId = null
        sim.jumpTo(sim.totalM - 350.0) // 도착 요약이 바로 시작되고 약 15초 뒤 정차 → 상세 → 여정 로그
    }

    fun showLog() {
        stopNarration()
        _ui.update { it.copy(screen = Screen.LOG) }
    }

    fun toggleDemo() = _ui.update { it.copy(demoOpen = !it.demoOpen) }

    fun resetAll() {
        stopNarration()
        sim.reset()
        lastTriggeredId = null
        restShown = false
        arrivedParkedAt = 0L
        _ui.update {
            UiState(fix = location.fix.value, route = navi.route.value, ttsAvailable = it.ttsAvailable, theme = it.theme)
        }
    }

    // ------------------------------------------------------------------ 보조

    private fun addLog(name: String, note: String) {
        val time = LocalTime.now().format(clockFmt)
        _ui.update { s ->
            val last = s.log.lastOrNull()
            val newLog = if (last != null && last.name == name && !last.note.startsWith("질문") && !note.startsWith("질문"))
                s.log.dropLast(1) + LogEntry(last.time, name, note)
            else s.log + LogEntry(time, name, note)
            s.copy(log = newLog)
        }
    }

    private fun showBanner(text: String) {
        bannerJob?.cancel()
        _ui.update { it.copy(banner = text) }
        bannerJob = viewModelScope.launch {
            delay(3500)
            _ui.update { it.copy(banner = null) }
        }
    }

    override fun onCleared() {
        tts.shutdown()
        super.onCleared()
    }
}
