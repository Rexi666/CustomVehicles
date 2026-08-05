package org.rexi.customVehicles;

import org.bukkit.plugin.java.JavaPlugin;
import org.rexi.customVehicles.commands.VehicleCommand;
import org.rexi.customVehicles.listener.VehicleInputListener;
import org.rexi.customVehicles.listener.VehicleListener;
import org.rexi.customVehicles.manager.VehicleManager;

public final class CustomVehicles extends JavaPlugin {

    private static CustomVehicles instance;

    private VehicleManager vehicleManager;
    private VehicleInputListener vehicleInputListener;

    @Override
    public void onEnable() {
        instance = this;

        vehicleManager = new VehicleManager();
        vehicleManager.start();

        if (getCommand("vehicle") != null) {
            getCommand("vehicle").setExecutor(new VehicleCommand());
        }

        getServer().getPluginManager().registerEvents(
                new VehicleListener(),
                this
        );

        vehicleInputListener = new VehicleInputListener();
        vehicleInputListener.register();

        getLogger().info("CustomVehicles enabled!");
    }

    @Override
    public void onDisable() {
        if (vehicleManager != null) {
            vehicleManager.removeAll();
        }

        getLogger().info("CustomVehicles disabled!");
    }

    public static CustomVehicles getInstance() {
        return instance;
    }

    public VehicleManager getVehicleManager() {
        return vehicleManager;
    }
}