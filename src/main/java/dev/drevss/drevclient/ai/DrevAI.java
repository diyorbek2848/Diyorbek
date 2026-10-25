package dev.drevss.drevclient.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.hud.HudElement;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.module.client.DrevAIModule;
import dev.drevss.drevclient.setting.Setting;
import dev.drevss.drevclient.util.KeyUtil;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * DrevAI talks to the Claude API from your own PC, with your own API key.
 * It gets a full description of DrevClient (every module, setting and command)
 * plus optional live game context, and can use Anthropic's web search tool.
 */
public class DrevAI {
    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final int MAX_HISTORY = 20;

    public record ChatLine(boolean fromUser, String text) { }

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final JsonArray history = new JsonArray();
    private final List<ChatLine> transcript = new ArrayList<>();
    private volatile boolean busy;

    public List<ChatLine> transcript() { return transcript; }
    public boolean isBusy() { return busy; }

    public void reset() {
        synchronized (history) {
            while (history.size() > 0) history.remove(0);
        }
        transcript.clear();
    }

    private DrevAIModule settings() { return DrevClient.get().modules().get(DrevAIModule.class); }

    /**
     * Asks a question on a background thread. The callback runs on the render
     * thread with the answer text (or an error message).
     */
    public void ask(String question, Consumer<String> callback) {
        String key = settings().apiKey.get().trim();
        transcript.add(new ChatLine(true, question));
        if (key.isEmpty()) {
            String msg = "No API key set. Get one at console.anthropic.com, then type: .ai key <your key>";
            transcript.add(new ChatLine(false, msg));
            callback.accept(msg);
            return;
        }
        if (busy) {
            callback.accept("Still thinking about the last question...");
            return;
        }
        busy = true;
        String systemPrompt = buildSystemPrompt();
        Thread t = new Thread(() -> {
            String answer;
            try {
                answer = request(key, systemPrompt, question);
            } catch (Exception e) {
                answer = "DrevAI error: " + e.getMessage();
                DrevClient.LOGGER.warn("DrevAI request failed", e);
            }
            String finalAnswer = answer;
            Minecraft.getInstance().execute(() -> {
                transcript.add(new ChatLine(false, finalAnswer));
                busy = false;
                callback.accept(finalAnswer);
            });
        }, "DrevAI");
        t.setDaemon(true);
        t.start();
    }

    private String request(String key, String systemPrompt, String question) throws Exception {
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", question);

        JsonArray messages;
        synchronized (history) {
            history.add(userMsg);
            while (history.size() > MAX_HISTORY) history.remove(0);
            // the history must start with a user turn
            while (history.size() > 0 && !"user".equals(history.get(0).getAsJsonObject().get("role").getAsString()))
                history.remove(0);
            messages = history.deepCopy();
        }

        StringBuilder text = new StringBuilder();
        JsonArray lastContent = null;
        for (int round = 0; round < 4; round++) {
            JsonObject body = new JsonObject();
            body.addProperty("model", settings().model.get());
            body.addProperty("max_tokens", 1500);
            body.addProperty("system", systemPrompt);
            body.add("messages", messages);
            if (settings().webSearch.isOn()) {
                JsonObject tool = new JsonObject();
                tool.addProperty("type", "web_search_20250305");
                tool.addProperty("name", "web_search");
                tool.addProperty("max_uses", 5);
                JsonArray tools = new JsonArray();
                tools.add(tool);
                body.add("tools", tools);
            }

            HttpRequest req = HttpRequest.newBuilder(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(120))
                .header("x-api-key", key)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
            if (resp.statusCode() != 200) {
                String err = json.has("error") ? json.getAsJsonObject("error").get("message").getAsString() : resp.body();
                synchronized (history) { if (history.size() > 0) history.remove(history.size() - 1); }
                return "API error " + resp.statusCode() + ": " + err;
            }

            JsonArray content = json.getAsJsonArray("content");
            lastContent = content;
            for (JsonElement el : content) {
                JsonObject block = el.getAsJsonObject();
                if ("text".equals(block.get("type").getAsString())) text.append(block.get("text").getAsString());
            }
            String stop = json.has("stop_reason") && !json.get("stop_reason").isJsonNull() ? json.get("stop_reason").getAsString() : "";
            if (!"pause_turn".equals(stop)) break;

            // long web searches pause; send the partial turn back so Claude can continue
            JsonObject partial = new JsonObject();
            partial.addProperty("role", "assistant");
            partial.add("content", content);
            messages.add(partial);
        }

        if (lastContent != null) {
            JsonObject assistant = new JsonObject();
            assistant.addProperty("role", "assistant");
            assistant.addProperty("content", text.length() > 0 ? text.toString() : "(no answer)");
            synchronized (history) { history.add(assistant); }
        }
        String answer = text.toString().trim();
        return answer.isEmpty() ? "(DrevAI returned no text)" : answer;
    }

