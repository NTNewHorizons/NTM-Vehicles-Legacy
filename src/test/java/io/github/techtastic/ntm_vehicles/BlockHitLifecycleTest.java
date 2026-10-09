package io.github.techtastic.ntm_vehicles;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.World;

import org.junit.Test;
import org.mockito.MockedStatic;

import com.thomass47.immersivevehicleslegacy.mcinterface1710.WrapperWorld;

import minecrafttransportsimulator.baseclasses.BlockHitResult;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityD_Definable;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet.BulletType;
import minecrafttransportsimulator.jsondefs.JSONConfigSettings;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONRendering;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.IInterfacePacket;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.systems.ConfigSystem;
import sun.misc.Unsafe;

/** Exercises collision detection and the next-tick block pass in the real IVL dispatcher. */
public class BlockHitLifecycleTest {

    @Test
    public void nuclearBlockImpactRunsOnce() throws Exception {
        assertBlockLifecycle("ntm_vehicles:nuke", true);
    }

    @Test
    public void gasBlockImpactRunsOnce() throws Exception {
        assertBlockLifecycle("ntm_vehicles:gas", true);
    }

    @Test
    public void napalmBlockImpactRunsOnce() throws Exception {
        assertBlockLifecycle("ntm_vehicles:napalm", true);
    }

    @Test
    public void nuclearBlockImpactRunsOnceWithExplosionsDisabled() throws Exception {
        assertBlockLifecycle("ntm_vehicles:nuke", false);
    }

    @Test
    public void gasBlockImpactRunsOnceWithExplosionsDisabled() throws Exception {
        assertBlockLifecycle("ntm_vehicles:gas", false);
    }

    @Test
    public void napalmBlockImpactRunsOnceWithExplosionsDisabled() throws Exception {
        assertBlockLifecycle("ntm_vehicles:napalm", false);
    }

    @SuppressWarnings("unchecked")
    private static void assertBlockLifecycle(String function, boolean explosions) throws Exception {
        Field registryField = EntityBullet.class.getDeclaredField("CUSTOM_HIT_FUNCTIONS");
        registryField.setAccessible(true);
        Map<String, EntityBullet.CustomHitFunction> registry = (Map<String, EntityBullet.CustomHitFunction>) registryField
            .get(null);
        Map<String, EntityBullet.CustomHitFunction> previous = new HashMap<>(registry);
        JSONConfigSettings previousSettings = ConfigSystem.settings;
        IInterfacePacket previousPackets = InterfaceManager.packetInterface;
        try (MockedStatic<NTMEffects> effects = mockStatic(NTMEffects.class)) {
            registry.clear();
            new NTMVehicles().preInit(null);
            ConfigSystem.settings = new JSONConfigSettings();
            ConfigSystem.settings.damage.bulletExplosions.value = explosions;
            ConfigSystem.settings.damage.packBulletDamageFactors.value.put("test", 1D);
            InterfaceManager.packetInterface = mock(IInterfacePacket.class);
            World actualWorld = mock(World.class);
            WrapperWorld world = mock(WrapperWorld.class);
            setField(WrapperWorld.class, world, "world", actualWorld);
            when(world.isInsideBorder(any(Point3D.class))).thenReturn(true);

            JSONBullet definition = new JSONBullet();
            definition.packID = "test";
            definition.systemName = "bullet";
            definition.bullet = new JSONBullet.Bullet();
            definition.bullet.diameter = 20;
            definition.bullet.types = Arrays.asList(BulletType.CUSTOM);
            definition.bullet.customHitFunctions = Arrays.asList(function);
            definition.bullet.impactDespawnTime = 5;
            definition.general = new JSONBullet.General();
            definition.rendering = new JSONRendering();
            JSONSubDefinition subDefinition = new JSONSubDefinition();
            subDefinition.subName = "";
            definition.definitions = Arrays.asList(subDefinition);
            ItemBullet item = (ItemBullet) unsafe().allocateInstance(ItemBullet.class);
            setField(AItemPack.class, item, "definition", definition);
            item.subDefinition = subDefinition;
            PartGun gun = (PartGun) unsafe().allocateInstance(PartGun.class);
            setField(AEntityA_Base.class, gun, "world", world);
            setField(AEntityA_Base.class, gun, "uniqueUUID", UUID.randomUUID());
            JSONPart gunDefinition = new JSONPart();
            gunDefinition.gun = new JSONPart.JSONPartGun();
            gunDefinition.gun.muzzleVelocity = 20;
            setField(AEntityD_Definable.class, gun, "definition", gunDefinition);
            gun.lastLoadedBullet = item;

            Point3D blockPosition = new Point3D(1, 2, -4);
            Point3D hitPosition = new Point3D(1.25, 2.5, -3.75);
            when(world.getBlockHit(any(Point3D.class), any(Point3D.class)))
                .thenAnswer(invocation -> new BlockHitResult(blockPosition.copy(), hitPosition.copy(), Axis.WEST));
            for (int bulletNumber = 1; bulletNumber <= 2; ++bulletNumber) {
                EntityBullet bullet = new EntityBullet(
                    new Point3D(),
                    new Point3D(1, 0, 0),
                    new RotationMatrix(),
                    gun,
                    bulletNumber);
                when(world.getBullet(gun.uniqueUUID, bulletNumber)).thenReturn(bullet);
                bullet.update();
                assertEquals(EntityBullet.HitType.BLOCK, bullet.lastHit);
                assertEquals(hitPosition, bullet.position);
                bullet.update();
                verifyEffect(effects, function, actualWorld, hitPosition, definition, bulletNumber);
                effects.verifyNoMoreInteractions();
            }
            verify(world, times(2)).getBlockHardness(blockPosition);
            verify(InterfaceManager.packetInterface, times(4)).sendToAllClients(any());
        } finally {
            registry.clear();
            registry.putAll(previous);
            ConfigSystem.settings = previousSettings;
            InterfaceManager.packetInterface = previousPackets;
        }
    }

    private static void verifyEffect(MockedStatic<NTMEffects> effects, String function, World world, Point3D position,
        JSONBullet definition, int count) {
        switch (function) {
            case "ntm_vehicles:nuke":
                effects.verify(() -> NTMEffects.nuke(world, position, definition), times(count));
                break;
            case "ntm_vehicles:gas":
                effects.verify(() -> NTMEffects.gas(world, position, definition), times(count));
                break;
            default:
                effects.verify(() -> NTMEffects.napalm(world, position), times(count));
        }
    }

    private static void setField(Class<?> owner, Object target, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Unsafe unsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }
}
