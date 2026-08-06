package org.rexi.customVehicles.definition;

public enum VehicleCategory {

    CAR("Cars"),
    MOTORCYCLE("Motorcycles"),
    BOAT("Boats"),
    PLANE("Planes"),
    HELICOPTER("Helicopters");

    private final String folderName;

    VehicleCategory(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderName() {
        return folderName;
    }
}