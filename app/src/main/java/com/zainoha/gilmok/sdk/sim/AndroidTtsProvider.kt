package com.zainoha.gilmok.sdk.sim

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.zainoha.gilmok.sdk.TtsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Android 기본 TextToSpeech 로 실제 소리를 낸다. Pleos 의 TTS SDK 로 바꿀 때는
 * 이 클래스만 교체하면 된다 (speak/stop 계약 동일).
 *
 * 에뮬레이터의 Google TTS 가 문장 중간에 합성 오류로 조용히 끝내는 일이 있어
 * onRangeStart 로 읽던 위치를 추적하고, 끝까지 읽지 못한 채 끝나면 남은 부분부터 이어 읽는다.
 */
class AndroidTtsProvider(context: Context) : TtsProvider {

    private val _available = MutableStateFlow(false)
    override val available: StateFlow<Boolean> = _available
    private val _speaking = MutableStateFlow(false)
    override val speaking: StateFlow<Boolean> = _speaking

    @Volatile private var ready = false
    private var engine: TextToSpeech? = null
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null

    // 현재 발화 추적 (이어 읽기용)
    @Volatile private var currentText: String? = null
    @Volatile private var lastRangeStart = 0
    @Volatile private var lastRangeEnd = 0
    @Volatile private var startedAt = 0L
    @Volatile private var resumes = 0

    init {
        val appCtx = context.applicationContext
        engine = TextToSpeech(appCtx) { status ->
            if (status == TextToSpeech.SUCCESS) configure() else _available.value = false
        }
    }

    private fun configure() {
        val e = engine ?: return
        val r = e.setLanguage(Locale.KOREAN)
        ready = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
        e.setSpeechRate(1.05f)
        e.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "onStart $utteranceId")
                _speaking.value = true
            }
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                lastRangeStart = start
                lastRangeEnd = end
            }
            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "onDone $utteranceId range=$lastRangeStart-$lastRangeEnd/${currentText?.length}")
                if (!resumeIfTruncated("done")) { currentText = null; _speaking.value = false }
            }
            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                Log.d(TAG, "onStop $utteranceId interrupted=$interrupted")
                _speaking.value = false
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { onError(utteranceId, -1) }
            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.w(TAG, "onError $utteranceId code=$errorCode")
                if (!resumeIfTruncated("error")) { currentText = null; _speaking.value = false }
            }
        })
        _available.value = ready
    }

    /** 끝까지 읽지 못하고 끝났으면 남은 부분부터 다시 읽는다. 최대 2회. */
    private fun resumeIfTruncated(reason: String): Boolean {
        val text = currentText ?: return false
        if (resumes >= 2) return false
        val expectedMs = text.length * 170L
        val elapsed = System.currentTimeMillis() - startedAt
        val remaining = when {
            lastRangeEnd in 1 until text.length - 6 -> text.substring(lastRangeStart)      // 중간에서 끊김 → 그 어절부터
            lastRangeEnd == 0 && elapsed < expectedMs * 0.5 -> text                        // 범위 콜백이 없을 때의 시간 기준 판단
            else -> return false
        }
        resumes++
        Log.w(TAG, "truncated($reason) after ${elapsed}ms at $lastRangeStart/${text.length} → resume ${remaining.length}자")
        currentText = remaining
        lastRangeStart = 0
        lastRangeEnd = 0
        startedAt = System.currentTimeMillis()
        handler.postDelayed({ engine?.speak(remaining, TextToSpeech.QUEUE_FLUSH, Bundle(), "resume$resumes") }, 300)
        return true
    }

    /**
     * stop() 직후 바로 speak() 하면 Google TTS 가 발화를 떨어뜨리는 경우가 있어
     * 250ms 뒤에 발화하고, 요청 자체가 실패(ERROR) 하면 한 번 더 시도한다.
     */
    override fun speak(text: String, utteranceId: String) {
        val e = engine ?: return
        if (!ready) return
        pending?.let { handler.removeCallbacks(it) }
        val attempt = object : Runnable {
            var tries = 0
            override fun run() {
                tries++
                currentText = text
                lastRangeStart = 0
                lastRangeEnd = 0
                resumes = 0
                startedAt = System.currentTimeMillis()
                val r = e.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), utteranceId)
                Log.d(TAG, "speak(${text.length}자) result=$r try=$tries")
                if (r != TextToSpeech.SUCCESS && tries < 3) handler.postDelayed(this, 400)
            }
        }
        pending = attempt
        handler.postDelayed(attempt, 250)
    }

    override fun stop() {
        pending?.let { handler.removeCallbacks(it) }
        pending = null
        currentText = null
        Log.d(TAG, "stop()")
        engine?.stop()
        _speaking.value = false
    }

    override fun shutdown() {
        engine?.shutdown()
        engine = null
    }

    private companion object { const val TAG = "GilmokTTS" }
}
