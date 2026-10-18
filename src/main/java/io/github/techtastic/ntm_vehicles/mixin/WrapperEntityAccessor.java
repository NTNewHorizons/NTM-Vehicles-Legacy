package io.github.techtastic.ntm_vehicles.mixin;

import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.thomass47.immersivevehicleslegacy.mcinterface1710.WrapperEntity;

@Mixin(value = WrapperEntity.class, remap = false)
public interface WrapperEntityAccessor {

    @Accessor("entity")
    Entity ntmVehicles$getEntity();
}
