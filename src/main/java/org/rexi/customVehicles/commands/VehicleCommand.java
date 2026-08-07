package org.rexi.customVehicles.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.rexi.customVehicles.CustomVehicles;
import org.rexi.customVehicles.definition.VehicleDefinition;
import org.rexi.customVehicles.vehicle.Car;

import java.util.Locale;

public class VehicleCommand implements CommandExecutor {

    private final CustomVehicles plugin;

    public VehicleCommand(CustomVehicles plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");

            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("spawn")) {
            return handleSpawn(
                    player,
                    args
            );
        }

        if (args[0].equalsIgnoreCase("reload")) {
            return handleReload(player);
        }

        if (args[0].equalsIgnoreCase("list")) {
            return handleList(player);
        }

        sendUsage(player);
        return true;
    }

    private boolean handleSpawn(
            Player player,
            String[] args
    ) {
        if (args.length != 2) {

            player.sendMessage(
                    "§eUsage: /vehicle spawn <id>"
            );

            return true;
        }

        String vehicleId =
                args[1].toLowerCase(Locale.ROOT);

        VehicleDefinition definition =
                plugin.getDefinitionLoader()
                        .getDefinition(vehicleId);

        if (definition == null) {

            player.sendMessage(
                    "§cVehicle doesnt exist '"
                            + args[1]
                            + "'."
            );

            player.sendMessage(
                    "§7Use §f/vehicle list §7to see "
                            + "available vehicles."
            );

            return true;
        }

        Car car;

        try {
            car = new Car(
                    player.getLocation(),
                    definition
            );

        } catch (Exception exception) {

            plugin.getLogger().severe(
                    "Vehicle couldnt be generated: "
                            + definition.id()
                            + ": "
                            + exception.getMessage()
            );

            exception.printStackTrace();

            player.sendMessage(
                    "§cVehicle couldnt be generated. "
                            + "Check the console."
            );

            return true;
        }

        plugin.getVehicleManager()
                .addVehicle(car);

        player.sendMessage(
                "§aVehicle generated: §f"
                        + definition.displayName()
        );

        player.sendMessage(
                "§7ID: "
                        + definition.id()
                        + " | Seats: "
                        + definition.getTotalSeats()
        );

        return true;
    }

    private boolean handleReload(
            Player player
    ) {
        if (!player.hasPermission(
                "customvehicles.admin.reload"
        )) {

            player.sendMessage(
                    "§cYou dont have permission to reload "
                            + "the vehicles."
            );

            return true;
        }

        plugin.reloadConfig();

        plugin.getDefinitionLoader()
                .loadAll();

        boolean resourcePackGenerated =
                plugin.rebuildResourcePack();

        player.sendMessage(
                "§aVehicle definitions reloaded."
        );

        if (resourcePackGenerated) {

            player.sendMessage(
                    "§aResource pack regenerated successfully."
            );

        } else {

            player.sendMessage(
                    "§cCouldnt regenerate the resource pack. "
                            + "Check the console."
            );
        }

        player.sendMessage(
                "§7Available vehicles: "
                        + plugin.getDefinitionLoader()
                        .getDefinitions()
                        .size()
        );

        return true;
    }

    private boolean handleList(
            Player player
    ) {
        var definitions =
                plugin.getDefinitionLoader()
                        .getDefinitions();

        if (definitions.isEmpty()) {

            player.sendMessage(
                    "§eNo available vehicles loaded."
            );

            return true;
        }

        player.sendMessage(
                "§6Available vehicles:"
        );

        definitions.values()
                .stream()
                .sorted(
                        (first, second) ->
                                first.id()
                                        .compareToIgnoreCase(
                                                second.id()
                                        )
                )
                .forEach(definition ->
                        player.sendMessage(
                                "§e- §f"
                                        + definition.id()
                                        + " §7("
                                        + definition.displayName()
                                        + ", "
                                        + definition.getTotalSeats()
                                        + " seats)"
                        )
                );

        return true;
    }

    private void sendUsage(Player player) {

        player.sendMessage(
                "§6CustomVehicles commands:"
        );

        player.sendMessage(
                "§e/vehicle spawn <id>"
                        + " §7- Generates a vehicle"
        );

        player.sendMessage(
                "§e/vehicle list"
                        + " §7- Lists available vehicles"
        );

        if (player.hasPermission(
                "customvehicles.admin.reload"
        )) {
            player.sendMessage(
                    "§e/vehicle reload"
                            + " §7- Reloads vehicle definitions"
            );
        }
    }
}