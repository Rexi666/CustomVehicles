package org.rexi.customVehicles.definition;

import org.bukkit.util.Vector;

public record SeatDefinition(
        String id,
        SeatRole role,
        Vector offset
) {
}
