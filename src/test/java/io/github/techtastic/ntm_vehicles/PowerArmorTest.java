package io.github.techtastic.ntm_vehicles;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemStack;

import org.junit.Test;

import com.hbm.items.armor.ArmorFSB;
import com.hbm.items.armor.ArmorFSBFueled;
import com.hbm.items.armor.ArmorFSBPowered;

public class PowerArmorTest {

    @Test
    public void emptySeatsNonPlayersAndUnarmouredPlayersDoNotCount() {
        assertFalse(PowerArmor.isWearing(null));
        assertFalse(PowerArmor.isWearing(mock(Entity.class)));
        assertFalse(PowerArmor.isWearing(player()));
    }

    @Test
    public void batteryAndFuelSuitsCountWithoutChargeOrFuel() {
        for (Class<? extends ArmorFSB> type : Arrays.asList(ArmorFSBPowered.class, ArmorFSBFueled.class)) {
            EntityPlayer player = suit(type);
            assertTrue(PowerArmor.isWearing(player));
            for (ItemStack stack : player.inventory.armorInventory) {
                verify((ArmorFSB) stack.getItem(), never()).isArmorEnabled(stack);
            }
        }
    }

    @Test
    public void ordinaryAndUnpoweredFullSetsDoNotCount() {
        EntityPlayer player = suit(ArmorFSB.class);
        assertFalse(PowerArmor.isWearing(player));
        player.inventory.armorInventory[2] = stack(mock(Item.class));
        assertFalse(PowerArmor.isWearing(player));
    }

    @Test
    public void everyRequiredSlotMustMatchTheChestMaterial() {
        for (int slot = 0; slot < 4; slot++) {
            EntityPlayer player = suit(ArmorFSBPowered.class);
            player.inventory.armorInventory[slot] = null;
            assertFalse(PowerArmor.isWearing(player));

            player = suit(ArmorFSBPowered.class);
            player.inventory.armorInventory[slot] = stack(mock(Item.class));
            assertFalse(PowerArmor.isWearing(player));

            player = suit(ArmorFSBPowered.class);
            when(((ArmorFSB) player.inventory.armorInventory[slot].getItem()).getArmorMaterial())
                .thenReturn(ArmorMaterial.DIAMOND);
            assertFalse(PowerArmor.isWearing(player));
        }
    }

    @Test
    public void optionalHelmetAndEquipmentChangesFollowNtmRules() {
        EntityPlayer player = suit(ArmorFSBFueled.class);
        ArmorFSB chestplate = (ArmorFSB) player.inventory.armorInventory[2].getItem();
        chestplate.noHelmet = true;
        player.inventory.armorInventory[3] = null;
        assertTrue(PowerArmor.isWearing(player));
        player.inventory.armorInventory[3] = stack(mock(Item.class));
        assertTrue(PowerArmor.isWearing(player));
        chestplate.noHelmet = false;
        assertFalse(PowerArmor.isWearing(player));
        player.inventory.armorInventory[3] = stack(armour(ArmorFSB.class));
        assertTrue(PowerArmor.isWearing(player));
    }

    private static EntityPlayer player() {
        EntityPlayer player = mock(EntityPlayer.class);
        player.inventory = mock(InventoryPlayer.class);
        player.inventory.armorInventory = new ItemStack[4];
        return player;
    }

    private static EntityPlayer suit(Class<? extends ArmorFSB> chestType) {
        EntityPlayer player = player();
        for (int slot = 0; slot < 4; slot++) {
            player.inventory.armorInventory[slot] = stack(armour(slot == 2 ? chestType : ArmorFSB.class));
        }
        return player;
    }

    private static ArmorFSB armour(Class<? extends ArmorFSB> type) {
        ArmorFSB armour = mock(type);
        when(armour.getArmorMaterial()).thenReturn(ArmorMaterial.IRON);
        return armour;
    }

    private static ItemStack stack(Item item) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(item);
        return stack;
    }
}
