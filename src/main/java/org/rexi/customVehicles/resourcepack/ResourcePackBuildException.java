package org.rexi.customVehicles.resourcepack;

public class ResourcePackBuildException extends Exception {

    public ResourcePackBuildException(String message) {
        super(message);
    }

    public ResourcePackBuildException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
