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
            return true;
        }

        if (args.length == 2
                && args[0].equalsIgnoreCase("spawn")
                && args[1].equalsIgnoreCase("car")) {

            Car car = new Car(
                    player.getLocation(),
                    VehicleType.SEDAN
            );

            CustomVehicles.getInstance().getVehicleManager()
                    .addVehicle(car);

            player.sendMessage("§aCoche generado.");
        }

        return true;
    }
}