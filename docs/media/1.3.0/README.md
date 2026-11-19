# 1.3.0 captures

Source: `4fdb6757ef98dd8f2c3f2bac72e3dd69d059a127`, [successful packaged-client run 44458198507](https://github.com/SleepCodeX/foragingPuckGrinding/actions/runs/44458198507). Minecraft 26.2, Java 25, native model outlines and real GUI rendering.

- `settings.png`: `0019_tracker-menu-all.png`, 1280×720, GUI scale 2.
- `colors.png`: `0020_line-color-dropdown.png`, scrolled color palette.
- `trackers.png`: `0023_all-colored-lines-and-boxes.png`, native model outlines visible through a stone wall, plus tracers and matching boxes.
- `compact.png`: `0003_tracker-menu-compact.png`, 960×720, GUI scale 3 (320×240 logical GUI), scrollable card body.
- `menu-opening.gif`: original native captures `0004_menu-opening-00.png` through `0018_menu-opening-14.png`. The menu clock advances by 40 ms per frame across its 560 ms transition. Encoding keeps the native 1280×720 dimensions and adds a 1200 ms pause on the finished menu before repeating. GIF palette encoding is the only conversion; it is a timing demonstration, not a performance measurement.

PNG files are unmodified. The disposable offline fixture simulates a Torrhus sidebar and known mobs; it is not a live Hypixel session. Runtime checks also verify exact outline colors, independent color persistence, the glow-Off option, selection/master/F1 gating, compact keyboard navigation and native dropdown wheel input.
