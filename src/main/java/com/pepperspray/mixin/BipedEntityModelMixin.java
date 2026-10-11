package com.pepperspray.mixin;

import com.pepperspray.PepperSprayItem;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Third person (what other players see): the arm holding the spray points straight forward, following the aim. */
@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin<T extends LivingEntity> {
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void pepperspray$extendArm(T entity, float limbAngle, float limbDistance, float animationProgress,
                                       float headYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity player)) return;

        boolean mainIsSpray = player.getMainHandStack().getItem() instanceof PepperSprayItem;
        boolean offIsSpray = player.getOffHandStack().getItem() instanceof PepperSprayItem;
        if (!mainIsSpray && !offIsSpray) return;
        if (!PepperSprayItem.ARM_ALWAYS_FORWARD && !player.isUsingItem()) return;

        boolean mainIsRight = player.getMainArm() == Arm.RIGHT;
        boolean rightHolds = mainIsRight ? mainIsSpray : offIsSpray;
        boolean leftHolds = mainIsRight ? offIsSpray : mainIsSpray;

        float forward = -1.5707964f + this.head.pitch;   // -90 degrees = straight ahead, plus where the head looks
        if (rightHolds) {
            this.rightArm.pitch = forward;
            this.rightArm.yaw = 0.0f;
        }
        if (leftHolds) {
            this.leftArm.pitch = forward;
            this.leftArm.yaw = 0.0f;
        }
    }
}
