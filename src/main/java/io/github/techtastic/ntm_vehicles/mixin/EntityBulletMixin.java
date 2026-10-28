package io.github.techtastic.ntm_vehicles.mixin;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.techtastic.ntm_vehicles.NTMEffects;
import io.github.techtastic.ntm_vehicles.NTMVehicles;
import io.github.techtastic.ntm_vehicles.Util;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet.BulletType;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;

/**
 * Bridges the MTS40k stock nuclear shells to NTM's real nuclear explosions.
 *
 * <p>
 * Those shells declare {@code "types": ["explosive"]} and
 * {@code "constantValues": {"nuke": 1.0}}, but no {@code custom} type and no
 * {@code customHitFunctions}, so the pack API path never triggers for them and
 * they only produce a vanilla IVL explosion. This mixin intercepts the vanilla
 * explosion call for {@code EXPLOSIVE} bullets that carry a {@code nuke}
 * constant of 1 or more and replaces it with the NTM {@code EntityNukeExplosionMK5}
 * {@code +} {@code EntityNukeTorex} pair, same as {@code ntm_vehicles:nuke}.
 * </p>
 *
 * <p>
 * The redirect deliberately covers every impact type because the explosive
 * branch in {@code performGenericHitLogic} runs for block, entity, armor, and
 * burst hits alike; replacing the single {@code spawnExplosion} call keeps the
 * surrounding packet sync and bullet bookkeeping intact.
 * </p>
 */
@Mixin(value = EntityBullet.class, remap = false)
public abstract class EntityBulletMixin {

    private static final String GENERIC_HIT_LOGIC = "performGenericHitLogic(Lminecrafttransportsimulator/entities/instances/PartGun;ILminecrafttransportsimulator/baseclasses/Point3D;Lminecrafttransportsimulator/baseclasses/RotationMatrix;Lminecrafttransportsimulator/blocks/components/ABlockBase$Axis;Lminecrafttransportsimulator/entities/instances/EntityBullet$HitType;)V";

    /**
     * Captured per invocation, from the entry point, so that the static redirect
     * below can classify the bullet without local capture support. Only read
     * inside this target method, so a leftover value from a prior invocation
     * (non-destructive bullet) is always overwritten before the redirect can
     * observe it.
     */
    @Unique
    private static final ThreadLocal<JSONBullet> ntmVehicles$impactedDefinition = new ThreadLocal<>();

    @Inject(method = GENERIC_HIT_LOGIC, at = @At("HEAD"), require = 1)
    private static void ntmVehicles$captureImpactedDefinition(PartGun gun, int bulletNumber, Point3D position,
        RotationMatrix orientation, Axis hitSide, EntityBullet.HitType hitType, CallbackInfo ci) {
        ntmVehicles$impactedDefinition
            .set(gun != null && gun.lastLoadedBullet != null ? gun.lastLoadedBullet.definition : null);
    }

    @Redirect(
        method = GENERIC_HIT_LOGIC,
        at = @At(
            value = "INVOKE",
            target = "Lminecrafttransportsimulator/mcinterface/AWrapperWorld;spawnExplosion(Lminecrafttransportsimulator/baseclasses/Point3D;DZZ)V",
            remap = false),
        require = 1)
    private static void ntmVehicles$nuclearOrVanillaExplosion(AWrapperWorld world, Point3D explosionPosition,
        double blastSize, boolean incendiaryBreakBlocks, boolean breakBlocks) {
        JSONBullet definition = ntmVehicles$impactedDefinition.get();
        if (definition != null && definition.bullet.types.contains(BulletType.EXPLOSIVE)
            && Util.getConstantValue(definition, "nuke", 0D) >= 1D) {
            try {
                World vanillaWorld = Util.getWorld(world);
                NTMEffects.nuke(vanillaWorld, explosionPosition, definition);
                return;
            } catch (RuntimeException exception) {
                NTMVehicles.LOGGER
                    .warn("Falling back to a vanilla explosion because the NTM shell could not fire", exception);
            }
        }
        world.spawnExplosion(explosionPosition, blastSize, incendiaryBreakBlocks, breakBlocks);
    }
}
