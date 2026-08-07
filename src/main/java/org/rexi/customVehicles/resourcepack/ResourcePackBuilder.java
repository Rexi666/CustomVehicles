package org.rexi.customVehicles.resourcepack;

import com.google.gson.*;
import org.rexi.customVehicles.CustomVehicles;
import org.rexi.customVehicles.definition.VehicleDefinition;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ResourcePackBuilder {

    private static final String NAMESPACE = "customvehicles";

    private final CustomVehicles plugin;

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public ResourcePackBuilder(CustomVehicles plugin) {
        this.plugin = plugin;
    }

    public ResourcePackBuildResult build()
            throws ResourcePackBuildException {

        if (!plugin.getConfig().getBoolean(
                "resource-pack.enabled",
                true
        )) {
            throw new ResourcePackBuildException(
                    "Resourcepack generation is disabled."
            );
        }

        int packFormat = plugin.getConfig().getInt(
                "resource-pack.pack-format",
                0
        );

        if (packFormat <= 0) {
            throw new ResourcePackBuildException(
                    "resource-pack.pack-format must be a positive number."
            );
        }

        String outputDirectoryName =
                plugin.getConfig().getString(
                        "resource-pack.output-directory",
                        "generated"
                );

        String outputFileName =
                plugin.getConfig().getString(
                        "resource-pack.output-file",
                        "CustomVehiclesPack.zip"
                );

        validateSimpleFileName(
                outputDirectoryName,
                "output-directory"
        );

        validateSimpleFileName(
                outputFileName,
                "output-file"
        );

        if (!outputFileName
                .toLowerCase(Locale.ROOT)
                .endsWith(".zip")) {

            throw new ResourcePackBuildException(
                    "resource-pack.output-file must end with .zip."
            );
        }

        Path dataDirectory = plugin
                .getDataFolder()
                .toPath()
                .toAbsolutePath()
                .normalize();

        Path outputDirectory = dataDirectory
                .resolve(outputDirectoryName)
                .normalize();

        ensureInside(
                dataDirectory,
                outputDirectory,
                outputDirectoryName
        );

        Path temporaryDirectory =
                outputDirectory.resolve("pack-build");

        Path zipFile =
                outputDirectory.resolve(outputFileName);

        Path sha1File =
                outputDirectory.resolve(
                        removeZipExtension(outputFileName)
                                + ".sha1"
                );

        try {
            Files.createDirectories(outputDirectory);

            deleteRecursively(temporaryDirectory);
            Files.createDirectories(temporaryDirectory);

            createPackStructure(temporaryDirectory);

            writePackMetadata(
                    temporaryDirectory,
                    packFormat
            );

            Map<String, VehicleDefinition> definitions =
                    plugin.getDefinitionLoader()
                            .getDefinitions();

            if (definitions.isEmpty()) {
                plugin.getLogger().warning(
                        "An empty resource pack will be generated "
                                + "because no vehicles are loaded."
                );
            }

            Set<String> generatedIds =
                    new TreeSet<>();

            for (VehicleDefinition definition
                    : definitions.values()) {

                String resourceId =
                        normalizeResourceId(
                                definition.id()
                        );

                if (!generatedIds.add(resourceId)) {
                    throw new ResourcePackBuildException(
                            "Duplicate model ID: "
                                    + resourceId
                    );
                }

                addVehicle(
                        temporaryDirectory,
                        definition,
                        resourceId
                );
            }

            Files.deleteIfExists(zipFile);

            createZip(
                    temporaryDirectory,
                    zipFile
            );

            validateGeneratedZip(
                    zipFile,
                    definitions
            );

            String sha1 =
                    calculateSha1(zipFile);

            Files.writeString(
                    sha1File,
                    sha1 + System.lineSeparator(),
                    StandardCharsets.UTF_8
            );

            deleteRecursively(temporaryDirectory);

            return new ResourcePackBuildResult(
                    zipFile,
                    sha1,
                    definitions.size()
            );

        } catch (ResourcePackBuildException exception) {
            deleteQuietly(temporaryDirectory);
            throw exception;

        } catch (IOException exception) {
            deleteQuietly(temporaryDirectory);

            throw new ResourcePackBuildException(
                    "Could not generate the resource pack.",
                    exception
            );
        }
    }

    private void createPackStructure(Path root)
            throws IOException {

        Files.createDirectories(
                root.resolve(
                        "assets/"
                                + NAMESPACE
                                + "/items"
                )
        );

        Files.createDirectories(
                root.resolve(
                        "assets/"
                                + NAMESPACE
                                + "/models/item"
                )
        );

        Files.createDirectories(
                root.resolve(
                        "assets/"
                                + NAMESPACE
                                + "/textures/item"
                )
        );
    }

    private void writePackMetadata(
            Path root,
            int packFormat
    ) throws IOException {

        JsonObject pack = new JsonObject();

        pack.addProperty(
                "pack_format",
                packFormat
        );

        pack.addProperty(
                "description",
                "Generated by CustomVehicles"
        );

        JsonObject document =
                new JsonObject();

        document.add(
                "pack",
                pack
        );

        writeJson(
                root.resolve("pack.mcmeta"),
                document
        );
    }

    private void addVehicle(
            Path packRoot,
            VehicleDefinition definition,
            String resourceId
    ) throws IOException,
            ResourcePackBuildException {

        Path itemDeclaration = packRoot.resolve(
                "assets/"
                        + NAMESPACE
                        + "/items/"
                        + resourceId
                        + ".json"
        );

        Path destinationModel = packRoot.resolve(
                "assets/"
                        + NAMESPACE
                        + "/models/item/"
                        + resourceId
                        + ".json"
        );

        Path vehicleTextureDirectory =
                packRoot.resolve(
                        "assets/"
                                + NAMESPACE
                                + "/textures/item/"
                                + resourceId
                );

        Files.createDirectories(
                vehicleTextureDirectory
        );

        writeItemDeclaration(
                itemDeclaration,
                resourceId
        );

        copyAndRewriteModel(
                definition,
                destinationModel,
                resourceId
        );

        copyTextures(
                definition,
                vehicleTextureDirectory
        );
    }

    private void writeItemDeclaration(
            Path destination,
            String resourceId
    ) throws IOException {

        JsonObject model =
                new JsonObject();

        model.addProperty(
                "type",
                "minecraft:model"
        );

        model.addProperty(
                "model",
                NAMESPACE
                        + ":item/"
                        + resourceId
        );

        JsonObject document =
                new JsonObject();

        document.add(
                "model",
                model
        );

        writeJson(
                destination,
                document
        );
    }

    private void copyAndRewriteModel(
            VehicleDefinition definition,
            Path destination,
            String resourceId
    ) throws IOException,
            ResourcePackBuildException {

        Path sourceModel = definition
                .model()
                .modelFile()
                .toAbsolutePath()
                .normalize();

        ensureVehicleFile(
                definition,
                sourceModel
        );

        if (!Files.isRegularFile(sourceModel)) {
            throw new ResourcePackBuildException(
                    "Model not found for "
                            + definition.id()
                            + ": "
                            + sourceModel
            );
        }

        String jsonText = Files.readString(
                sourceModel,
                StandardCharsets.UTF_8
        );

        JsonElement parsed;

        try {
            parsed = JsonParser.parseString(
                    jsonText
            );

        } catch (RuntimeException exception) {
            throw new ResourcePackBuildException(
                    "Invalid JSON in the model of "
                            + definition.id(),
                    exception
            );
        }

        if (!parsed.isJsonObject()) {
            throw new ResourcePackBuildException(
                    "The model of "
                            + definition.id()
                            + " does not contain a JSON object."
            );
        }

        JsonObject model =
                parsed.getAsJsonObject();

        model.remove("format_version");

        /*
         * Corrige las rotaciones de ángulo cero y valida
         * las rotaciones efectivas antes de generar el pack.
         */
        sanitizeElementRotations(
                model,
                definition
        );

        JsonObject textures;

        if (model.has("textures")
                && model.get("textures").isJsonObject()) {

            textures = model.getAsJsonObject(
                    "textures"
            );

        } else {
            textures = new JsonObject();

            model.add(
                    "textures",
                    textures
            );
        }

        /*
         * Ejemplo:
         *
         * config.yml:
         *
         * model:
         *   textures:
         *     body: "texture.png"
         *
         * model.json:
         *
         * "textures": {
         *   "body": "..."
         * }
         *
         * Las caras del modelo deben utilizar:
         *
         * "texture": "#body"
         */
        for (String textureKey
                : definition.textures().keySet()) {

            String normalizedTextureKey =
                    normalizeResourceId(
                            textureKey
                    );

            textures.addProperty(
                    textureKey,
                    NAMESPACE
                            + ":item/"
                            + resourceId
                            + "/"
                            + normalizedTextureKey
            );
        }

        /*
         * Si no existe una textura de partículas,
         * usamos la primera textura definida.
         */
        if (!definition.textures().isEmpty()
                && !textures.has("particle")) {

            String firstTextureKey =
                    definition.textures()
                            .keySet()
                            .stream()
                            .sorted()
                            .findFirst()
                            .orElseThrow();

            textures.addProperty(
                    "particle",
                    NAMESPACE
                            + ":item/"
                            + resourceId
                            + "/"
                            + normalizeResourceId(
                            firstTextureKey
                    )
            );
        }

        writeJson(
                destination,
                model
        );
    }

    private void copyTextures(
            VehicleDefinition definition,
            Path destinationDirectory
    ) throws IOException,
            ResourcePackBuildException {

        for (Map.Entry<String, Path> entry
                : definition.textures().entrySet()) {

            String textureKey =
                    normalizeResourceId(
                            entry.getKey()
                    );

            Path source = entry
                    .getValue()
                    .toAbsolutePath()
                    .normalize();

            ensureVehicleFile(
                    definition,
                    source
            );

            if (!Files.isRegularFile(source)) {
                throw new ResourcePackBuildException(
                        "Texture not found: "
                                + source
                );
            }

            String sourceName = source
                    .getFileName()
                    .toString()
                    .toLowerCase(Locale.ROOT);

            if (!sourceName.endsWith(".png")) {
                throw new ResourcePackBuildException(
                        "Texture must be a PNG file: "
                                + source
                );
            }

            BufferedImage image;

            try {
                image = ImageIO.read(
                        source.toFile()
                );
            } catch (IOException exception) {
                throw new ResourcePackBuildException(
                        "Could not read the PNG texture: "
                                + source,
                        exception
                );
            }

            if (image == null) {
                throw new ResourcePackBuildException(
                        "The file is not a valid PNG: "
                                + source
                );
            }

            Path destination =
                    destinationDirectory.resolve(
                            textureKey + ".png"
                    );

            Files.copy(
                    source,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            plugin.getLogger().info(
                    "Texture validated for "
                            + definition.id()
                            + ": "
                            + source.getFileName()
                            + " ("
                            + image.getWidth()
                            + "x"
                            + image.getHeight()
                            + ")"
            );
        }
    }

    private void ensureVehicleFile(
            VehicleDefinition definition,
            Path file
    ) throws ResourcePackBuildException {

        Path vehicleDirectory = definition
                .directory()
                .toAbsolutePath()
                .normalize();

        if (!file.startsWith(vehicleDirectory)) {
            throw new ResourcePackBuildException(
                    "The file is outside the vehicle folder "
                            + definition.id()
                            + ": "
                            + file
            );
        }
    }

    private void createZip(
            Path sourceDirectory,
            Path zipFile
    ) throws IOException {

        List<Path> files;

        try (Stream<Path> pathStream =
                     Files.walk(sourceDirectory)) {

            files = pathStream
                    .filter(Files::isRegularFile)
                    .sorted(
                            Comparator.comparing(
                                    path -> sourceDirectory
                                            .relativize(path)
                                            .toString()
                            )
                    )
                    .toList();
        }

        try (
                OutputStream fileOutput =
                        Files.newOutputStream(zipFile);

                BufferedOutputStream bufferedOutput =
                        new BufferedOutputStream(
                                fileOutput
                        );

                ZipOutputStream zipOutput =
                        new ZipOutputStream(
                                bufferedOutput,
                                StandardCharsets.UTF_8
                        )
        ) {
            for (Path file : files) {

                String entryName = sourceDirectory
                        .relativize(file)
                        .toString()
                        .replace('\\', '/');

                ZipEntry entry =
                        new ZipEntry(entryName);

                /*
                 * Fecha fija para que el ZIP sea reproducible
                 * cuando los archivos no cambien.
                 */
                entry.setTime(0L);

                zipOutput.putNextEntry(entry);

                try (
                        InputStream input =
                                new BufferedInputStream(
                                        Files.newInputStream(
                                                file
                                        )
                                )
                ) {
                    input.transferTo(
                            zipOutput
                    );
                }

                zipOutput.closeEntry();
            }
        }
    }

    private String calculateSha1(
            Path file
    ) throws IOException,
            ResourcePackBuildException {

        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance(
                    "SHA-1"
            );

        } catch (NoSuchAlgorithmException exception) {
            throw new ResourcePackBuildException(
                    "SHA-1 is not available.",
                    exception
            );
        }

        try (
                InputStream inputStream =
                        new BufferedInputStream(
                                Files.newInputStream(file)
                        )
        ) {
            byte[] buffer =
                    new byte[8192];

            int read;

            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(
                        buffer,
                        0,
                        read
                );
            }
        }

        return HexFormat.of()
                .formatHex(
                        digest.digest()
                );
    }

    private void writeJson(
            Path file,
            JsonElement json
    ) throws IOException {

        Files.createDirectories(
                file.getParent()
        );

        Files.writeString(
                file,
                gson.toJson(json)
                        + System.lineSeparator(),
                StandardCharsets.UTF_8
        );
    }

    private String normalizeResourceId(
            String value
    ) throws ResourcePackBuildException {

        if (value == null
                || value.isBlank()) {

            throw new ResourcePackBuildException(
                    "An empty ID was found."
            );
        }

        String normalized = value
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_');

        if (!normalized.matches(
                "[a-z0-9._-]+"
        )) {
            throw new ResourcePackBuildException(
                    "Invalid ID for resource pack: "
                            + value
            );
        }

        return normalized;
    }

    private void validateSimpleFileName(
            String value,
            String configKey
    ) throws ResourcePackBuildException {

        if (value == null
                || value.isBlank()
                || value.contains("/")
                || value.contains("\\")
                || value.contains("..")) {

            throw new ResourcePackBuildException(
                    "resource-pack."
                            + configKey
                            + " is not valid."
            );
        }
    }

    private void ensureInside(
            Path parent,
            Path child,
            String value
    ) throws ResourcePackBuildException {

        if (!child.startsWith(parent)) {
            throw new ResourcePackBuildException(
                    "Path not allowed: "
                            + value
            );
        }
    }

    private String removeZipExtension(
            String fileName
    ) {
        return fileName.substring(
                0,
                fileName.length() - 4
        );
    }

    private void deleteRecursively(
            Path directory
    ) throws IOException {

        if (!Files.exists(directory)) {
            return;
        }

        Files.walkFileTree(
                directory,
                new SimpleFileVisitor<>() {

                    @Override
                    public FileVisitResult visitFile(
                            Path file,
                            BasicFileAttributes attributes
                    ) throws IOException {

                        Files.deleteIfExists(file);

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(
                            Path dir,
                            IOException exception
                    ) throws IOException {

                        if (exception != null) {
                            throw exception;
                        }

                        Files.deleteIfExists(dir);

                        return FileVisitResult.CONTINUE;
                    }
                }
        );
    }

    private void deleteQuietly(
            Path directory
    ) {
        try {
            deleteRecursively(directory);

        } catch (IOException exception) {
            plugin.getLogger().warning(
                    "Could not delete the temporary folder "
                            + directory
                            + ": "
                            + exception.getMessage()
            );
        }
    }

    private void sanitizeElementRotations(
            JsonObject model,
            VehicleDefinition definition
    ) throws ResourcePackBuildException {

        if (!model.has("elements")) {
            return;
        }

        if (!model.get("elements").isJsonArray()) {
            throw new ResourcePackBuildException(
                    "The 'elements' field of the model "
                            + definition.id()
                            + " is not a list."
            );
        }

        var elements =
                model.getAsJsonArray("elements");

        int removedZeroRotations = 0;
        int bakedRightAngleRotations = 0;

        for (int index = 0;
             index < elements.size();
             index++) {

            JsonElement elementValue =
                    elements.get(index);

            if (!elementValue.isJsonObject()) {
                throw new ResourcePackBuildException(
                        "The element "
                                + index
                                + " of the model "
                                + definition.id()
                                + " is not a JSON object."
                );
            }

            JsonObject element =
                    elementValue.getAsJsonObject();

            if (!element.has("rotation")) {
                continue;
            }

            JsonElement rotationValue =
                    element.get("rotation");

            if (!rotationValue.isJsonObject()) {
                throw new ResourcePackBuildException(
                        "The rotation of the element "
                                + index
                                + " of the model "
                                + definition.id()
                                + " is not a JSON object."
                );
            }

            JsonObject rotation =
                    rotationValue.getAsJsonObject();

            if (!rotation.has("angle")
                    || !rotation.get("angle").isJsonPrimitive()
                    || !rotation.getAsJsonPrimitive("angle")
                    .isNumber()) {

                throw new ResourcePackBuildException(
                        "The 'angle' field is missing or invalid in the rotation of element "
                                + index
                                + " of the model "
                                + definition.id()
                );
            }

            double angle =
                    rotation.get("angle")
                            .getAsDouble();

            /*
             * Una rotación de cero grados no produce
             * ningún cambio visual.
             */
            if (Math.abs(angle) < 0.000001) {

                element.remove("rotation");

                removedZeroRotations++;

                continue;
            }

            if (!rotation.has("axis")
                    || !rotation.get("axis").isJsonPrimitive()
                    || !rotation.getAsJsonPrimitive("axis")
                    .isString()) {

                throw new ResourcePackBuildException(
                        "The 'axis' field is missing or invalid in the rotation of element "
                                + index
                                + " of the model "
                                + definition.id()
                                + ". Angle: "
                                + angle
                );
            }

            String axis =
                    rotation.get("axis")
                            .getAsString()
                            .toLowerCase(Locale.ROOT);

            if (!axis.equals("x")
                    && !axis.equals("y")
                    && !axis.equals("z")) {

                throw new ResourcePackBuildException(
                        "The 'axis' field is missing or invalid in the rotation of element "
                                + index
                                + " of the model "
                                + definition.id()
                                + ": "
                                + axis
                );
            }

            if (!rotation.has("origin")
                    || !rotation.get("origin").isJsonArray()
                    || rotation.getAsJsonArray("origin")
                    .size() != 3) {

                throw new ResourcePackBuildException(
                        "The 'origin' field is missing or invalid in the rotation of element "
                                + index
                                + " of the model "
                                + definition.id()
                );
            }

            /*
             * Minecraft permite las rotaciones normales
             * comprendidas entre -45 y 45 grados.
             */
            if (angle >= -45.0 && angle <= 45.0) {

                rotation.addProperty(
                        "axis",
                        axis
                );

                continue;
            }

            /*
             * Las rotaciones exactas de 90, -90, 180
             * o -180 grados pueden hornearse en las
             * coordenadas del cubo.
             */
            if (isRightAngleRotation(angle)) {

                bakeElementRotation(
                        element,
                        rotation,
                        angle,
                        axis,
                        index,
                        definition
                );

                element.remove("rotation");

                bakedRightAngleRotations++;

                continue;
            }

            throw new ResourcePackBuildException(
                    "The 'angle' field is missing or invalid in the rotation of element "
                            + index
                            + " of the model "
                            + definition.id()
                            + ": "
                            + angle
                            + " degrees. Minecraft only supports "
                            + "rotations of elements between "
                            + "-45 and 45 degrees."
            );
        }

        plugin.getLogger().info(
                "Rotations processed for "
                        + definition.id()
                        + ": "
                        + removedZeroRotations
                        + " rotations of 0° removed, "
                        + bakedRightAngleRotations
                        + " right-angle rotations baked."
        );
    }

    private boolean isRightAngleRotation(
            double angle
    ) {
        return approximately(angle, 90.0)
                || approximately(angle, -90.0)
                || approximately(angle, 180.0)
                || approximately(angle, -180.0);
    }

    private boolean approximately(
            double first,
            double second
    ) {
        return Math.abs(first - second)
                < 0.000001;
    }

    private void bakeElementRotation(
            JsonObject element,
            JsonObject rotation,
            double angle,
            String axis,
            int elementIndex,
            VehicleDefinition definition
    ) throws ResourcePackBuildException {

        double[] from = readVector3(
                element,
                "from",
                elementIndex,
                definition
        );

        double[] to = readVector3(
                element,
                "to",
                elementIndex,
                definition
        );

        double[] origin = readVector3(
                rotation,
                "origin",
                elementIndex,
                definition
        );

        /*
         * Generamos las ocho esquinas del prisma.
         */
        double[][] corners = new double[][]{
                {from[0], from[1], from[2]},
                {from[0], from[1], to[2]},
                {from[0], to[1], from[2]},
                {from[0], to[1], to[2]},
                {to[0], from[1], from[2]},
                {to[0], from[1], to[2]},
                {to[0], to[1], from[2]},
                {to[0], to[1], to[2]}
        };

        double minimumX =
                Double.POSITIVE_INFINITY;

        double minimumY =
                Double.POSITIVE_INFINITY;

        double minimumZ =
                Double.POSITIVE_INFINITY;

        double maximumX =
                Double.NEGATIVE_INFINITY;

        double maximumY =
                Double.NEGATIVE_INFINITY;

        double maximumZ =
                Double.NEGATIVE_INFINITY;

        for (double[] corner : corners) {

            double[] rotated = rotatePoint(
                    corner,
                    origin,
                    angle,
                    axis
            );

            minimumX = Math.min(
                    minimumX,
                    rotated[0]
            );

            minimumY = Math.min(
                    minimumY,
                    rotated[1]
            );

            minimumZ = Math.min(
                    minimumZ,
                    rotated[2]
            );

            maximumX = Math.max(
                    maximumX,
                    rotated[0]
            );

            maximumY = Math.max(
                    maximumY,
                    rotated[1]
            );

            maximumZ = Math.max(
                    maximumZ,
                    rotated[2]
            );
        }

        element.add(
                "from",
                createVector3(
                        cleanCoordinate(minimumX),
                        cleanCoordinate(minimumY),
                        cleanCoordinate(minimumZ)
                )
        );

        element.add(
                "to",
                createVector3(
                        cleanCoordinate(maximumX),
                        cleanCoordinate(maximumY),
                        cleanCoordinate(maximumZ)
                )
        );
    }

    private double[] rotatePoint(
            double[] point,
            double[] origin,
            double angle,
            String axis
    ) {

        /*
         * Trasladamos el punto para que el origen
         * de rotación quede en 0,0,0.
         */
        double x = point[0] - origin[0];
        double y = point[1] - origin[1];
        double z = point[2] - origin[2];

        double radians =
                Math.toRadians(angle);

        double cos =
                Math.cos(radians);

        double sin =
                Math.sin(radians);

        double rotatedX;
        double rotatedY;
        double rotatedZ;

        switch (axis) {

            case "x" -> {
                rotatedX = x;
                rotatedY = y * cos - z * sin;
                rotatedZ = y * sin + z * cos;
            }

            case "y" -> {
                rotatedX = x * cos + z * sin;
                rotatedY = y;
                rotatedZ = -x * sin + z * cos;
            }

            case "z" -> {
                rotatedX = x * cos - y * sin;
                rotatedY = x * sin + y * cos;
                rotatedZ = z;
            }

            default -> throw new IllegalArgumentException(
                    "Invalid rotation axis: "
                            + axis
            );
        }

        /*
         * Volvemos a trasladar el punto a su
         * posición original.
         */
        return new double[]{
                rotatedX + origin[0],
                rotatedY + origin[1],
                rotatedZ + origin[2]
        };
    }

    private double[] readVector3(
            JsonObject object,
            String field,
            int elementIndex,
            VehicleDefinition definition
    ) throws ResourcePackBuildException {

        if (!object.has(field)
                || !object.get(field).isJsonArray()
                || object.getAsJsonArray(field)
                .size() != 3) {

            throw new ResourcePackBuildException(
                    "The field "
                            + field
                            + " of element "
                            + elementIndex
                            + " of the model "
                            + definition.id()
                            + " is not a valid vector."
            );
        }

        var array =
                object.getAsJsonArray(field);

        try {
            return new double[]{
                    array.get(0).getAsDouble(),
                    array.get(1).getAsDouble(),
                    array.get(2).getAsDouble()
            };

        } catch (RuntimeException exception) {

            throw new ResourcePackBuildException(
                    "The field "
                            + field
                            + " of element "
                            + elementIndex
                            + " of the model "
                            + definition.id()
                            + " contains non-numeric values.",
                    exception
            );
        }
    }

    private JsonArray createVector3(
            double x,
            double y,
            double z
    ) {
        JsonArray result =
                new JsonArray();

        result.add(x);
        result.add(y);
        result.add(z);

        return result;
    }

    private double cleanCoordinate(
            double value
    ) {
        double rounded =
                Math.round(value * 100000.0)
                        / 100000.0;

        if (Math.abs(rounded) < 0.000001) {
            return 0;
        }

        return rounded;
    }

    private void validateGeneratedZip(
            Path zipFile,
            Map<String, VehicleDefinition> definitions
    ) throws IOException,
            ResourcePackBuildException {

        try (ZipFile archive =
                     new ZipFile(zipFile.toFile())) {

            if (archive.getEntry("pack.mcmeta") == null) {
                throw new ResourcePackBuildException(
                        "The generated ZIP does not contain pack.mcmeta."
                );
            }

            for (VehicleDefinition definition
                    : definitions.values()) {

                String resourceId =
                        normalizeResourceId(
                                definition.id()
                        );

                String itemPath =
                        "assets/"
                                + NAMESPACE
                                + "/items/"
                                + resourceId
                                + ".json";

                String modelPath =
                        "assets/"
                                + NAMESPACE
                                + "/models/item/"
                                + resourceId
                                + ".json";

                if (archive.getEntry(itemPath) == null) {
                    throw new ResourcePackBuildException(
                            "The generated ZIP does not contain: "
                                    + itemPath
                    );
                }

                if (archive.getEntry(modelPath) == null) {
                    throw new ResourcePackBuildException(
                            "The generated ZIP does not contain: "
                                    + modelPath
                    );
                }

                for (String textureKey
                        : definition.textures().keySet()) {

                    String normalizedTextureKey =
                            normalizeResourceId(
                                    textureKey
                            );

                    String texturePath =
                            "assets/"
                                    + NAMESPACE
                                    + "/textures/item/"
                                    + resourceId
                                    + "/"
                                    + normalizedTextureKey
                                    + ".png";

                    if (archive.getEntry(texturePath) == null) {
                        throw new ResourcePackBuildException(
                                "The generated ZIP does not contain: "
                                        + texturePath
                        );
                    }

                    plugin.getLogger().info(
                            "Verified in the ZIP: "
                                    + texturePath
                    );
                }
            }
        }
    }
}