    // ------------------------------------------------------------------
    // Knowledge about the client
    // ------------------------------------------------------------------

    private static String dimensionName(Minecraft mc) {
        var dim = mc.level.dimension();
        if (dim == net.minecraft.world.level.Level.NETHER) return "the_nether";
        if (dim == net.minecraft.world.level.Level.END) return "the_end";
        if (dim == net.minecraft.world.level.Level.OVERWORLD) return "overworld";
        return "custom";
    }

    private String buildSystemPrompt() {
        DrevClient c = DrevClient.get();
        String prefix = c.commands().getPrefix();
        StringBuilder sb = new StringBuilder();
        sb.append("You are DrevAI, the assistant built into DrevClient ").append(DrevClient.VERSION)
            .append(", a Fabric utility client for Minecraft Java 1.21.11 made by DrevSS. ")
            .append("You know every DrevClient feature listed below and you can search the web for anything else ")
            .append("(Minecraft mechanics, recipes, server plugins, Fabric modding, news).\n\n")
            .append("Answer style: you are shown inside a small in-game window, so be short and practical. ")
            .append("Plain text only, no markdown tables or headers. ")
            .append("When a DrevClient command would help, put it on its own line starting with '>> ' followed by the exact command, ")
            .append("for example:\n>> ").append(prefix).append("toggle KillAura\n")
            .append("The player can click those lines to run them. Only suggest commands that exist below. ")
            .append("The player uses DrevClient on their own server and in singleplayer; if they ask how to use it on servers ")
            .append("where cheating is against the rules, remind them it can get them banned.\n\n");

        sb.append("GENERAL: Right Shift opens the ClickGUI (left click = toggle module, right click = settings, ")
            .append("middle click = bind a key, drag headers to move panels, type to search). ")
            .append("The ClickGUI has buttons for the HUD editor and DrevAI. Config auto-saves to .minecraft/config/drevclient/. ")
            .append("Command prefix is '").append(prefix).append("'. Middle-clicking a player toggles them as a friend.\n\n");

        sb.append("MODULES (name [current state] - description; settings as name=value (description)):\n");
        for (Category cat : Category.values()) {
            sb.append("== ").append(cat.displayName).append(" ==\n");
            for (Module m : c.modules().byCategory(cat)) {
                sb.append("- ").append(m.getName()).append(m.isEnabled() ? " [ON]" : " [off]");
                if (m.getKey() >= 0) sb.append(" key=").append(KeyUtil.name(m.getKey()));
                sb.append(" - ").append(m.getDescription());
                if (!m.getSettings().isEmpty()) {
                    sb.append(" | ");
                    List<String> parts = new ArrayList<>();
                    for (Setting<?> s : m.getSettings()) {
                        if (s.getName().equals("ApiKey")) { parts.add("ApiKey=(hidden)"); continue; }
                        parts.add(s.getName() + "=" + s.getDisplayValue() + " (" + s.getDescription() + ")");
                    }
                    sb.append(String.join("; ", parts));
                }
                sb.append('\n');
            }
        }

        sb.append("\nCOMMANDS:\n");
        for (Command cmd : c.commands().all()) {
            sb.append("- ").append(prefix).append(cmd.getSyntax()).append(" - ").append(cmd.getDescription()).append('\n');
        }

        sb.append("\nHUD ELEMENTS (move them in the HUD editor, right click to hide/show): ");
        List<String> hud = new ArrayList<>();
        for (HudElement e : c.hud().elements()) hud.add(e.getName() + (e.isVisible() ? "" : " (hidden)"));
        sb.append(String.join(", ", hud)).append('\n');

        if (settings().gameContext.isOn()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                sb.append("\nLIVE GAME CONTEXT: dimension=").append(dimensionName(mc))
                    .append(String.format(", position=%.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ()))
                    .append(", health=").append(String.format("%.1f", mc.player.getHealth()))
                    .append(", food=").append(mc.player.getFoodData().getFoodLevel())
                    .append(", gamemode=").append(mc.gameMode != null ? mc.gameMode.getPlayerMode().getName() : "?")
                    .append(", server=").append(mc.hasSingleplayerServer() ? "singleplayer"
                        : mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "unknown")
                    .append(", held item=").append(mc.player.getMainHandItem().getHoverName().getString())
                    .append(", friends=").append(String.join(",", c.friends().all()))
                    .append('\n');
            }
        }
        return sb.toString();
    }
}
