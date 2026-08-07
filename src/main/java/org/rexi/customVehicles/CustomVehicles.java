package org.rexi.customVehicles;

import org.bukkit.plugin.java.JavaPlugin;
import org.rexi.customVehicles.commands.VehicleCommand;
import org.rexi.customVehicles.listener.VehicleInputListener;
import org.rexi.customVehicles.listener.VehicleListener;
import org.rexi.customVehicles.loader.VehicleDefinitionLoader;
import org.rexi.customVehicles.manager.VehicleManager;
import org.rexi.customVehicles.resourcepack.ResourcePackBuildException;
import org.rexi.customVehicles.resourcepack.ResourcePackBuildResult;
import org.rexi.customVehicles.resourcepack.ResourcePackBuilder;

import java.io.File;

public final class CustomVehicles extends JavaPlugin {

    private static CustomVehicles instance;

    private VehicleManager vehicleManager;
    private VehicleInputListener vehicleInputListener;
    private VehicleDefinitionLoader definitionLoader;
    private ResourcePackBuilder resourcePackBuilder;

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()
                && !getDataFolder().mkdirs()) {

            getLogger().severe(
                    "No se pudo crear la carpeta "
                            + getDataFolder().getPath()
            );

            getServer().getPluginManager()
                    .disablePlugin(this);

            return;
        }

        saveDefaultConfig();

        File sedanDirectory = new File(
                getDataFolder(),
                "Cars/Astral"
        );

        if (!sedanDirectory.exists()) {
            saveResource(
                    "Cars/Astral/config.yml",
                    false
            );

            saveResource(
                    "Cars/Astral/model.json",
                    false
            );

            saveResource(
                    "Cars/Astral/texture.png",
                    false
            );
        }

        definitionLoader = new VehicleDefinitionLoader(this);
        definitionLoader.loadAll();

        resourcePackBuilder =
                new ResourcePackBuilder(this);

        if (getConfig().getBoolean(
                "resource-pack.enabled",
                true
        )
                && getConfig().getBoolean(
                "resource-pack.rebuild-on-startup",
                true
        )) {

            rebuildResourcePack();
        }

        vehicleManager = new VehicleManager();
        vehicleManager.start();

        if (getCommand("vehicle") != null) {
            getCommand("vehicle").setExecutor(new VehicleCommand(this));
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

    public VehicleDefinitionLoader getDefinitionLoader() {
        return definitionLoader;
    }

    public boolean rebuildResourcePack() {

        if (resourcePackBuilder == null) {

            getLogger().warning(
                    "ResourcePackBuilder todavía "
                            + "no está inicializado."
            );

            return false;
        }

        try {

            ResourcePackBuildResult result =
                    resourcePackBuilder.build();

            getLogger().info(
                    "Resource pack generado correctamente."
            );

            getLogger().info(
                    "Archivo: "
                            + result.zipFile()
            );

            getLogger().info(
                    "SHA-1: "
                            + result.sha1()
            );

            getLogger().info(
                    "Modelos incluidos: "
                            + result.vehicleCount()
            );

            return true;

        } catch (ResourcePackBuildException exception) {

            getLogger().severe(
                    "No se pudo generar el resource pack: "
                            + exception.getMessage()
            );

            exception.printStackTrace();

            return false;
        }
    }

    public ResourcePackBuilder getResourcePackBuilder() {
        return resourcePackBuilder;
    }
}