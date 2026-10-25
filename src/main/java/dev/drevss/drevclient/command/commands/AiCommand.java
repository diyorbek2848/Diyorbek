package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.gui.DrevAIScreen;
import dev.drevss.drevclient.module.client.DrevAIModule;
import dev.drevss.drevclient.util.ChatUtil;

public class AiCommand extends Command {
    public AiCommand() {
        super("ai", "ai <question> | ai key <key> | ai model <model> | ai open | ai reset",
            "Ask DrevAI anything about the client or the game (it can search the web)", "drevai", "ask");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length < 1) throw usage();
        DrevAIModule ai = DrevClient.get().modules().get(DrevAIModule.class);
        switch (args[0].toLowerCase()) {
            case "key" -> {
                if (args.length < 2) throw usage();
                ai.apiKey.set(args[1].trim());
                DrevClient.get().config().save("default");
                ChatUtil.success("DrevAI API key saved");
                return;
            }
            case "model" -> {
                if (args.length < 2 || !ai.model.parse(args[1])) throw new CommandException("Models: " + ai.model.getModes());
                ChatUtil.success("DrevAI model: " + ai.model.get());
                return;
            }
            case "open" -> {
                mc.execute(() -> mc.setScreen(new DrevAIScreen()));
                return;
            }
            case "reset", "clear" -> {
                DrevClient.get().ai().reset();
                ChatUtil.success("DrevAI conversation cleared");
                return;
            }
            default -> { }
        }
        String question = join(args, 0);
        ChatUtil.raw("§d[DrevAI] §7Thinking...");
        DrevClient.get().ai().ask(question, answer -> {
            for (String line : answer.split("\n")) {
                if (!line.isBlank()) ChatUtil.raw("§d[DrevAI] §f" + line);
            }
        });
    }
}
