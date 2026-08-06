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

    public VehicleCommand(
            CustomVehicles plugin
    ) {
        this.plugin = plugin;
    }

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
                    "§eUso: /vehicle spawn <id>"
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
                    "§cNo existe el vehículo '"
                            + args[1]
                            + "'."
            );

            player.sendMessage(
                    "§7Usa §f/vehicle list §7para ver "
                            + "los vehículos disponibles."
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
                    "No se pudo generar el vehículo "
                            + definition.id()
                            + ": "
                            + exception.getMessage()
            );

            exception.printStackTrace();

            player.sendMessage(
                    "§cNo se pudo generar el vehículo. "
                            + "Consulta la consola."
            );

            return true;
        }

        plugin.getVehicleManager()
                .addVehicle(car);

        player.sendMessage(
                "§aVehículo generado: §f"
                        + definition.displayName()
        );

        player.sendMessage(
                "§7ID: "
                        + definition.id()
                        + " | Plazas: "
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
                    "§cNo tienes permiso para recargar "
                            + "los vehículos."
            );

            return true;
        }

        plugin.reloadConfig();

        plugin.getDefinitionLoader()
                .loadAll();

        boolean resourcePackGenerated =
                plugin.rebuildResourcePack();

        player.sendMessage(
                "§aDefiniciones de vehículos recargadas."
        );

        if (resourcePackGenerated) {

            player.sendMessage(
                    "§aResource pack regenerado correctamente."
            );

        } else {

            player.sendMessage(
                    "§cNo se pudo regenerar el resource pack. "
                            + "Consulta la consola."
            );
        }

        player.sendMessage(
                "§7Vehículos disponibles: "
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
                    "§eNo hay vehículos cargados."
            );

            return true;
        }

        player.sendMessage(
                "§6Vehículos disponibles:"
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
                                        + " plazas)"
                        )
                );

        return true;
    }

    private void sendUsage(Player player) {

        player.sendMessage(
                "§6Comandos de CustomVehicles:"
        );

        player.sendMessage(
                "§e/vehicle spawn <id>"
                        + " §7- Genera un vehículo"
        );

        player.sendMessage(
                "§e/vehicle list"
                        + " §7- Lista los vehículos disponibles"
        );

        if (player.hasPermission(
                "customvehicles.admin.reload"
        )) {
            player.sendMessage(
                    "§e/vehicle reload"
                            + " §7- Recarga las definiciones"
            );
        }
    }
}