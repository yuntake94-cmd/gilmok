package com.zainoha.gilmok.sdk.sim

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.io.File
import java.util.Locale

/**
 * 시연 영상 제작 보조: `adb shell am broadcast -a com.zainoha.gilmok.TTS_TO_FILE --es text "..." --es out /sdcard/x.wav`
 * 로 받은 문장을 앱과 같은 TTS 음성(한국어, 속도 1.05)으로 WAV 파일에 합성한다. 완료 시 logcat 에 "GilmokTTSFile: done <out>".
 */
class TtsFileReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra("text") ?: return
        val name = intent.getStringExtra("out") ?: return
        val appCtx = context.applicationContext
        // 앱 내부 저장소에 쓴다 (debug 빌드에서 `adb exec-out run-as com.zainoha.gilmok cat files/<name>` 로 꺼냄)
        val out = File(appCtx.filesDir, File(name).name).absolutePath
        Log.i(TAG, "request ${text.length}자 → $out")
        var engine: TextToSpeech? = null
        engine = TextToSpeech(appCtx) { status ->
            val e = engine ?: return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) { Log.e(TAG, "init failed"); return@TextToSpeech }
            e.setLanguage(Locale.KOREAN)
            e.setSpeechRate(1.05f)
            e.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { Log.i(TAG, "done $out"); e.shutdown() }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { Log.e(TAG, "error $out"); e.shutdown() }
                override fun onError(utteranceId: String?, errorCode: Int) { Log.e(TAG, "error $out code=$errorCode"); e.shutdown() }
            })
            val r = e.synthesizeToFile(text, Bundle(), File(out), "file")
            Log.i(TAG, "synthesizeToFile(${text.length}자) → $out result=$r")
        }
    }

    private companion object { const val TAG = "GilmokTTSFile" }
}
