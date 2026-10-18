package io.github.techtastic.ntm_vehicles;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hbm.items.armor.ArmorFSB;
import com.hbm.items.armor.ArmorFSBFueled;
import com.hbm.items.armor.ArmorFSBPowered;

public final class PowerArmor {

    private PowerArmor() {}

    public static boolean isWearing(Entity entity) {
        if (!(entity instanceof EntityPlayer)) return false;

        EntityPlayer player = (EntityPlayer) entity;
        ItemStack chestplate = player.inventory.armorInventory[2];
        if (chestplate == null) return false;

        Item item = chestplate.getItem();
        return (item instanceof ArmorFSBPowered || item instanceof ArmorFSBFueled)
            && ArmorFSB.hasFSBArmorIgnoreCharge(player);
    }
}
