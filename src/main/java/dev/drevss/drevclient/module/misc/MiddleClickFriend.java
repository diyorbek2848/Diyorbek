package dev.drevss.drevclient.module.misc;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.util.ChatUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

public class MiddleClickFriend extends Module {
    public MiddleClickFriend() {
        super("MiddleClickFriend", "Middle-click a player to add or remove them as a friend", Category.MISC);
        setEnabledSilently(true);
        setDrawn(false);
    }

    public void onMiddleClick() {
        if (!isEnabled() || mc.player == null || mc.screen != null) return;
        if (!(mc.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof Player p)) return;
        String name = p.getGameProfile().name();
        var friends = DrevClient.get().friends();
        if (friends.isFriend(name)) {
            friends.remove(name);
            ChatUtil.info("Removed §b" + name + "§7 from friends");
        } else {
            friends.add(name);
            ChatUtil.success("Added §b" + name + "§a as a friend");
        }
        DrevClient.get().config().markDirty();
    }
}
