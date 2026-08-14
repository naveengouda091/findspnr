# FindSpnr – Dungeon Radar Mod

A **Fabric client-side mod** for **Minecraft 26.2 "Chaos Cubed"** that scans loaded chunks for **monster spawners (dungeons)** and shows them as **red dots** on a HUD radar — inspired by the SeedCracker minimap style.

> ✅ Updated for MC 26.2 — Vulkan-compatible rendering, Java 25, Fabric Loader 0.19.3

---

## Features

| Feature | Description |
|---|---|
| 🔴 **HUD Radar** | Circular radar (top-right) with red dots for each nearby spawner, rotated to match your player yaw |
| 📋 **Text List** | Top-left overlay listing the closest spawners with distance & coordinates |
| 🌐 **3-D ESP** | Glowing red bounding-box outline rendered **through walls** — Vulkan & OpenGL compatible |
| ⌨️ **Keybinding** | Press **G** to toggle the entire mod on/off |
| 💬 **Chat Commands** | `/findspnr toggle \| esp \| radar \| list` |

---

## Installation

1. Install [Fabric Loader 0.16.9+](https://fabricmc.net/use/installer/) for Minecraft **1.21.4**
2. Install [Fabric API 0.114.0+1.21.4](https://modrinth.com/mod/fabric-api)
3. Drop `findspnr-1.0.0.jar` into your `mods/` folder
4. Launch and join any world — press **G** to toggle

---

## Building

```bash
# Requires Java 21
./gradlew build
# Output: build/libs/findspnr-1.0.0.jar
```

---

## Controls

| Key / Command | Action |
|---|---|
| `G` | Toggle mod on/off |
| `/findspnr toggle` | Same as G |
| `/findspnr esp` | Toggle 3-D world ESP |
| `/findspnr radar` | Toggle HUD radar |
| `/findspnr list` | List all detected spawners in chat |

---

## License
MIT © 2026 bevin
