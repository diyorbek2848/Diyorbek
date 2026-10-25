package dev.drevss.drevclient.module.misc;

import com.google.gson.JsonObject;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.TextSetting;
import dev.drevss.drevclient.util.ChatUtil;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Discord Rich Presence over Discord's local IPC socket (no extra library).
 * Create an application at discord.com/developers, copy its Application ID
 * into the AppId setting, and optionally upload an art asset named "drevclient".
 */
public class DiscordPresence extends Module {
    private final TextSetting appId = add(new TextSetting("AppId", "Your Discord application ID", ""));
    private final BoolSetting showServer = add(new BoolSetting("ShowServer", "Show the server address", true));
    private final TextSetting line = add(new TextSetting("Text", "Custom second line", "Using DrevClient"));

    private volatile boolean running;
    private Thread thread;
    private final long started = System.currentTimeMillis() / 1000L;

    public DiscordPresence() {
        super("DiscordRPC", "Shows DrevClient in your Discord status", Category.MISC);
    }

    @Override
    public void onEnable() {
        if (appId.get().isBlank()) {
            ChatUtil.error("Set your Discord application id first: .settings DiscordRPC AppId <id>");
            setEnabledSilently(false);
            return;
        }
        running = true;
        thread = new Thread(this::loop, "DrevClient-Discord");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void onDisable() {
        running = false;
        if (thread != null) thread.interrupt();
    }

    private void loop() {
        while (running) {
            try (Ipc ipc = Ipc.open()) {
                JsonObject hello = new JsonObject();
                hello.addProperty("v", 1);
                hello.addProperty("client_id", appId.get().trim());
                ipc.send(0, hello);
                ipc.read();
                while (running) {
                    ipc.send(1, activity());
                    ipc.read();
                    Thread.sleep(15000);
                }
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                try { Thread.sleep(20000); } catch (InterruptedException ie) { return; }
            }
        }
    }

    private JsonObject activity() {
        String details;
        if (mc.level == null) details = "In the menus";
        else if (mc.hasSingleplayerServer()) details = "Playing singleplayer";
        else if (showServer.isOn() && mc.getCurrentServer() != null) details = "Playing on " + mc.getCurrentServer().ip;
        else details = "Playing multiplayer";
        long enabled = DrevClient.get().modules().all().stream().filter(Module::isEnabled).count();

        JsonObject timestamps = new JsonObject();
        timestamps.addProperty("start", started);
        JsonObject assets = new JsonObject();
        assets.addProperty("large_image", "drevclient");
        assets.addProperty("large_text", "DrevClient " + DrevClient.VERSION);
        JsonObject act = new JsonObject();
        act.addProperty("details", details);
        act.addProperty("state", line.get() + " • " + enabled + " modules");
        act.add("timestamps", timestamps);
        act.add("assets", assets);

        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        args.add("activity", act);
        JsonObject cmd = new JsonObject();
        cmd.addProperty("cmd", "SET_ACTIVITY");
        cmd.add("args", args);
        cmd.addProperty("nonce", UUID.randomUUID().toString());
        return cmd;
    }

    /** Minimal Discord IPC client: frames are [opcode LE int][length LE int][json]. */
    private interface Ipc extends AutoCloseable {
        void write(ByteBuffer buf) throws IOException;
        int readInto(ByteBuffer buf) throws IOException;

        default void send(int op, JsonObject json) throws IOException {
            byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);
            ByteBuffer buf = ByteBuffer.allocate(8 + body.length).order(ByteOrder.LITTLE_ENDIAN);
            buf.putInt(op).putInt(body.length).put(body).flip();
            write(buf);
        }

        default void read() throws IOException {
            ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            while (header.hasRemaining()) if (readInto(header) < 0) throw new IOException("closed");
            header.flip();
            header.getInt();
            int len = header.getInt();
            ByteBuffer body = ByteBuffer.allocate(Math.max(0, len));
            while (body.hasRemaining()) if (readInto(body) < 0) throw new IOException("closed");
        }

        @Override void close() throws IOException;

        static Ipc open() throws IOException {
            boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
            for (int i = 0; i < 10; i++) {
                try {
                    if (windows) {
                        RandomAccessFile pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                        return new Ipc() {
                            public void write(ByteBuffer b) throws IOException { pipe.write(b.array(), b.position(), b.remaining()); }
                            public int readInto(ByteBuffer b) throws IOException {
                                int n = pipe.read(b.array(), b.position(), b.remaining());
                                if (n > 0) b.position(b.position() + n);
                                return n;
                            }
                            public void close() throws IOException { pipe.close(); }
                        };
                    }
                    for (Path dir : unixDirs()) {
                        Path sock = dir.resolve("discord-ipc-" + i);
                        if (!Files.exists(sock)) continue;
                        SocketChannel ch = SocketChannel.open(StandardProtocolFamily.UNIX);
                        ch.connect(UnixDomainSocketAddress.of(sock));
                        return new Ipc() {
                            public void write(ByteBuffer b) throws IOException { while (b.hasRemaining()) ch.write(b); }
                            public int readInto(ByteBuffer b) throws IOException { return ch.read(b); }
                            public void close() throws IOException { ch.close(); }
                        };
                    }
                } catch (IOException ignored) { }
            }
            throw new IOException("Discord is not running");
        }

        private static Path[] unixDirs() {
            String base = System.getenv("XDG_RUNTIME_DIR");
            if (base == null) base = System.getenv("TMPDIR");
            if (base == null) base = "/tmp";
            Path b = Path.of(base);
            return new Path[] { b, b.resolve("app/com.discordapp.Discord"), b.resolve("snap.discord"), Path.of("/tmp") };
        }
    }
}
