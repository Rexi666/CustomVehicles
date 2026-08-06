package org.rexi.customVehicles.loader;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;
import org.rexi.customVehicles.CustomVehicles;
import org.rexi.customVehicles.definition.*;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VehicleDefinitionLoader {

    private final CustomVehicles plugin;

    private final Map<String, VehicleDefinition> definitions =
            new HashMap<>();

    public VehicleDefinitionLoader(CustomVehicles plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        definitions.clear();

        for (VehicleCategory category
                : VehicleCategory.values()) {

            loadCategory(category);
        }

        plugin.getLogger().info(
                "Vehículos cargados: "
                        + definitions.size()
        );
    }

    private void loadCategory(VehicleCategory category) {
        File categoryFolder = new File(
                plugin.getDataFolder(),
                category.getFolderName()
        );

        if (!categoryFolder.exists()
                && !categoryFolder.mkdirs()) {

            plugin.getLogger().warning(
                    "No se pudo crear "
                            + categoryFolder.getPath()
            );

            return;
        }

        File[] vehicleFolders =
                categoryFolder.listFiles(File::isDirectory);

        if (vehicleFolders == null) {
            return;
        }

        for (File vehicleFolder : vehicleFolders) {
            try {
                VehicleDefinition definition =
                        loadVehicle(
                                category,
                                vehicleFolder
                        );

                String normalizedId =
                        normalizeId(definition.id());

                if (definitions.containsKey(normalizedId)) {
                    plugin.getLogger().warning(
                            "ID duplicado: "
                                    + definition.id()
                    );

                    continue;
                }

                definitions.put(
                        normalizedId,
                        definition
                );

                plugin.getLogger().info(
                        "Cargado: "
                                + category.name()
                                + "/"
                                + definition.id()
                );

            } catch (VehicleLoadException exception) {
                plugin.getLogger().warning(
                        "No se pudo cargar "
                                + vehicleFolder.getPath()
                                + ": "
                                + exception.getMessage()
                );
            }
        }
    }

    private VehicleDefinition loadVehicle(
            VehicleCategory category,
            File directory
    ) throws VehicleLoadException {

        File configFile =
                new File(directory, "config.yml");

        if (!configFile.isFile()) {
            throw new VehicleLoadException(
                    "Falta config.yml"
            );
        }

        YamlConfiguration config =
                YamlConfiguration.loadConfiguration(
                        configFile
                );

        if (!config.getBoolean("enabled", true)) {
            throw new VehicleLoadException(
                    "El vehículo está desactivado"
            );
        }

        String id = requireString(config, "id");

        String displayName = config.getString(
                "display-name",
                id
        );

        VehiclePhysics physics =
                loadPhysics(config);

        VehicleDimensions dimensions =
                loadDimensions(config);

        VehicleModel model =
                loadModel(config, directory.toPath());

        SeatDefinition driver =
                loadDriverSeat(config);

        List<SeatDefinition> passengers =
                loadPassengerSeats(config);

        Map<String, Path> textures =
                loadTextures(config, directory.toPath());

        return new VehicleDefinition(
                id,
                displayName,
                category,
                directory.toPath(),
                physics,
                dimensions,
                model,
                driver,
                List.copyOf(passengers),
                Map.copyOf(textures)
        );
    }

    private VehiclePhysics loadPhysics(
            YamlConfiguration config
    ) throws VehicleLoadException {

        double maximumForwardSpeed =
                positive(
                        config,
                        "physics.max-forward-speed"
                );

        double maximumReverseSpeed =
                positive(
                        config,
                        "physics.max-reverse-speed"
                );

        double acceleration =
                positive(
                        config,
                        "physics.acceleration"
                );

        double reverseAcceleration =
                positive(
                        config,
                        "physics.reverse-acceleration"
                );

        double friction = config.getDouble(
                "physics.friction",
                0.98
        );

        if (friction <= 0 || friction > 1) {
            throw new VehicleLoadException(
                    "physics.friction debe estar "
                            + "entre 0 y 1"
            );
        }

        float handling = (float) positive(
                config,
                "physics.handling"
        );

        double minimumTurnSpeed =
                nonNegative(
                        config,
                        "physics.minimum-turn-speed"
                );

        return new VehiclePhysics(
                maximumForwardSpeed,
                maximumReverseSpeed,
                acceleration,
                reverseAcceleration,
                friction,
                handling,
                minimumTurnSpeed
        );
    }

    private VehicleDimensions loadDimensions(
            YamlConfiguration config
    ) throws VehicleLoadException {

        return new VehicleDimensions(
                positive(config, "dimensions.width"),
                positive(config, "dimensions.height"),
                positive(config, "dimensions.length")
        );
    }

    private VehicleModel loadModel(
            YamlConfiguration config,
            Path vehicleDirectory
    ) throws VehicleLoadException {

        String fileName =
                requireString(config, "model.file");

        Path modelFile =
                vehicleDirectory.resolve(fileName);

        if (!modelFile.toFile().isFile()) {
            throw new VehicleLoadException(
                    "No existe el modelo: "
                            + fileName
            );
        }

        float scale = (float) positive(
                config,
                "model.scale"
        );

        Vector offset = readVector(
                config,
                "model.offset"
        );

        float yawOffset = (float) config.getDouble(
                "model.rotation.yaw-offset",
                0
        );

        float pitchOffset = (float) config.getDouble(
                "model.rotation.pitch-offset",
                0
        );

        float rollOffset = (float) config.getDouble(
                "model.rotation.roll-offset",
                0
        );

        return new VehicleModel(
                modelFile,
                scale,
                offset,
                yawOffset,
                pitchOffset,
                rollOffset
        );
    }

    private SeatDefinition loadDriverSeat(
            YamlConfiguration config
    ) throws VehicleLoadException {

        return new SeatDefinition(
                "driver",
                SeatRole.DRIVER,
                readVector(
                        config,
                        "seats.driver.offset"
                )
        );
    }

    private List<SeatDefinition> loadPassengerSeats(
            YamlConfiguration config
    ) throws VehicleLoadException {

        List<Map<?, ?>> entries =
                config.getMapList("seats.passengers");

        List<SeatDefinition> result =
                new ArrayList<>();

        for (int index = 0;
             index < entries.size();
             index++) {

            Map<?, ?> entry = entries.get(index);

            Object idValue = entry.get("id");
            Object offsetValue = entry.get("offset");

            if (!(idValue instanceof String id)) {
                throw new VehicleLoadException(
                        "Asiento " + index
                                + " sin id válido"
                );
            }

            if (!(offsetValue instanceof Map<?, ?>
                    offsetMap)) {

                throw new VehicleLoadException(
                        "Asiento " + id
                                + " sin offset válido"
                );
            }

            Vector offset = vectorFromMap(
                    offsetMap,
                    "asiento " + id
            );

            result.add(
                    new SeatDefinition(
                            id,
                            SeatRole.PASSENGER,
                            offset
                    )
            );
        }

        return result;
    }

    private Map<String, Path> loadTextures(
            YamlConfiguration config,
            Path vehicleDirectory
    ) throws VehicleLoadException {

        ConfigurationSection section =
                config.getConfigurationSection(
                        "model.textures"
                );

        if (section == null) {
            throw new VehicleLoadException(
                    "Falta model.textures"
            );
        }

        Map<String, Path> result =
                new HashMap<>();

        for (String key : section.getKeys(false)) {
            String relativePath =
                    section.getString(key);

            if (relativePath == null
                    || relativePath.isBlank()) {

                throw new VehicleLoadException(
                        "Textura vacía: " + key
                );
            }

            Path textureFile =
                    vehicleDirectory.resolve(relativePath);

            if (!textureFile.toFile().isFile()) {
                throw new VehicleLoadException(
                        "No existe la textura "
                                + relativePath
                );
            }

            result.put(key, textureFile);
        }

        return result;
    }

    private Vector readVector(
            YamlConfiguration config,
            String path
    ) throws VehicleLoadException {

        ConfigurationSection section =
                config.getConfigurationSection(path);

        if (section == null) {
            throw new VehicleLoadException(
                    "Falta " + path
            );
        }

        return new Vector(
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z")
        );
    }

    private Vector vectorFromMap(
            Map<?, ?> map,
            String context
    ) throws VehicleLoadException {

        return new Vector(
                number(map.get("x"), context + ".x"),
                number(map.get("y"), context + ".y"),
                number(map.get("z"), context + ".z")
        );
    }

    private double number(
            Object value,
            String path
    ) throws VehicleLoadException {

        if (!(value instanceof Number number)) {
            throw new VehicleLoadException(
                    path + " debe ser un número"
            );
        }

        return number.doubleValue();
    }

    private String requireString(
            YamlConfiguration config,
            String path
    ) throws VehicleLoadException {

        String value = config.getString(path);

        if (value == null || value.isBlank()) {
            throw new VehicleLoadException(
                    "Falta " + path
            );
        }

        return value;
    }

    private double positive(
            YamlConfiguration config,
            String path
    ) throws VehicleLoadException {

        double value = config.getDouble(
                path,
                Double.NaN
        );

        if (!Double.isFinite(value) || value <= 0) {
            throw new VehicleLoadException(
                    path + " debe ser mayor que 0"
            );
        }

        return value;
    }

    private double nonNegative(
            YamlConfiguration config,
            String path
    ) throws VehicleLoadException {

        double value = config.getDouble(
                path,
                Double.NaN
        );

        if (!Double.isFinite(value) || value < 0) {
            throw new VehicleLoadException(
                    path
                            + " no puede ser negativo"
            );
        }

        return value;
    }

    private String normalizeId(String id) {
        return id.toLowerCase(Locale.ROOT);
    }

    public VehicleDefinition getDefinition(String id) {
        return definitions.get(normalizeId(id));
    }

    public Map<String, VehicleDefinition> getDefinitions() {
        return Map.copyOf(definitions);
    }
}