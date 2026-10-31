"""PC 기본 스피커 WASAPI 루프백을 별도 프로세스에서 녹음한다 (record_demo.py 가 띄움).
사용: python -I tools/record_audio.py <out.wav> <stop_flag_file>
시작 시각(epoch)은 <out.wav>.start 파일에 기록한다. stop_flag_file 이 생기면 저장하고 종료."""
import os
import sys
import time
import wave

import numpy as np
import soundcard as sc

out, stop_flag = sys.argv[1], sys.argv[2]
sr = 48000
spk = sc.default_speaker()
mic = sc.get_microphone(spk.name, include_loopback=True)
chunks = []
with mic.recorder(samplerate=sr, channels=2, blocksize=sr // 10) as rec:
    with open(out + ".start", "w") as f:
        f.write(repr(time.time()))
    while not os.path.exists(stop_flag):
        chunks.append(rec.record(numframes=sr // 10))
data = np.concatenate(chunks) if chunks else np.zeros((1, 2))
pcm = (np.clip(data, -1, 1) * 32767).astype(np.int16)
with wave.open(out, "wb") as w:
    w.setnchannels(2)
    w.setsampwidth(2)
    w.setframerate(sr)
    w.writeframes(pcm.tobytes())
print("rms %.5f dur %.1fs" % (float(np.sqrt((data ** 2).mean())), len(data) / sr))
