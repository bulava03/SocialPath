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
