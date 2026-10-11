package com.pepperspray;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Vector3f;

/**
 * Hold right-click to spray (one-handed, normal item pose). Unlimited uses.
 * Reach = 15 blocks, the same as a skeleton's bow range. Walls stop the spray.
 * Every living entity hit takes one heart every 0.5s for 15 seconds (+ 5s slowness and blindness).
 */
public class PepperSprayItem extends Item {
    /** Other players see the holding arm stretched forward. false = only while spraying. */
    public static final boolean ARM_ALWAYS_FORWARD = true;

    private static final int EFFECT_TICKS = 100;      // slowness / blindness: 5 seconds
    private static final int BURN_TICKS = 300;        // pepper poison: 15 seconds
    private static final double RANGE = 15.0;         // skeleton shooting range
    private static final double BASE_WIDTH = 0.8;     // spray width at the nozzle (blocks)
    private static final double SPREAD = 0.12;        // extra half-width per block of distance
    private static final DustParticleEffect ORANGE =
            new DustParticleEffect(new Vector3f(1.0f, 0.5f, 0.05f), 1.6f);

    public PepperSprayItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }

    /** NONE = ordinary one-handed item pose (no bow/crossbow two-hand pose). */
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient || !(world instanceof ServerWorld sw)) return;

        Vec3d eye = user.getEyePos();
        Vec3d dir = user.getRotationVec(1.0f);

        // The spray stops at the first solid block
        Vec3d farEnd = eye.add(dir.multiply(RANGE));
        BlockHitResult wall = world.raycast(new RaycastContext(eye, farEnd,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, user));
        double reach = wall.getType() == HitResult.Type.MISS ? RANGE : wall.getPos().distanceTo(eye);

        // Mist leaves the nozzle: a bit forward, down and toward the hand that holds the can
        boolean rightSide = (user.getActiveHand() == Hand.MAIN_HAND) == (user.getMainArm() == Arm.RIGHT);
        Vec3d right = dir.crossProduct(new Vec3d(0, 1, 0)).normalize();
        Vec3d nozzle = eye.add(dir.multiply(0.5)).add(right.multiply(rightSide ? 0.3 : -0.3)).add(0, -0.3, 0);

        for (double d = 0; d <= reach; d += 1.0) {
            Vec3d p = nozzle.add(dir.multiply(d));
            double spread = 0.04 + d * 0.03;
            sw.spawnParticles(ORANGE, p.x, p.y, p.z, 4, spread, spread, spread, 0.01);
        }

        int used = getMaxUseTime(stack) - remainingUseTicks;
        if (used % 4 == 0) {
            world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_FIRE_EXTINGUISH,
                    SoundCategory.PLAYERS, 0.5f, 2.0f);
        }

        // Apply effects every 5 ticks
        if (used % 5 == 0) {
            double maxWidth = BASE_WIDTH + reach * SPREAD;
            Box box = new Box(eye, eye.add(dir.multiply(reach))).expand(maxWidth + 1.0);
            for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box,
                    x -> x != user && x.isAlive())) {
                Vec3d to = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(eye);
                double along = to.dotProduct(dir);
                if (along < 0 || along > reach + 0.5) continue;
                double off = to.subtract(dir.multiply(along)).length();
                if (off > BASE_WIDTH + along * SPREAD + e.getWidth() * 0.5) continue;

                // Only (re)start the burn when it is gone or nearly over, so the
                // damage ticks keep their rhythm while spraying.
                StatusEffectInstance burn = e.getStatusEffect(PepperSprayMod.PEPPER_BURN);
                if (burn == null || burn.getDuration() <= 5) {
                    e.addStatusEffect(new StatusEffectInstance(PepperSprayMod.PEPPER_BURN, BURN_TICKS, 0), user);
                }
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, EFFECT_TICKS, 1), user);
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, EFFECT_TICKS, 0), user);
            }
        }
    }
}
