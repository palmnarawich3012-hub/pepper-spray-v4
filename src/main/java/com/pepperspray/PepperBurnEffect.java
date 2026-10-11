package com.pepperspray;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/** Deals one heart (2.0 HP) every 0.5 second to ANY living entity, undead included. Can kill. */
public class PepperBurnEffect extends StatusEffect {
    public static final float DAMAGE = 2.0f;     // 2.0 = one heart
    public static final int INTERVAL_TICKS = 10; // 10 ticks = 0.5 second (10 is the fastest the game allows: hit-immunity)

    public PepperBurnEffect() {
        super(StatusEffectCategory.HARMFUL, 0xFF8000);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration % INTERVAL_TICKS == 0;
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        entity.damage(entity.getDamageSources().magic(), DAMAGE);
    }
}
