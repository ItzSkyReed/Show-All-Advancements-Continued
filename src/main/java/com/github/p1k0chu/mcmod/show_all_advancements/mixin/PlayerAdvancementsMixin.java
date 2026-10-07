package com.github.p1k0chu.mcmod.show_all_advancements.mixin;

import com.github.p1k0chu.mcmod.show_all_advancements.IServerAdvancementManager;
import com.github.p1k0chu.mcmod.show_all_advancements.ShowAllAdvsEntry;
import com.github.p1k0chu.mcmod.show_all_advancements.ducks.PlayerAdvancementsDuck;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.advancements.*;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin implements PlayerAdvancementsDuck {
    @Shadow
    @Final
    private Set<AdvancementHolder> visible;

    @Shadow
    private boolean isFirstPacket;

    @Shadow
    protected abstract void markForVisibilityUpdate(AdvancementHolder advancement);

    @WrapOperation(method = "updateTreeVisibility", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/advancements/AdvancementVisibilityEvaluator;evaluateVisibility(Lnet/minecraft/advancements/AdvancementNode;Ljava/util/function/Predicate;Lnet/minecraft/server/advancements/AdvancementVisibilityEvaluator$Output;)V"))
    private void updateTreeVisibility(AdvancementNode node, Predicate<AdvancementNode> isDone, AdvancementVisibilityEvaluator.Output output, Operation<Void> original) {
        Predicate<AdvancementNode> pred = advNode -> {
            Optional<DisplayInfo> displayInfo = advNode.advancement().display();
            if (displayInfo.isEmpty()) return false;

            boolean nonHidden = !displayInfo.get().hidden();
            return nonHidden || isDone.test(advNode) || ShowAllAdvsEntry.getInstance().showsThisHidden(advNode.holder().id().toString());
        };
        original.call(node, pred, output);
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void ensureRootsVisible(ServerAdvancementManager manager, CallbackInfo ci) {
        Iterable<AdvancementNode> roots = ((IServerAdvancementManager) manager).show_all_advancements$getRoots();

        roots.forEach(node -> markForVisibilityUpdate(node.holder()));
    }

    @Override
    public void show_all_advancements$clearVisible() {
        if (this.visible.isEmpty()) {
            return;
        }

        this.isFirstPacket = true;
        this.visible.clear();
    }
}
