"""Collect the captures taken by the packaged Minecraft client for this release."""
from pathlib import Path
from shutil import copyfile
from PIL import Image

screens = Path("build/run/productionClientGameTest/screenshots")
output = Path("build/release-media")
output.mkdir(parents=True, exist_ok=True)
captures = list(screens.rglob("*.png"))


def capture(name):
    matches = [path for path in captures if name in path.stem]
    if len(matches) != 1:
        raise RuntimeError(f"Expected one screenshot for {name}, got {matches}")
    return matches[0]


names = {
    "tracker-menu-all": "settings",
    "tracker-menu-large": "large",
    "tracker-menu-compact": "compact",
    "line-color-dropdown": "colors",
    "all-five-trackers": "trackers",
    "nametag-parched-and-pollendart": "nametags",
    "custom-nametag-editor": "nametag-editor",
    "custom-mob-type-editor": "type-editor",
    "custom-tracker-library": "library",
    "custom-tracked-mobs": "custom-targets",
    "bundled-safari-settings": "safari",
    "home-dashboard": "home",
    "home-opening-03": "hivethief",
    "compact-dashboard": "home-compact",
    "hh-safari-settings-hunting": "hunting-settings",
    "hunting-run-report": "run-report",
    "hunting-recatch-and-perch-marks": "world-marks",
    "cavern-roster-checklist": "cavern-checklist",
    "missing-panel-extras-gazer-2-of-4": "missing-panel",
    "puff-crux-settings": "puff-crux",
    "puff-crux-countdown": "puff-hud"
}
for original, published in names.items():
    copyfile(capture(original), output / f"{published}.png")

frames = []
for index in range(11):
    with Image.open(capture(f"home-opening-{index:02}")) as image:
        frames.append(image.convert("RGB").resize((800, 450), Image.Resampling.LANCZOS))
frames[0].save(output / "menu-opening.gif", save_all=True, append_images=frames[1:],
               duration=[40] * 10 + [1400], loop=0)
