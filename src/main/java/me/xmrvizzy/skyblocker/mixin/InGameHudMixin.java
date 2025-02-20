package me.xmrvizzy.skyblocker.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import me.xmrvizzy.skyblocker.SkyblockerMod;
import me.xmrvizzy.skyblocker.config.SkyblockerConfig;
import me.xmrvizzy.skyblocker.skyblock.FancyStatusBars;
import me.xmrvizzy.skyblocker.skyblock.HotbarSlotLock;
import me.xmrvizzy.skyblocker.skyblock.StatusBarTracker;
import me.xmrvizzy.skyblocker.skyblock.dungeon.DungeonMap;
import me.xmrvizzy.skyblocker.utils.Utils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public abstract class InGameHudMixin extends DrawableHelper {
    @Unique
    private static final Identifier SLOT_LOCK = new Identifier(SkyblockerMod.NAMESPACE, "textures/gui/slot_lock.png");
    @Unique

    private final StatusBarTracker statusBarTracker = SkyblockerMod.getInstance().statusBarTracker;
    @Unique
    private final FancyStatusBars statusBars = new FancyStatusBars();
    @Unique
    private MatrixStack hotbarMatrices;
    @Unique
    private int hotbarSlotIndex;

    @Shadow
    private int scaledHeight;
    @Shadow
    private int scaledWidth;

    @Shadow
    public void setOverlayMessage(Text message, boolean tinted) {
    }

    @Inject(method = "setOverlayMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"), cancellable = true)
    private void skyblocker$onSetOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
        if (!Utils.isOnSkyblock || !SkyblockerConfig.get().general.bars.enableBars)
            return;
        String msg = message.getString();
        String res = statusBarTracker.update(msg, SkyblockerConfig.get().messages.hideMana);
        if (!msg.equals(res)) {
            if (res != null)
                setOverlayMessage(Text.of(res), tinted);
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"))
    public void skyblocker$renderHotbar(float f, MatrixStack matrices, CallbackInfo ci) {
        if (Utils.isOnSkyblock) {
            hotbarMatrices = matrices;
            hotbarSlotIndex = 0;
        }
    }

    @Inject(method = "renderHotbarItem", at = @At("HEAD"))
    public void skyblocker$renderHotbarItem(MatrixStack matrices, int i, int j, float f, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        if (Utils.isOnSkyblock) {
            if (HotbarSlotLock.isLocked(hotbarSlotIndex)) {
                RenderSystem.setShaderTexture(0, SLOT_LOCK);
                DrawableHelper.drawTexture(hotbarMatrices, i, j, 0, 0, 16, 16);
            }
            hotbarSlotIndex++;
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void skyblocker$renderExperienceBar(MatrixStack matrices, int x, CallbackInfo ci) {
        if (Utils.isOnSkyblock && SkyblockerConfig.get().general.bars.enableBars)
            ci.cancel();
    }

    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void skyblocker$renderStatusBars(MatrixStack matrices, CallbackInfo ci) {
        if (!Utils.isOnSkyblock)
            return;
        if (statusBars.render(matrices, scaledWidth, scaledHeight))
            ci.cancel();

        if (Utils.isInDungeons && SkyblockerConfig.get().locations.dungeons.enableMap)
            DungeonMap.render(matrices);

        RenderSystem.setShaderTexture(0, GUI_ICONS_TEXTURE);
    }

    @Inject(method = "renderMountHealth", at = @At("HEAD"), cancellable = true)
    private void skyblocker$renderMountHealth(MatrixStack matrices, CallbackInfo ci) {
        if (Utils.isOnSkyblock && SkyblockerConfig.get().general.bars.enableBars)
            ci.cancel();
    }
}