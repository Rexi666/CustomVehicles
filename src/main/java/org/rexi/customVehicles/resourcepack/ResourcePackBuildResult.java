package org.rexi.customVehicles.resourcepack;

import java.nio.file.Path;

public record ResourcePackBuildResult(
        Path zipFile,
        String sha1,
        int vehicleCount
) {
}