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
                    "§cYa estás montado."
            );
            return;
        }

        Car car = CustomVehicles
                .getInstance()
                .getVehicleManager()
                .getCarBySeat(seat);

        if (car == null) {
            player.sendMessage(
                    "§cNo se encontró el vehículo."
            );
            return;
        }

        if (!seat.getPassengers().isEmpty()) {
            player.sendMessage(
                    "§cEse asiento está ocupado."
            );
            return;
        }

        boolean mounted =
                seat.addPassenger(player);

        if (!mounted) {
            player.sendMessage(
                    "§cNo has podido subir."
            );
            return;
        }

        if (car.isDriverSeat(seat)) {
            player.sendMessage(
                    "§aHas subido como conductor."
            );
        } else {
            player.sendMessage(
                    "§aHas subido como acompañante."
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

        player.sendMessage("§eTe has bajado.");
    }
}