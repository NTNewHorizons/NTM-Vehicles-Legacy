package io.github.techtastic.ntm_vehicles;

import static org.junit.Assert.*;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import net.minecraft.entity.Entity;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import com.thomass47.immersivevehicleslegacy.mcinterface1710.WrapperEntity;

import io.github.techtastic.ntm_vehicles.mixin.WrapperEntityAccessor;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.entities.instances.PartSeat;

public class MixinLoadingTest {

    @Test
    public void modMixinsAreQueuedOnlyByTheLateLoader() throws Exception {
        NTMVehiclesMixinLoader loader = new NTMVehiclesMixinLoader();
        assertTrue(NTMVehiclesMixinLoader.class.isAnnotationPresent(LateMixin.class));
        assertEquals(Arrays.asList("WrapperEntityAccessor", "PartSeatMixin"), loader.getMixins(Collections.emptySet()));
        assertNotEquals("mixins.ntm_vehicles.json", loader.getMixinConfig());
        for (String resource : Arrays.asList("mixins.ntm_vehicles.json", loader.getMixinConfig())) {
            try (InputStreamReader reader = new InputStreamReader(
                getClass().getClassLoader()
                    .getResourceAsStream(resource),
                StandardCharsets.UTF_8)) {
                JsonObject config = new Gson().fromJson(reader, JsonObject.class);
                assertTrue(
                    config.get("required")
                        .getAsBoolean());
                assertEquals(
                    "io.github.techtastic.ntm_vehicles.mixin",
                    config.get("package")
                        .getAsString());
                assertEquals(
                    0,
                    config.getAsJsonArray("mixins")
                        .size());
            }
        }
    }

    @Test
    public void targetsMatchPinnedIvlSignatures() throws Exception {
        assertEquals(
            ComputedVariable.class,
            PartSeat.class.getMethod("createComputedVariable", String.class, boolean.class)
                .getReturnType());
        assertEquals(
            Entity.class,
            WrapperEntity.class.getDeclaredField("entity")
                .getType());
        assertEquals(
            Entity.class,
            WrapperEntityAccessor.class.getMethod("ntmVehicles$getEntity")
                .getReturnType());
    }
}
