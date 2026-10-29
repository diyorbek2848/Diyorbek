#!/bin/bash
SRC="$1"
show() { f="$SRC/$1"; echo "===== $1"; if [ -f "$f" ]; then sed -n "${2:-1},${3:-400}p" "$f" | grep -v '^import'; else echo MISSING; fi; }
show net/minecraft/world/entity/player/PlayerSkin.java 1 80
show net/minecraft/core/ClientAsset.java
show net/minecraft/world/entity/player/PlayerModelType.java 1 40
show net/minecraft/client/gui/components/PlayerSkinWidget.java 1 120
show net/minecraft/client/gui/font/providers/TrueTypeGlyphProviderDefinition.java 20 60
show net/minecraft/client/renderer/texture/DynamicTexture.java 1 60
show net/minecraft/client/gui/screens/inventory/InventoryScreen.java 95 140
show net/minecraft/client/player/AbstractClientPlayer.java 78 90
show net/minecraft/client/multiplayer/PlayerInfo.java 28 40
show net/minecraft/client/multiplayer/PlayerInfo.java 84 94
echo "===== grep"
grep -rn "PlayerSkinWidget(" "$SRC/net/minecraft" | sed "s|$SRC/||" | head -5
grep -rn "class .*Toast\b\|SystemToast.add\|addOrUpdate(" "$SRC/net/minecraft/client/gui/components/toasts/SystemToast.java" | head -5
grep -n "public static void onceOrIgnore\|public static void add\|public static .* multiline\|public static void addOrUpdate" "$SRC/net/minecraft/client/gui/components/toasts/SystemToast.java" | head
grep -n "Ingredient\|public .*getEntityModels\|EntityModelSet getEntityModels" "$SRC/net/minecraft/client/Minecraft.java" | head -3
grep -rn "tinyfd_openFileDialog" "$SRC/net/minecraft" | sed "s|$SRC/||" | head -3
