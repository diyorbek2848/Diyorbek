# DrevClient

Utility client for **Minecraft Java 1.21.11 (Fabric)** by DrevSS.
57 modules, a draggable ClickGUI, an editable HUD, dot-commands, macros, and **DrevAI**, an assistant that knows the whole client and can search the web.

Made for your own server and singleplayer. Using combat/movement modules on other people's servers can get you banned.

---

## Install

1. Install the latest **Fabric Loader** for 1.21.11 and put **Fabric API** (0.140+ for 1.21.11) in `mods/`.
2. Put `DrevClient-1.0.0+mc1.21.11.jar` in `mods/`.
3. Launch, join a world, press **Right Shift**.

## Build the jar

You need **JDK 21** (Temurin/Adoptium is fine).

**On your PC (Windows):**
```
cd DrevClient
gradlew.bat build
```
The jar appears in `build/libs/`. The first build downloads Minecraft and Fabric, so it takes a few minutes.

**On GitHub (no setup):** push this folder to a GitHub repo, open the **Actions** tab, and download the `DrevClient` artifact when the build finishes. The workflow is in `.github/workflows/build.yml`.

If the build fails, copy the first error from the log and send it to me: it will be a small name fix.

---

## Controls

| Key | Action |
|---|---|
| Right Shift | ClickGUI |
| J | DrevAI chat window |
| C (hold) | Zoom |
| Middle click a player | Add/remove friend |

**ClickGUI:** left click toggles a module, right click opens its settings, middle click binds a key. Drag panel headers to move them, right click a header to collapse it, scroll inside a panel, just start typing to search. Right click a number setting to reset it; right click a color to toggle rainbow. Bottom-right buttons open the **HUD Editor** and **DrevAI**.

**HUD Editor:** drag elements anywhere, right click to hide/show.

---

## Modules

**Combat:** KillAura, CrystalAura, AutoTotem, AutoArmor, Criticals, Surround, BowAimbot, AutoWeapon, Velocity

**Player:** AutoEat, AutoTool, AutoReplenish, FastUse, NoFall, AutoRespawn, InventoryTweaks (steal/dump buttons, auto-steal, junk dropping), Reach, GuiUtils (UI utils: soft close, desync, save/restore GUI, sync id)

**Movement:** Flight, Speed, Sprint, NoSlow, Step, Jesus, ElytraFly, Spider, AutoWalk

**Render:** ESP (box / glow), Tracers, Fullbright, Freecam, XRay (ore highlighting through walls), Nametags (health, ping, distance, gear, durability), StorageESP, BlockESP, Zoom, Trajectories, NoRender

**World:** Nuker, Scaffold, AutoFarm, LiquidFiller, Timer, StashFinder, InfinityMiner

**Misc:** AutoReconnect, Notifier (visual range, join/leave, totem pops, armor and health warnings), Spam, DiscordRPC, BetterChat (timestamps, anti-spam, mention highlight), SoundBlocker, MiddleClickFriend

**Client:** ClickGUI, HUD, Client (toggle messages), DrevAI, Panic

**HUD elements:** Watermark, ModuleList, Coords (with nether/overworld conversion), FPS, Ping, Speed, Direction, Time, Server, Durability, Totems, Armor, Compass, Potions.

---

## Commands (prefix `.`)

```
.help [command]                 list commands
.toggle <module> [on|off]       or just .<module>
.bind <module> <key|none> [hold]
.binds
.settings <module> [setting] [value|reset]
.friend add|remove|list|clear [name]
.macro add <key> <line; line; ...>   (chat, /command or .command)
.macro remove <key> | .macro list
.give <item> [count]            creative only
.enchant <id|all> [level]       creative only, up to 255
.ai <question>                  ask DrevAI in chat
.ai key <key> | .ai model <m> | .ai open | .ai reset
.config save|load [name]
.prefix <symbol>
.drawn <module>                 hide/show in module list
.hud [edit|list|toggle <element>]
.vclip <n> | .hclip <n>
.gui restore|close|desync       UI utils
.coords   .panic   .modules   .say <msg>
```

---

## DrevAI

DrevAI runs from your PC through the Claude API with your own key.

1. Create a key at **console.anthropic.com** (API usage is billed to that account).
2. In game: `.ai key sk-ant-...`
3. Press **J** or type `.ai how do I set up CrystalAura?`

It gets a live description of every module, setting, command and HUD element, plus (optional) your coords, dimension, health and server. With **WebSearch** on it can look things up online. When it suggests a DrevClient command it shows it as a clickable line in the DrevAI window.

Settings: `.settings DrevAI` (Model, WebSearch, GameContext).

## Discord presence

Create an application at discord.com/developers, copy its **Application ID**, then:
```
.settings DiscordRPC AppId 123456789012345678
.toggle DiscordRPC
```
Optional: upload an art asset named `drevclient` in the app's Rich Presence settings.

---

Config files live in `.minecraft/config/drevclient/` (`default.json`, named configs, `stashes.txt`).
