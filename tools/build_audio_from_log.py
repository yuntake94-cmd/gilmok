"""
녹화 중 앱이 남긴 해설 로그(logcat GilmokVM)로 깨끗한 음성 트랙을 만든다.

PC 루프백 녹음은 끊김이 섞이므로, 각 해설 문장을 앱과 같은 Google TTS 음성으로
에뮬레이터 안에서 WAV 로 합성(TtsFileReceiver)해 받아 온 뒤, 로그 시각에 맞춰 타임라인에 올린다.
다음 해설/정지가 오면 그 시점에서 앞 해설을 잘라낸다(실제 앱 동작과 동일).

사용: python -I tools/build_audio_from_log.py <logcat.txt> <video_first_scene_sec> <out.wav>
  - logcat.txt : `adb logcat -d -v time -s GilmokVM:*` 출력 (녹화 직전 `adb logcat -c` 로 비운 것)
  - video_first_scene_sec : 영상에서 출발 탭으로 화면이 바뀐 시각 (record_demo.py 가 계산)
"""
import os
import re
import subprocess
import sys
import time
import wave

import numpy as np

ADB = os.environ.get("ADB") or os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
SR = 48000
SPEAK_LATENCY = 0.55   # narrate() → 실제 발화 시작 (250ms 지연 + 엔진 준비)

log_path, first_scene, out_path = sys.argv[1], float(sys.argv[2]), sys.argv[3]
work = os.path.join(os.path.dirname(os.path.abspath(out_path)), "tts_parts")
os.makedirs(work, exist_ok=True)


def adb(*a):
    return subprocess.run([ADB, *a], capture_output=True, text=True, encoding="utf-8", errors="ignore")


# ---- 로그 파싱: "MM-DD HH:MM:SS.mmm D/GilmokVM(pid): narrate|SCREEN|text"
events = []
for line in open(log_path, encoding="utf-8", errors="ignore"):
    m = re.match(r"(\d\d)-(\d\d) (\d\d):(\d\d):(\d\d)\.(\d{3}).*GilmokVM.*?: (narrate|stopNarration)\|([^|]*)\|?(.*)$", line.strip())
    if not m:
        continue
    t = int(m[3]) * 3600 + int(m[4]) * 60 + int(m[5]) + int(m[6]) / 1000.0
    events.append((t, m[7], m[9].strip()))
if not events:
    sys.exit("narrate 로그가 없습니다")
t_first = events[0][0]                       # 첫 해설(출발 탭) = 영상의 첫 화면 전환
narr = [(t, kind, text) for (t, kind, text) in events]
print(f"{len(narr)} events, first narrate at log {t_first:.3f}s → video {first_scene:.2f}s")

# ---- 각 문장을 에뮬레이터 TTS 로 합성해서 받아오기
def synth(i, text):
    local = os.path.join(work, f"n{i:02d}.wav")
    if os.path.exists(local) and os.path.getsize(local) > 1000:
        return local
    name = f"gilmok_tts_{i:02d}.wav"
    remote = f"/data/data/com.zainoha.gilmok/files/{name}"
    adb("logcat", "-c")
    # adb shell 은 원격 셸에서 인자를 다시 나누므로 문장을 작은따옴표로 감싼다
    safe = text.replace("'", "")
    adb("shell", f"am broadcast -n com.zainoha.gilmok/.sdk.sim.TtsFileReceiver -a com.zainoha.gilmok.TTS_TO_FILE "
                 f"--es text '{safe}' --es out {name}")
    for _ in range(120):
        time.sleep(0.5)
        lg = adb("logcat", "-d", "-s", "GilmokTTSFile:*").stdout
        if "done " in lg and name in lg.split("done ",1)[1]:
            break
        if "error " in lg and "/" + name in lg:
            print("  !! TTS error for", i); break
    # debug 빌드: run-as 로 앱 내부 파일을 꺼낸다
    raw = subprocess.run([ADB, "exec-out", "run-as", "com.zainoha.gilmok", "cat", f"files/{name}"], capture_output=True).stdout
    with open(local, "wb") as f:
        f.write(raw)
    return local


def read_wav(path):
    with wave.open(path) as w:
        sr, ch = w.getframerate(), w.getnchannels()
        d = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(np.float32) / 32767
    if ch > 1:
        d = d.reshape(-1, ch).mean(axis=1)
    if sr != SR:   # 선형 보간 리샘플
        n = int(len(d) * SR / sr)
        d = np.interp(np.linspace(0, len(d) - 1, n), np.arange(len(d)), d)
    return d


total_len = int((events[-1][0] - t_first + first_scene + 40) * SR)
mix = np.zeros(total_len, dtype=np.float32)
for i, (t, kind, text) in enumerate(narr):
    if kind != "narrate":
        continue
    start = (t - t_first) + first_scene + SPEAK_LATENCY
    nxt = next((e[0] for e in narr[i + 1:]), None)
    end = (nxt - t_first) + first_scene + 0.25 if nxt else None   # 다음 narrate/stop 에서 끊김
    local = synth(i, text)
    d = read_wav(local)
    if end is not None:
        d = d[: max(0, int((end - start) * SR))]
    fade = min(len(d), int(SR * 0.03))
    if fade > 0:
        d[:fade] *= np.linspace(0, 1, fade); d[-fade:] *= np.linspace(1, 0, fade)
    s = int(start * SR)
    mix[s:s + len(d)] += d[: max(0, len(mix) - s)]
    print(f"  {i:02d} video {start:6.2f}s +{len(d)/SR:5.1f}s  {text[:28]}…")

peak = np.abs(mix).max() or 1.0
mix = mix / peak * 0.9
pcm = (np.clip(mix, -1, 1) * 32767).astype(np.int16)
stereo = np.repeat(pcm[:, None], 2, axis=1)
with wave.open(out_path, "wb") as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(stereo.tobytes())
print("wrote", out_path, f"{len(pcm)/SR:.1f}s")
