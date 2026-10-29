package dev.drevss.drevclient.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import dev.drevss.drevclient.DrevClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Client-side skins for cracked/offline accounts. Skins are PNG files in
 * .minecraft/config/drevclient/skins/. The selected one replaces your skin
 * on your screen (first person, third person, F5, tab list).
 */
public class SkinManager {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final Identifier TEXTURE = Identifier.parse("drevclient:dynamic/custom_skin");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private String selected;           // file name in the skins folder, or null
    private boolean slim;
    private String sourcePlayer;       // set when fetched by username (for SkinsRestorer)
    private boolean loaded;
    private volatile String status = "";
    private volatile boolean busy;

    public Path dir() {
        Path d = DrevClient.get().config().getDir().resolve("skins");
        try { Files.createDirectories(d); } catch (Exception ignored) { }
        return d;
    }

    public String status() { return status; }
    public boolean isBusy() { return busy; }
    public String selected() { return selected; }
    public boolean isSlim() { return slim; }
    public String sourcePlayer() { return sourcePlayer; }

    public List<String> library() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir())) {
            files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                .sorted().forEach(p -> out.add(p.getFileName().toString()));
        } catch (Exception ignored) { }
        return out;
    }

    // ------------------------------------------------------------------
    // applying
    // ------------------------------------------------------------------

    /** Loads and applies a skin from the library. Must run on the render thread. */
    public boolean apply(String fileName, boolean slimModel) {
        Path file = dir().resolve(fileName);
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(normalize(in.readAllBytes()));
            mc.getTextureManager().release(TEXTURE);
            mc.getTextureManager().register(TEXTURE, new DynamicTexture(TEXTURE::toString, image));
            selected = fileName;
            slim = slimModel;
            loaded = true;
            if (!fileName.equals(sourcePlayer + ".png")) sourcePlayer = null;
            status = "Applied " + fileName.replace(".png", "") + (slim ? " (slim)" : "");
            save();
            return true;
        } catch (Exception e) {
            status = "Couldn't load " + fileName + ": " + e.getMessage();
            return false;
        }
    }

    public void setSlim(boolean slim) {
        this.slim = slim;
        save();
    }

    public void reset() {
        selected = null;
        sourcePlayer = null;
        loaded = false;
        mc.getTextureManager().release(TEXTURE);
        status = "Using your normal skin again";
        save();
    }

    public void delete(String fileName) {
        try { Files.deleteIfExists(dir().resolve(fileName)); } catch (Exception ignored) { }
        if (fileName.equals(selected)) reset();
        status = "Deleted " + fileName;
    }

    /** The skin to show for the local player, or null for the normal one. */
    public PlayerSkin override(PlayerSkin original) {
        if (!loaded || selected == null) return original;
        ClientAsset.ResourceTexture body = new ClientAsset.ResourceTexture(TEXTURE, TEXTURE);
        return new PlayerSkin(body, original.cape(), original.elytra(), slim ? PlayerModelType.SLIM : PlayerModelType.WIDE, false);
    }

    public boolean isLocalName(String name) {
        return name != null && name.equalsIgnoreCase(mc.getUser().getName());
    }

    /**
     * Converts old 64x32 skins to 64x64 and checks the size. Returns PNG bytes.
     */
    private static InputStream normalize(byte[] png) throws Exception {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        if (img == null) throw new IllegalArgumentException("not a PNG image");
        int w = img.getWidth(), h = img.getHeight();
        if (w % 64 != 0 || (h != w && h * 2 != w)) throw new IllegalArgumentException("skin must be 64x64 or 64x32 (got " + w + "x" + h + ")");
        if (h * 2 == w) {
            int s = w / 64;
            BufferedImage full = new BufferedImage(w, w, BufferedImage.TYPE_INT_ARGB);
            full.getGraphics().drawImage(img, 0, 0, null);
            // legacy skins have no left arm/leg: mirror the right ones
            copyMirrored(full, 0, 16, 16, 16, 16, 48, s);  // leg
            copyMirrored(full, 40, 16, 16, 16, 32, 48, s); // arm
            img = full;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return new ByteArrayInputStream(out.toByteArray());
    }

    private static void copyMirrored(BufferedImage img, int sx, int sy, int w, int h, int dx, int dy, int s) {
        for (int y = 0; y < h * s; y++)
            for (int x = 0; x < w * s; x++)
                img.setRGB(dx * s + (w * s - 1 - x), dy * s + y, img.getRGB(sx * s + x, sy * s + y));
    }

    // ------------------------------------------------------------------
    // importing
    // ------------------------------------------------------------------

    /** Copies PNG files into the library and applies the first one. */
    public void importFiles(List<Path> files) {
        String first = null;
        for (Path p : files) {
            String name = p.getFileName().toString();
            if (!name.toLowerCase(Locale.ROOT).endsWith(".png")) continue;
            try {
                normalize(Files.readAllBytes(p)); // validate
                String clean = name.replaceAll("[^A-Za-z0-9_.-]", "_");
                Files.copy(p, dir().resolve(clean), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                if (first == null) first = clean;
            } catch (Exception e) {
                status = name + ": " + e.getMessage();
            }
        }
        if (first != null) apply(first, slim);
        else if (status.isEmpty()) status = "Drop a .png skin file";
    }

    /** Opens the system file picker on a background thread. */
    public void openFilePicker() {
        if (busy) return;
        busy = true;
        status = "Choose a skin file…";
        Thread t = new Thread(() -> {
            String result = null;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(1);
                filters.put(stack.UTF8("*.png"));
                filters.flip();
                result = TinyFileDialogs.tinyfd_openFileDialog("Choose a Minecraft skin (.png)", "", filters, "PNG skin", false);
            } catch (Throwable e) {
                status = "File picker unavailable, drag the PNG into the window instead";
            }
            String picked = result;
            mc.execute(() -> {
                busy = false;
                if (picked != null) importFiles(List.of(Path.of(picked)));
                else if (status.startsWith("Choose")) status = "";
            });
        }, "DrevClient-FilePicker");
        t.setDaemon(true);
        t.start();
    }

    /** Downloads any premium player's skin by name from Mojang. */
    public void fetchByName(String rawName) {
        String name = rawName.trim();
        if (busy || !name.matches("[A-Za-z0-9_]{2,16}")) {
            if (!busy) status = "Enter a valid Minecraft username";
            return;
        }
        busy = true;
        status = "Looking up " + name + "…";
        Thread t = new Thread(() -> {
            String msg;
            String file = null;
            boolean slimModel = false;
            try {
                JsonObject profile = getJson("https://api.mojang.com/users/profiles/minecraft/" + name);
                if (profile == null) throw new IllegalStateException("No premium player called " + name);
                String id = profile.get("id").getAsString();
                String realName = profile.get("name").getAsString();
                JsonObject session = getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + id);
                if (session == null) throw new IllegalStateException("Mojang didn't return a profile");
                String value = session.getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString();
                JsonObject textures = JsonParser.parseString(new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("textures");
                if (textures == null || !textures.has("SKIN")) throw new IllegalStateException(realName + " uses a default skin");
                JsonObject skin = textures.getAsJsonObject("SKIN");
                String url = skin.get("url").getAsString().replace("http://", "https://");
                slimModel = skin.has("metadata") && skin.getAsJsonObject("metadata").has("model")
                    && "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString());
                HttpResponse<byte[]> png = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
                if (png.statusCode() != 200) throw new IllegalStateException("Download failed (" + png.statusCode() + ")");
                file = realName + ".png";
                Files.write(dir().resolve(file), png.body());
                msg = null;
                sourcePlayer = realName;
            } catch (Exception e) {
                msg = e.getMessage() == null ? e.toString() : e.getMessage();
            }
            String error = msg, saved = file;
            boolean s = slimModel;
            mc.execute(() -> {
                busy = false;
                if (error != null) status = error;
                else if (apply(saved, s)) status = "Got " + saved.replace(".png", "") + "'s skin";
            });
        }, "DrevClient-SkinFetch");
        t.setDaemon(true);
        t.start();
    }

    private static JsonObject getJson(String url) throws Exception {
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).build(),
            HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() == 204 || r.statusCode() == 404) return null;
        if (r.statusCode() != 200) throw new IllegalStateException("Mojang API error " + r.statusCode());
        return JsonParser.parseString(r.body()).getAsJsonObject();
    }

    // ------------------------------------------------------------------
    // persistence
    // ------------------------------------------------------------------

    private Path stateFile() { return DrevClient.get().config().getDir().resolve("skin.json"); }

    private void save() {
        JsonObject o = new JsonObject();
        if (selected != null) o.addProperty("file", selected);
        o.addProperty("slim", slim);
        if (sourcePlayer != null) o.addProperty("player", sourcePlayer);
        try { Files.writeString(stateFile(), o.toString()); } catch (Exception ignored) { }
    }

    /** Re-applies the saved skin; called once the game window is ready. */
    public void load() {
        try {
            if (!Files.exists(stateFile())) return;
            JsonObject o = JsonParser.parseString(Files.readString(stateFile())).getAsJsonObject();
            slim = o.has("slim") && o.get("slim").getAsBoolean();
            if (o.has("player")) sourcePlayer = o.get("player").getAsString();
            if (o.has("file") && Files.exists(dir().resolve(o.get("file").getAsString()))) {
                apply(o.get("file").getAsString(), slim);
                status = "";
            }
        } catch (Exception ignored) { }
    }
}
