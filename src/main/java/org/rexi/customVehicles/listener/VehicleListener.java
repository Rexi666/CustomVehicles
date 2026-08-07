package org.rexi.customVehicles.listener;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.rexi.customVehicles.CustomVehicles;
import org.rexi.customVehicles.vehicle.Car;

public class VehicleListener implements Listener {

    @EventHandler
    public void onInteract(
            PlayerInteractAtEntityEvent event
    ) {
        if (!(event.getRightClicked()
                instanceof Slime seat)) {
            return;
        }

        NamespacedKey seatKey = new NamespacedKey(
                CustomVehicles.getInstance(),
                "vehicle_seat"
        );

        String value =
                seat.getPersistentDataContainer().get(
                        seatKey,
                        PersistentDataType.STRING
                );

        if (!"seat".equals(value)) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();

        if (player.isInsideVehicle()) {
            player.sendMessage(
                    "§cYou are already inside the seat!."
            );
            return;
        }

        Car car = CustomVehicles
                .getInstance()
                .getVehicleManager()
                .getCarBySeat(seat);

        if (car == null) {
            player.sendMessage(
                    "§cNo available vehicle found."
            );
            return;
        }

        if (!seat.getPassengers().isEmpty()) {
            player.sendMessage(
                    "§cThat seat is occupied."
            );
            return;
        }

        boolean mounted =
                seat.addPassenger(player);

        if (!mounted) {
            player.sendMessage(
                    "§cYou could not mount the vehicle."
            );
            return;
        }

        if (car.isDriverSeat(seat)) {
            player.sendMessage(
                    "§aYou have mounted as the driver."
            );
        } else {
            player.sendMessage(
                    "§aYou have mounted as a passenger."
            );
        }
    }

    @EventHandler
    public void onDismount(
            EntityDismountEvent event
    ) {
        if (!(event.getEntity()
                instanceof Player player)) {
            return;
        }

        if (!(event.getDismounted()
                instanceof Slime seat)) {
            return;
        }

        Car car = CustomVehicles
                .getInstance()
                .getVehicleManager()
                .getCarBySeat(seat);

        if (car == null) {
            return;
        }

        if (car.isDriverSeat(seat)) {
            car.resetInput();
        }

        player.sendMessage("§eYou have dismounted.");
    }
}