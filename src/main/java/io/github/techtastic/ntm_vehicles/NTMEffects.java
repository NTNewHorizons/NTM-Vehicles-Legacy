package io.github.techtastic.ntm_vehicles;

import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.particle.EntityChlorineFX;
import com.hbm.entity.particle.EntityCloudFX;
import com.hbm.entity.particle.EntityModFX;
import com.hbm.entity.particle.EntityOrangeFX;
import com.hbm.entity.particle.EntityPinkCloudFX;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.jsondefs.JSONBullet;

/** Adapts the upstream effects to the user's 1.7.10 NTM API. */
final class NTMEffects {

    private NTMEffects() {}

    static void nuke(World world, Point3D pos, JSONBullet definition) {
        int strength = (int) Util.getBlastSize(definition);
        world.spawnEntityInWorld(EntityNukeExplosionMK5.statFac(world, strength, pos.x, pos.y, pos.z));
        EntityNukeTorex.statFacStandard(world, pos.x, pos.y, pos.z, strength);
    }

    static void gas(World world, Point3D pos, JSONBullet definition) {
        double speed = Util.getConstantValue(definition, "gasSpreadSpeed", 1.25);
        int gasType = (int) Util.getConstantValue(definition, "gasType", 0);
        int count = (int) Util.getBlastSize(definition);
        // The 1.12.2 spawnChlorine helper is absent in this NTM version. Preserve
        // its exact type mapping and Gaussian velocity using NTM's own entities.
        for (int i = 0; i < count; ++i) {
            EntityModFX gas = createGas(world, pos, gasType);
            gas.motionY = world.rand.nextGaussian() * speed;
            gas.motionX = world.rand.nextGaussian() * speed;
            gas.motionZ = world.rand.nextGaussian() * speed;
            world.spawnEntityInWorld(gas);
        }
    }

    static EntityModFX createGas(World world, Point3D pos, int gasType) {
        switch (gasType) {
            case 0:
                return new EntityChlorineFX(world, pos.x, pos.y, pos.z, 0, 0, 0);
            case 1:
                return new EntityCloudFX(world, pos.x, pos.y, pos.z, 0, 0, 0);
            case 2:
                return new EntityPinkCloudFX(world, pos.x, pos.y, pos.z, 0, 0, 0);
            default:
                return new EntityOrangeFX(world, pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }

    static void napalm(World world, Point3D pos) {
        ExplosionLarge.explode(world, pos.x, pos.y, pos.z, 2.5F, false, false, false);
        // BlockPos(double, double, double) floors coordinates, including negatives.
        int x = MathHelper.floor_double(pos.x);
        int y = MathHelper.floor_double(pos.y);
        int z = MathHelper.floor_double(pos.z);
        ExplosionChaos.igniteAllBlocks(world, x, y, z, 9);
        ExplosionChaos.igniteFlammableBlocks(world, x, y, z, 14);
        for (int i = 0; i < 5; ++i) {
            ExplosionLarge.spawnBurst(
                world,
                pos.x,
                pos.y + 1,
                pos.z,
                world.rand.nextInt(10) + 15,
                world.rand.nextFloat() * 2 + 2);
        }
    }
}
