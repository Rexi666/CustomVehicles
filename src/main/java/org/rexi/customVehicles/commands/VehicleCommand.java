package org.rexi.customVehicles.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.rexi.customVehicles.CustomVehicles;
import org.rexi.customVehicles.vehicle.Car;
import org.rexi.customVehicles.vehicle.VehicleType;

public class VehicleCommand implements CommandExecutor {

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    "Este comando sólo puede usarlo un jugador."
            );
            return true;
        }

        if (args.length != 2
                || !args[0].equalsIgnoreCase("spawn")) {

            player.sendMessage(
                    "§eUso: /vehicle spawn "
                            + "<car|compact|sedan|van>"
            );

            return true;
        }

        VehicleType type;

        switch (args[1].toLowerCase()) {
            case "car", "sedan" ->
                    type = VehicleType.SEDAN;

            case "compact" ->
                    type = VehicleType.COMPACT;

            case "van" ->
                    type = VehicleType.VAN;

            default -> {
                player.sendMessage(
                        "§cTipo desconocido. "
                                + "Usa compact, sedan o van."
                );

                return true;
            }
        }

        Car car = new Car(
                player.getLocation(),
                type
        );

        CustomVehicles.getInstance()
                .getVehicleManager()
                .addVehicle(car);

        player.sendMessage(
                "§aVehículo generado: "
                        + type.name().toLowerCase()
                        + " con "
                        + type.getSeats()
                        + " plazas."
        );

        return true;
    }
}