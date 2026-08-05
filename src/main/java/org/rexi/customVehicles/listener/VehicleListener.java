package org.rexi.customVehicles.listener;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.rexi.customVehicles.CustomVehicles;

public class VehicleListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {

        if (!(event.getRightClicked() instanceof ArmorStand stand))
            return;

        NamespacedKey key = new NamespacedKey(
                CustomVehicles.getInstance(),
                "vehicle"
        );

        String type = stand.getPersistentDataContainer().get(
                key,
                PersistentDataType.STRING
        );

        if (type == null)
            return;

        if (!stand.getPassengers().isEmpty()) {
            return;
        }

        Player player = event.getPlayer();

        stand.addPassenger(player);
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {

        if (!(event.getEntity() instanceof Player))
            return;

        if (!(event.getDismounted() instanceof ArmorStand))
            return;

        Player player = (Player) event.getEntity();

        player.sendMessage("Te has bajado.");
    }
}