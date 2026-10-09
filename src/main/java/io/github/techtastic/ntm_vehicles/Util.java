package io.github.techtastic.ntm_vehicles;

import java.lang.reflect.Field;

import net.minecraft.world.World;

import com.thomass47.immersivevehicleslegacy.mcinterface1710.WrapperWorld;

import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONVariableModifier;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;

public class Util {

    private static final Field WORLD_FIELD = findWorldField();

    private static Field findWorldField() {
        try {
            Field field = WrapperWorld.class.getDeclaredField("world");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot access the IVL world bridge", exception);
        }
    }

    public static World getWorld(AWrapperWorld world) {
        if (!(world instanceof WrapperWorld)) {
            throw new IllegalArgumentException("NTM: Vehicles requires an IVL world wrapper");
        }
        try {
            return (World) WORLD_FIELD.get(world);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot unwrap the IVL world", exception);
        }
    }

    public static JSONVariableModifier getVariableModifier(JSONBullet definition, String name) {
        if (definition.variableModifiers == null) return null;

        for (JSONVariableModifier var : definition.variableModifiers) {
            if (var.variable.equals(name)) return var;
        }
        return null;
    }

    public static double getConstantValue(JSONBullet definition, String name, double defaultValue) {
        if (definition.constantValues == null) return defaultValue;
        return definition.constantValues.getOrDefault(name, defaultValue);
    }

    public static double getBlastSize(JSONBullet bullet) {
        return bullet.bullet.blastStrength == 0f ? bullet.bullet.diameter / 10f : bullet.bullet.blastStrength;
    }
}
