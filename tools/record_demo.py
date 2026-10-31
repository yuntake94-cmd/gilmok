"""
길목 시연 영상 자동 녹화 스크립트 (Windows + Android 에뮬레이터).

- 영상: `adb shell screenrecord` (에뮬레이터 화면 1280x720)
- 소리: PC 기본 스피커의 WASAPI 루프백 (soundcard 라이브러리) → TTS 소리가 그대로 담김
- 조작: DEMO_SCRIPT.md 의 순서를 adb 탭으로 자동 수행
- 마지막에 ffmpeg 으로 영상+소리 합성 → <out_dir>/gilmok_demo.mp4

사용: python -I tools/record_demo.py <out_dir>
환경 변수(선택): ADB, FFMPEG 로 실행 파일 경로 지정. 앱은 홈 화면에 떠 있어야 한다.
"""
import os
import re
import subprocess
import sys
import time


ADB = os.environ.get("ADB") or os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
FFMPEG = os.environ.get("FFMPEG") or "ffmpeg"
OUT_DIR = sys.argv[1] if len(sys.argv) > 1 else "demo"
os.makedirs(OUT_DIR, exist_ok=True)
LOG = []


def log(msg):
    line = f"[{time.time() - T0:6.1f}s] {msg}" if "T0" in globals() else msg
    LOG.append(line)
    try:
        print(line, flush=True)
    except UnicodeEncodeError:
        print(line.encode("ascii", "replace").decode(), flush=True)


def adb(*args, capture=True):
    return subprocess.run([ADB, *args], capture_output=True if capture else False)


def tap(x, y):
    adb("shell", "input", "tap", str(x), str(y))


