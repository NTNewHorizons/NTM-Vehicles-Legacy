package io.github.techtastic.ntm_vehicles.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.techtastic.ntm_vehicles.PowerArmor;
import minecrafttransportsimulator.baseclasses.ComputedVariable;
import minecrafttransportsimulator.entities.instances.PartSeat;

@Mixin(value = PartSeat.class, remap = false)
public abstract class PartSeatMixin {

    @Inject(method = "createComputedVariable", at = @At("HEAD"), cancellable = true)
    private void ntmVehicles$createPowerArmorVariable(String variable, boolean createDefaultIfNotPresent,
        CallbackInfoReturnable<ComputedVariable> cir) {
        if (variable.equals("seat_rider_power_armor")) {
            PartSeat seat = (PartSeat) (Object) this;
            cir.setReturnValue(
                new ComputedVariable(
                    seat,
                    variable,
                    partialTicks -> seat.rider instanceof WrapperEntityAccessor
                        && PowerArmor.isWearing(((WrapperEntityAccessor) seat.rider).ntmVehicles$getEntity()) ? 1 : 0,
                    false));
        }
    }
}
