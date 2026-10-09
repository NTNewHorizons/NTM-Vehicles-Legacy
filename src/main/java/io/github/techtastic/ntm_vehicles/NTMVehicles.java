package io.github.techtastic.ntm_vehicles;

import net.minecraft.world.World;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderException;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import minecrafttransportsimulator.entities.instances.EntityBullet;

@Mod(
    modid = NTMVehicles.MOD_ID,
    name = NTMVehicles.MOD_NAME,
    version = Tags.VERSION,
    dependencies = "required-after:immersivevehicleslegacy@[0.1.0-ntmv2];required-after:hbm@[1.0.27,)",
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*")
public class NTMVehicles {

    public static final String MOD_ID = "ntm_vehicles";
    public static final String MOD_NAME = "NTM: Vehicles";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Forge's range parser cannot express versions containing a closing parenthesis.
        String hbmVersion = Loader.instance()
            .getIndexedModList()
            .get("hbm")
            .getVersion();
        if (!"1.0.27 BETA (5808)".equals(hbmVersion)) {
            throw new LoaderException(
                "NTM: Vehicles requires HBM 1.0.27_X5808 (runtime version " + "1.0.27 BETA (5808)); found "
                    + hbmVersion);
        }
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        EntityBullet.registerCustomHitFunction("ntm_vehicles:nuke", (world, pos, side, type, bullet) -> {
            World actualWorld = Util.getWorld(world);
            if (actualWorld != null && !actualWorld.isRemote) {
                NTMEffects.nuke(actualWorld, pos, bullet.definition);
            }
        });
        EntityBullet.registerCustomHitFunction("ntm_vehicles:gas", (world, pos, side, type, bullet) -> {
            World actualWorld = Util.getWorld(world);
            if (actualWorld != null && !actualWorld.isRemote) {
                NTMEffects.gas(actualWorld, pos, bullet.definition);
            }
        });
        EntityBullet.registerCustomHitFunction("ntm_vehicles:napalm", (world, pos, side, type, bullet) -> {
            World actualWorld = Util.getWorld(world);
            if (actualWorld != null && !actualWorld.isRemote) {
                NTMEffects.napalm(actualWorld, pos);
            }
        });
    }
}
