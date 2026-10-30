# Test pictures

Made for this project, so they carry no third-party rights.

- `rotated-photo.heic` — a 400x200 picture, left half red and right half
  blue, stored sideways with the orientation an iPhone writes for an upright
  portrait photo (EXIF orientation 6, an `irot` box in the HEIF file). Shown
  correctly, it stands upright with the red half on top.
- `broken-photo.heic` — the first half of `rotated-photo.heic`: a valid HEIC
  header with damaged picture data.

Generated with Python and `pillow-heif`:

```python
import pillow_heif
from PIL import Image

pillow_heif.register_heif_opener()
img = Image.new("RGB", (400, 200), (220, 30, 30))
img.paste((30, 60, 220), (200, 0, 400, 200))
exif = Image.Exif()
exif[0x0112] = 6
img.save("rotated-photo.heic", quality=90, exif=exif.tobytes())
data = open("rotated-photo.heic", "rb").read()
open("broken-photo.heic", "wb").write(data[: len(data) // 2])
```

- `clip.webm` — one second of ffmpeg's built-in test pattern, 64x48, VP8,
  no sound, made with the ffmpeg bundled in the app's build:

```
ffmpeg -f lavfi -i "testsrc=size=64x48:rate=10:duration=1" -c:v libvpx -b:v 50k -an clip.webm
```

- `clip.mov` — one second of the test pattern, 160x120, MPEG-4 video in a
  QuickTime file: a format browsers do not play, so the app converts it.
- `long-clip.mov` — eleven minutes of a still grey 16x16 picture at one frame
  per second: over the ten-minute limit, yet tiny.

```
ffmpeg -f lavfi -i "testsrc=size=160x120:rate=10:duration=1" -c:v mpeg4 -q:v 10 clip.mov
ffmpeg -f lavfi -i "color=c=gray:size=16x16:rate=1:duration=660" -c:v mpeg4 -q:v 31 long-clip.mov
```
