"""Bake the menu's coverage atlas offline with Pillow. Pass the unmodified Nunito variable TTF
from google/fonts at 604936664fd62c14271209b51f98e7f495dd1a3e, ofl/nunito/Nunito[wght].ttf.
The shipped atlas needs no font loader or native rasterization at runtime.
"""
import hashlib
import json
import sys
from pathlib import Path

from PIL import Image, ImageFont

font_path = Path(sys.argv[1])
if hashlib.sha256(font_path.read_bytes()).hexdigest() != "bb55a5ca5c2042335b3991af27c4d0705d0ef41cac6164ac737fd8f2a1e85207":
    raise ValueError("Expected the pinned Nunito variable TTF")
assets = Path(__file__).resolve().parents[1] / "src/main/resources/assets/foragingpuckgrinding"
characters = "".join(chr(i) for i in range(32, 127)) + "".join(chr(i) for i in range(160, 256)) + "•…–—−‘’“”‹›←→"
sizes = (8, 10, 12, 14, 16, 20, 24, 28, 32, 40, 48, 64, 96)


def face(size):
    font = ImageFont.truetype(font_path, size)
    font.set_variation_by_axes([650])
    return font


reference = face(128)
atlas_width = 1024
metrics = {
    "characters": characters,
    "advances": [reference.getlength(char) / 128 for char in characters],
    "width": atlas_width,
    "strikes": [],
}
x, y, row_height = 1, 1, 0
images = []
for size in sizes:
    glyphs = []
    font = face(size)
    for char in characters:
        mask, (left, top) = font.getmask2(char, mode="L", anchor="ls")
        bounds = mask.getbbox()
        if not bounds:
            glyphs.append([0, 0, 0, 0, 0, 0])
            continue
        ink = Image.frombytes("L", mask.size, bytes(mask)).crop(bounds)
        if x + ink.width + 1 > atlas_width:
            x, y, row_height = 1, y + row_height + 2, 0
        images.append((ink, (x, y)))
        glyphs.append([x, y, ink.width, ink.height, left + bounds[0], top + bounds[1]])
        row_height = max(row_height, ink.height)
        x += ink.width + 2
    metrics["strikes"].append({"size": size, "glyphs": glyphs})
metrics["height"] = y + row_height + 1
alpha = Image.new("L", (atlas_width, metrics["height"]))
for ink, position in images:
    alpha.paste(ink, position)
atlas = Image.new("RGBA", alpha.size, "white")
atlas.putalpha(alpha)
atlas.save(assets / "textures/gui/menu_font.png", optimize=True)
(assets / "menu_font.json").write_text(json.dumps(metrics, separators=(",", ":")) + "\n")
