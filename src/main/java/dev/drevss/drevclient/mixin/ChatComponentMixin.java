package dev.drevss.drevclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.misc.BetterChat;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
        at = @At("HEAD"), cancellable = true)
    private void drev$onMessage(Component ignored, @Nullable MessageSignature signature, @Nullable GuiMessageTag tag,
                                CallbackInfo ci, @Local(argsOnly = true) LocalRef<Component> message) {
        if (DrevClient.get() == null) return;
        BetterChat chat = DrevClient.get().modules().get(BetterChat.class);
        if (!chat.isEnabled()) return;
        Component result = chat.process(message.get());
        if (result == null) {
            ci.cancel();
            return;
        }
        message.set(result);
    }
}
