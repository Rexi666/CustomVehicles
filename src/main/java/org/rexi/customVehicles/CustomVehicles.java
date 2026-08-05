package org.rexi.customVehicles;

import org.bukkit.plugin.java.JavaPlugin;
import org.rexi.customVehicles.commands.VehicleCommand;
import org.rexi.customVehicles.listener.VehicleInputListener;
import org.rexi.customVehicles.listener.VehicleListener;
import org.rexi.customVehicles.manager.VehicleManager;

public final class CustomVehicles extends JavaPlugin {

    private static CustomVehicles instance;
    private VehicleManager vehicleManager;

    @Override
    public void onEnable() {
        instance = this;

        vehicleManager = new VehicleManager();
        vehicleManager.start();

        getCommand("vehicle").setExecutor(new VehicleCommand());

        getServer().getPluginManager().registerEvents(
                new VehicleListener(),
                this
        );
        new VehicleInputListener().register();

        getLogger().info("CustomVehicles enabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static CustomVehicles getInstance() {
        return instance;
    }

    public VehicleManager getVehicleManager() {
        return vehicleManager;
    }
}