def tap_text(needle, retries=3):
    """uiautomator 덤프에서 텍스트를 찾아 그 중앙을 탭한다."""
    for _ in range(retries):
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        xml = adb("exec-out", "cat", "/sdcard/ui.xml").stdout.decode("utf-8", "ignore")
        nodes = re.findall(r'<node[^>]*text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
        hit = [n for n in nodes if needle in n[0]]
        if hit:
            _, x1, y1, x2, y2 = hit[0]
            tap((int(x1) + int(x2)) // 2, (int(y1) + int(y2)) // 2)
            log(f"tap '{needle}'")
            return True
        time.sleep(0.6)
    log(f"!! not found '{needle}'")
    return False


def open_demo():
    tap(1243, 695)
    time.sleep(0.8)


def close_demo():
    tap_text("닫기")
    time.sleep(0.4)


def until(sec):
    """녹화 시작 기준 sec 초가 될 때까지 대기."""
    while time.time() - T0 < sec:
        time.sleep(0.1)


# ------------------------------------------------------------------ 녹화 시작
wav_path = os.path.join(OUT_DIR, "gilmok_demo_audio.wav")
raw_video = os.path.join(OUT_DIR, "gilmok_demo_video.mp4")
final = os.path.join(OUT_DIR, "gilmok_demo.mp4")

adb("wait-for-device")
# 앱을 홈 화면으로 초기화하고 에뮬레이터 미디어 볼륨을 최대로
adb("shell", "am", "force-stop", "com.zainoha.gilmok")
adb("shell", "am", "start", "-n", "com.zainoha.gilmok/.MainActivity")
time.sleep(4)
tap(880, 190)  # 전체화면 안내가 떠 있으면 닫기 (없으면 무해)
for _ in range(15):
    adb("shell", "input", "keyevent", "24")
time.sleep(1)
stop_flag = os.path.join(OUT_DIR, "stop.flag")
if os.path.exists(stop_flag):
    os.remove(stop_flag)
if os.path.exists(wav_path + ".start"):
    os.remove(wav_path + ".start")
audio_proc = subprocess.Popen([sys.executable, "-I", os.path.join(os.path.dirname(os.path.abspath(__file__)), "record_audio.py"),
                               wav_path, stop_flag], stdout=subprocess.PIPE, text=True)
while not os.path.exists(wav_path + ".start"):
    time.sleep(0.05)
time.sleep(0.2)
audio_started_at = float(open(wav_path + ".start").read())

adb("shell", "rm", "-f", "/sdcard/gilmok_demo.mp4")
adb("logcat", "-c")
proc = subprocess.Popen([ADB, "shell", "screenrecord", "--size", "1280x720", "--bit-rate", "8000000",
                         "--time-limit", "180", "/sdcard/gilmok_demo.mp4"])
T0 = time.time()
log("screenrecord spawned")

# ------------------------------------------------------------------ 시연 순서 (DEMO_SCRIPT.md)
until(3)
tap_text("출발하기"); T_START_TAP = time.time()   # 01 홈 → 02 주행: 해운대 요약 TTS, 이어서 광안대교 자동 트리거(약 34초)
until(47)                                          # 광안대교 요약 재생 후
tap(1088, 626); log("mic: bridge question")        # 04 Q&A "이 다리 길이가 얼마야?"
until(61)
tap_text("주행 화면으로")
open_demo(); tap_text("다음 길목으로 점프"); close_demo()   # 오륙도: 요약 즉시 시작 (왼쪽 n시 방향)
until(76)
tap(1088, 626); log("mic: island question")        # "저 멀리 보이는 섬 이름이 뭐야?" → 방향 포함 답변
until(89)
tap_text("주행 화면으로")
open_demo(); tap_text("다음 길목으로 점프"); close_demo()   # 가덕해저터널 요약
until(103)
open_demo(); tap_text("다음 길목으로 점프"); close_demo()   # 거가대교 요약
until(108)
open_demo(); tap_text("정차 (기어 P)"); close_demo()        # 03 정차 상세 (기어 P)
until(120)
open_demo(); tap_text("정차 (기어 P)"); time.sleep(0.6); tap_text("쉼표 제안 지금"); close_demo()  # 05 쉼표
until(131)
tap_text("경유지 추가 ·")                          # 경유지 추가 → 배너 + TTS
until(138)
open_demo(); tap_text("도착 지점으로 점프"); close_demo()   # 외도 요약 → 약 18초 뒤 정차 → 상세
until(166)
tap_text("여정 로그 보기")                         # 06 여정 로그
until(174)  # screenrecord 는 마지막 화면 변화까지만 기록하므로 여유를 둔다

# ------------------------------------------------------------------ 녹화 종료
adb("shell", "pkill", "-2", "screenrecord")
T_STOP = time.time()
log("screenrecord stopped")
try:
    proc.wait(timeout=15)
except subprocess.TimeoutExpired:
    proc.kill()
time.sleep(1.0)
open(stop_flag, "w").close()
audio_out = audio_proc.communicate(timeout=60)[0].strip()
log(f"audio: {audio_out}")

adb("pull", "/sdcard/gilmok_demo.mp4", raw_video)
FFPROBE = os.path.join(os.path.dirname(FFMPEG), "ffprobe" + (".exe" if FFMPEG.lower().endswith(".exe") else "")) if os.path.dirname(FFMPEG) else "ffprobe"
probe = subprocess.run([FFPROBE, "-v", "error", "-show_entries", "format=duration",
                        "-of", "default=nw=1:nk=1", raw_video], capture_output=True, text=True)
vdur = float(probe.stdout.strip() or 0)
# 영상 안에서 첫 화면 전환(홈 → 주행, 출발 탭) 시각을 찾아 벽시계와 맞춘다
scene = subprocess.run([FFMPEG, "-hide_banner", "-t", "12", "-i", raw_video, "-vf", "select='gt(scene,0.2)',showinfo",
                        "-an", "-f", "null", "-"], capture_output=True, text=True, errors="ignore")
m = re.search(r"pts_time:([0-9.]+)", scene.stderr + scene.stdout)
if m:
    video_start = T_START_TAP - float(m.group(1)) + 0.15   # 탭 → 화면 반영 지연 보정
    log(f"first scene change at {float(m.group(1)):.2f}s in video")
else:
    video_start = T0 + 0.5
offset = max(0.0, video_start - audio_started_at)
log(f"video {vdur:.1f}s, audio offset {offset:.2f}s")

# 앱 해설 로그로 깨끗한 음성 트랙을 만든다 (PC 루프백 녹음은 끊김이 섞이므로 예비용)
narr_log = os.path.join(OUT_DIR, "narration_log.txt")
with open(narr_log, "w", encoding="utf-8") as f:
    f.write(subprocess.run([ADB, "logcat", "-d", "-v", "time", "-s", "GilmokVM:*"], capture_output=True, text=True,
                           encoding="utf-8", errors="ignore").stdout)
first_scene_sec = float(m.group(1)) if m else 5.0
clean_wav = os.path.join(OUT_DIR, "gilmok_demo_tts.wav")
r = subprocess.run([sys.executable, "-I", os.path.join(os.path.dirname(os.path.abspath(__file__)), "build_audio_from_log.py"),
                    narr_log, f"{first_scene_sec:.3f}", clean_wav], capture_output=True, text=True, encoding="utf-8", errors="ignore")
log(r.stdout.strip()[-600:])
if r.returncode == 0 and os.path.exists(clean_wav):
    audio_src, offset = clean_wav, 0.0     # 합성 트랙은 영상 시각 기준으로 이미 배치됨
    log("audio: TTS 합성 트랙 사용")
else:
    audio_src = wav_path
    log("audio: 합성 실패 → 루프백 녹음 사용")

# screenrecord 는 마지막 화면 변화에서 끝나므로 마지막 프레임을 6초 유지(tpad)하고 30fps 로 다시 인코딩
subprocess.run([FFMPEG, "-y", "-i", raw_video, "-ss", f"{offset:.3f}", "-i", audio_src,
                "-map", "0:v", "-map", "1:a", "-vf", "tpad=stop_mode=clone:stop_duration=6,fps=30",
                "-c:v", "libx264", "-preset", "fast", "-crf", "20", "-pix_fmt", "yuv420p",
                "-c:a", "aac", "-b:a", "160k", "-shortest", "-movflags", "+faststart", final],
               capture_output=True)
log(f"done → {final}")
with open(os.path.join(OUT_DIR, "record_log.txt"), "w", encoding="utf-8") as f:
    f.write("\n".join(LOG))
