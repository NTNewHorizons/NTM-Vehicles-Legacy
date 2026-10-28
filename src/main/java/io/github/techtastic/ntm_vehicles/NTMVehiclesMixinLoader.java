package io.github.techtastic.ntm_vehicles;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

@LateMixin
public class NTMVehiclesMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.ntm_vehicles.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        return Arrays.asList("WrapperEntityAccessor", "PartSeatMixin", "EntityBulletMixin");
    }
}
