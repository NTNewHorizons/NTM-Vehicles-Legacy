package io.github.techtastic.ntm_vehicles;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.google.gson.Gson;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.entity.particle.EntityChlorineFX;
import com.hbm.entity.particle.EntityCloudFX;
import com.hbm.entity.particle.EntityOrangeFX;
import com.hbm.entity.particle.EntityPinkCloudFX;
import com.hbm.main.MainRegistry;
import com.thomass47.immersivevehicleslegacy.mcinterface1710.WrapperWorld;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderException;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import cpw.mods.fml.common.versioning.VersionParser;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONVariableModifier;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import sun.misc.Unsafe;

public class PackApiCompatibilityTest {
    @Test
    public void unchangedPackJsonRetainsBlastAndGasConstants() {
        JSONBullet bullet = new Gson().fromJson("{\"bullet\":{\"diameter\":120,\"blastStrength\":8,"
            + "\"types\":[\"CUSTOM\"],\"customHitFunctions\":[\"ntm_vehicles:gas\"]},"
            + "\"constantValues\":{\"gasType\":2,\"gasSpreadSpeed\":0.75}}", JSONBullet.class);
        assertEquals(Arrays.asList("ntm_vehicles:gas"), bullet.bullet.customHitFunctions);
        assertEquals(8, Util.getBlastSize(bullet), 0);
        assertEquals(2, Util.getConstantValue(bullet, "gasType", 0), 0);
        assertEquals(0.75, Util.getConstantValue(bullet, "gasSpreadSpeed", 1.25), 0);
    }

    @Test
    public void absentConstantsAndZeroStrengthKeepOriginalDefaults() {
        JSONBullet bullet = bullet(120, 0);
        assertEquals(12, Util.getBlastSize(bullet), 0);
        assertEquals(0, Util.getConstantValue(bullet, "gasType", 0), 0);
        assertEquals(1.25, Util.getConstantValue(bullet, "gasSpreadSpeed", 1.25), 0);
        bullet.constantValues = new HashMap<>();
        assertEquals(1.25, Util.getConstantValue(bullet, "gasSpreadSpeed", 1.25), 0);
    }

    @Test
    public void variableModifierHelperKeepsNullableAndNamedLookup() {
        JSONBullet bullet = bullet(120, 0);
        assertNull(Util.getVariableModifier(bullet, "gasType"));
        JSONVariableModifier modifier = new JSONVariableModifier();
        modifier.variable = "gasType";
        bullet.variableModifiers = Arrays.asList(modifier);
        assertSame(modifier, Util.getVariableModifier(bullet, "gasType"));
        assertNull(Util.getVariableModifier(bullet, "missing"));
    }

    @Test
    public void actualIvlWrapperUnwrapsWithoutConstructingAWorld() throws Exception {
        World world = mockWorld(false);
        assertSame(world, Util.getWorld(wrap(world)));
    }

    @Test
    public void gasTypeNumbersSelectTheSameNtmEntities() throws Exception {
        World world = mockWorld(false);
        Point3D pos = new Point3D(1.5, 2.5, -3.5);
        assertEquals(EntityChlorineFX.class, NTMEffects.createGas(world, pos, 0).getClass());
        assertEquals(EntityCloudFX.class, NTMEffects.createGas(world, pos, 1).getClass());
        assertEquals(EntityPinkCloudFX.class, NTMEffects.createGas(world, pos, 2).getClass());
        assertEquals(EntityOrangeFX.class, NTMEffects.createGas(world, pos, 3).getClass());
        assertEquals(EntityOrangeFX.class, NTMEffects.createGas(world, pos, -1).getClass());
    }

    @Test
    public void gasPreservesCountPositionAndSpreadSpeed() throws Exception {
        World world = mockWorld(false);
        JSONBullet bullet = bullet(120, 3);
        bullet.constantValues = new HashMap<>();
        bullet.constantValues.put("gasType", 2D);
        bullet.constantValues.put("gasSpreadSpeed", 0D);
        Point3D pos = new Point3D(1.5, 2.5, -3.5);
        NTMEffects.gas(world, pos, bullet);
        ArgumentCaptor<Entity> entities = ArgumentCaptor.forClass(Entity.class);
        verify(world, times(3)).spawnEntityInWorld(entities.capture());
        for (Entity gas : entities.getAllValues()) {
            assertEquals(EntityPinkCloudFX.class, gas.getClass());
            assertEquals(pos.x, gas.posX, 0);
            assertEquals(pos.y, gas.posY, 0);
            assertEquals(pos.z, gas.posZ, 0);
            assertEquals(0, gas.motionX, 0);
            assertEquals(0, gas.motionY, 0);
            assertEquals(0, gas.motionZ, 0);
        }
    }

