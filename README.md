# Loupe

Hold a key to zoom in, scroll to zoom in or out, and let go to ease back out. The camera glides instead of snapping, so zooming feels natural and looking around while zoomed stays smooth.

Loupe is a small client-side Fabric mod for Minecraft, compatible with Mod Menu.

Supported versions: 1.21 to 1.21.11 and 26.1 to 26.3.

## Features

- **Smooth zoom** - the view eases in when you press the key and eases out when you let go. It takes the same time at any frame rate.
- **Smooth look** - while zoomed, the camera builds up speed gradually as you move the mouse and glides to a stop when you let go. The more you zoom in, the more it smooths, so it stays easy to aim at small details. You can turn it down or off.
- **Scroll to zoom** - while you hold the key, the mouse wheel zooms closer or wider instead of changing your hotbar slot. Go from about 1.1x up to 30x.
- **Starts fresh every time** - each new zoom begins at your chosen amount, no matter how far you scrolled last time.
- **Your FOV setting is left alone** - only the camera changes while you zoom, and everything returns to normal when you let go.
- **Works with any key or mouse button** - it is a normal entry in Minecraft's Controls menu, so you can rebind it and conflicts show up like any other key.
- **Client-side only** - it only changes your own camera, so there is nothing to install on a server.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for your version of Minecraft.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) and the Loupe jar for your version in your `mods` folder.
3. Start the game.

Optional: add [Mod Menu](https://modrinth.com/mod/modmenu) to get a settings screen (*Mods > Loupe > Configure*).

## How to use

Hold **C** while playing to zoom in. Scroll up to zoom closer, scroll down to zoom out, and release to go back to normal.

On Minecraft 1.21 to 1.21.8 the default key is **Z** instead, because C is already used by the creative toolbar there.

To change the key, go to *Options > Controls > Key Binds > Loupe*. You can pick any keyboard key or mouse button. Unbind it to turn zooming off.

## Settings

| Setting | Range | Default | What it does |
|---|---|---|---|
| Zoom Amount | 1.25x - 10x | 4x | How far you zoom in when you press the key |
| Animation Speed | 1 - 20 | 8 | How quickly the zoom eases in and out. Higher is snappier |
| Look Smoothing | Off - 100% | 60% | How much the camera glides while you look around zoomed in. Off follows the mouse directly |
| Scroll to Zoom | on / off | on | Whether the mouse wheel changes the zoom while you hold the key |

With Mod Menu, changes apply straight away and are saved when you close the screen. The screen also has a shortcut to the key setting and a *Reset to Defaults* button.

Without Mod Menu you can edit `config/loupe.json` in your game folder and restart the game:

```json
{
  "zoomAmount": 4.0,
  "zoomSpeed": 8.0,
  "lookSmoothing": 0.6,
  "scrollToZoom": true
}
```

Values that are out of range are corrected automatically. If the file is broken, the game still starts with the defaults and your old file is kept as `loupe.json.broken`.

## FAQ

**Will it change my FOV slider?**
No. Your FOV setting is never touched.

**The zoom feels too floaty (or not smooth enough) when I move the mouse.**
Change *Look Smoothing*. Lower it for a more direct feel, or set it to Off to follow the mouse exactly like vanilla.

**I use Minecraft's Cinematic Camera option.**
Loupe leaves look smoothing off in that case, since the game is already smoothing the camera.

## License

[MIT](LICENSE)