    @Test
    public void gasDefaultSpreadUsesGaussianVelocityAndDiameterFallback() throws Exception {
        World world = mockWorld(false);
        Random expected = new Random(42);
        NTMEffects.gas(world, new Point3D(), bullet(20, 0));
        ArgumentCaptor<Entity> entities = ArgumentCaptor.forClass(Entity.class);
        verify(world, times(2)).spawnEntityInWorld(entities.capture());
        for (Entity gas : entities.getAllValues()) {
            assertTrue(gas instanceof EntityChlorineFX);
            assertEquals(expected.nextGaussian() * 1.25, gas.motionY, 0);
            assertEquals(expected.nextGaussian() * 1.25, gas.motionX, 0);
            assertEquals(expected.nextGaussian() * 1.25, gas.motionZ, 0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void forgeEntrypointRegistersOriginalIdsAndDoesNothingOnClient() throws Exception {
        Field registryField = EntityBullet.class.getDeclaredField("CUSTOM_HIT_FUNCTIONS");
        registryField.setAccessible(true);
        Map<String, EntityBullet.CustomHitFunction> registry =
            (Map<String, EntityBullet.CustomHitFunction>) registryField.get(null);
        Map<String, EntityBullet.CustomHitFunction> previous = new HashMap<>(registry);
        try {
            new NTMVehicles().preInit(null);
            for (String name : Arrays.asList("ntm_vehicles:nuke", "ntm_vehicles:gas", "ntm_vehicles:napalm")) {
                assertTrue(registry.containsKey(name));
                World world = mockWorld(true);
                registry.get(name).execute(wrap(world), new Point3D(), null, EntityBullet.HitType.BURST, null);
                verify(world, never()).spawnEntityInWorld(any(Entity.class));
            }
        } finally {
            registry.clear();
            registry.putAll(previous);
        }
    }

    @Test
    public void forgeMetadataRequiresIvlAndNtmNotMtsOrMixinBooter() {
        Mod mod = NTMVehicles.class.getAnnotation(Mod.class);
        assertEquals("ntm_vehicles", mod.modid());
        assertEquals("required-after:immersivevehicleslegacy@[0.1.0-ntmv2];required-after:hbm@[1.0.27,)",
            mod.dependencies());
        assertEquals("[1.7.10]", mod.acceptedMinecraftVersions());
    }

    @Test
    public void hbmVersionConstraintAcceptsTestedRuntimeAndRejectsUntestedBuilds() {
        String dependency = NTMVehicles.class.getAnnotation(Mod.class).dependencies().split(";")[1];
        String versionReference = dependency.substring("required-after:".length());
        String testedVersion = MainRegistry.class.getAnnotation(Mod.class).version();
        assertEquals("1.0.27 BETA (5808)", testedVersion);
        assertTrue(VersionParser.parseVersionReference(versionReference)
            .containsVersion(new DefaultArtifactVersion("hbm", testedVersion)));
        assertFalse(VersionParser.parseVersionReference(versionReference)
            .containsVersion(new DefaultArtifactVersion("hbm", "1.0.26")));
        Loader loader = mock(Loader.class);
        ModContainer hbm = mock(ModContainer.class);
        Map<String, ModContainer> mods = new HashMap<>();
        mods.put("hbm", hbm);
        when(loader.getIndexedModList()).thenReturn(mods);
        try (MockedStatic<Loader> loaders = mockStatic(Loader.class)) {
            loaders.when(Loader::instance).thenReturn(loader);
            when(hbm.getVersion()).thenReturn(testedVersion);
            new NTMVehicles().init(null);
            for (String unsupported : Arrays.asList("1.0.27 BETA (5807)", "1.0.27 BETA (5809)", "1.0.27_X5808")) {
                when(hbm.getVersion()).thenReturn(unsupported);
                LoaderException error = assertThrows(LoaderException.class, () -> new NTMVehicles().init(null));
                assertTrue(error.getMessage().contains(unsupported));
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void registeredGasCallbackUsesTheActualIvlBulletDefinitionOnServer() throws Exception {
        Field registryField = EntityBullet.class.getDeclaredField("CUSTOM_HIT_FUNCTIONS");
        registryField.setAccessible(true);
        Map<String, EntityBullet.CustomHitFunction> registry =
            (Map<String, EntityBullet.CustomHitFunction>) registryField.get(null);
        Map<String, EntityBullet.CustomHitFunction> previous = new HashMap<>(registry);
        try {
            new NTMVehicles().preInit(null);
            ItemBullet item = (ItemBullet) unsafe().allocateInstance(ItemBullet.class);
            Field definition = AItemPack.class.getDeclaredField("definition");
            definition.setAccessible(true);
            definition.set(item, bullet(20, 0));
            World world = mockWorld(false);
            registry.get("ntm_vehicles:gas").execute(wrap(world), new Point3D(), null, EntityBullet.HitType.ENTITY, item);
            verify(world, times(2)).spawnEntityInWorld(any(EntityChlorineFX.class));
        } finally {
            registry.clear();
            registry.putAll(previous);
        }
    }

    @Test
    public void nukePassesTruncatedBlastStrengthToBothNtmFactories() throws Exception {
        World world = mockWorld(false);
        Point3D pos = new Point3D(1.5, 2.5, -3.5);
        EntityNukeExplosionMK5 explosion = mock(EntityNukeExplosionMK5.class);
        try (MockedStatic<EntityNukeExplosionMK5> nukes = mockStatic(EntityNukeExplosionMK5.class);
             MockedStatic<EntityNukeTorex> clouds = mockStatic(EntityNukeTorex.class)) {
            nukes.when(() -> EntityNukeExplosionMK5.statFac(world, 8, pos.x, pos.y, pos.z)).thenReturn(explosion);
            NTMEffects.nuke(world, pos, bullet(120, 8.9F));
            nukes.verify(() -> EntityNukeExplosionMK5.statFac(world, 8, pos.x, pos.y, pos.z));
            clouds.verify(() -> EntityNukeTorex.statFacStandard(world, pos.x, pos.y, pos.z, 8));
            verify(world).spawnEntityInWorld(explosion);
        }
    }

    @Test
    public void nukeKeepsDiameterFallback() throws Exception {
        World world = mockWorld(false);
        try (MockedStatic<EntityNukeExplosionMK5> nukes = mockStatic(EntityNukeExplosionMK5.class);
             MockedStatic<EntityNukeTorex> clouds = mockStatic(EntityNukeTorex.class)) {
            NTMEffects.nuke(world, new Point3D(), bullet(125, 0));
            nukes.verify(() -> EntityNukeExplosionMK5.statFac(world, 12, 0, 0, 0));
            clouds.verify(() -> EntityNukeTorex.statFacStandard(world, 0, 0, 0, 12));
        }
    }

    @Test
    public void napalmKeepsExplosionIgnitionBoundsAndFiveBursts() throws Exception {
        World world = mockWorld(false);
        Point3D pos = new Point3D(-1.5, 2.5, -3.5);
        Random expected = new Random(42);
        try (MockedStatic<ExplosionLarge> explosions = mockStatic(ExplosionLarge.class);
             MockedStatic<ExplosionChaos> fires = mockStatic(ExplosionChaos.class)) {
            NTMEffects.napalm(world, pos);
            explosions.verify(() -> ExplosionLarge.explode(world, pos.x, pos.y, pos.z, 2.5F, false, false, false));
            fires.verify(() -> ExplosionChaos.igniteAllBlocks(world, -2, 2, -4, 9));
            fires.verify(() -> ExplosionChaos.igniteFlammableBlocks(world, -2, 2, -4, 14));
            for (int i = 0; i < 5; ++i) {
                int count = expected.nextInt(10) + 15;
                double strength = expected.nextFloat() * 2 + 2;
                explosions.verify(() -> ExplosionLarge.spawnBurst(world, pos.x, pos.y + 1, pos.z, count, strength));
            }
            explosions.verifyNoMoreInteractions();
            fires.verifyNoMoreInteractions();
        }
    }

    private static JSONBullet bullet(float diameter, float strength) {
        JSONBullet definition = new JSONBullet();
        definition.bullet = new JSONBullet.Bullet();
        definition.bullet.diameter = diameter;
        definition.bullet.blastStrength = strength;
        return definition;
    }

    private static World mockWorld(boolean client) throws Exception {
        World world = mock(World.class);
        Field random = World.class.getDeclaredField("rand");
        random.setAccessible(true);
        random.set(world, new Random(42));
        Field remote = World.class.getDeclaredField("isRemote");
        remote.setAccessible(true);
        remote.setBoolean(world, client);
        Field provider = World.class.getDeclaredField("provider");
        provider.setAccessible(true);
        provider.set(world, mock(WorldProvider.class));
        return world;
    }

    private static WrapperWorld wrap(World world) throws Exception {
        WrapperWorld wrapper = (WrapperWorld) unsafe().allocateInstance(WrapperWorld.class);
        Field internal = WrapperWorld.class.getDeclaredField("world");
        internal.setAccessible(true);
        internal.set(wrapper, world);
        return wrapper;
    }

    private static Unsafe unsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }
}
