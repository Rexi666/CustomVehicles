package org.rexi.customVehicles.vehicle;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Slime;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.rexi.customVehicles.CustomVehicles;

import java.util.ArrayList;
import java.util.List;

public class Car {

    private final Slime body;

    private final ArmorStand driverSeat;

    private final List<ArmorStand> passengerSeats =
            new ArrayList<>();

    private final VehicleType type;

    private double speed = 0;

    private int steering = 0;

    private boolean forward;

    private boolean backward;

    public Car(Location location, VehicleType type) {

        this.type = type;

        World world = location.getWorld();

        body = world.spawn(location, Slime.class);

        body.setInvisible(true);
        body.setAI(false);
        body.setInvulnerable(true);
        body.setGravity(false);
        body.setSilent(true);

        driverSeat = createSeat(location);

        for (int i = 1; i < type.getSeats(); i++) {
            passengerSeats.add(
                    createSeat(location)
            );
        }

        updateSeats();
    }

    private ArmorStand createSeat(Location location) {

        ArmorStand seat =
                location.getWorld().spawn(
                        location,
                        ArmorStand.class
                );

        NamespacedKey key = new NamespacedKey(
                CustomVehicles.getInstance(),
                "vehicle_seat"
        );

        seat.getPersistentDataContainer().set(
                key,
                PersistentDataType.STRING,
                "seat"
        );

        seat.setInvisible(true);
        seat.setMarker(false);
        seat.setGravity(false);
        seat.setInvulnerable(true);

        return seat;
    }

    public void update() {

        if (forward) {
            speed += 0.01;
        }

        if (backward) {
            speed -= 0.01;
        }

        speed *= 0.98;

        speed = Math.max(
                -0.2,
                Math.min(speed, 0.5)
        );

        Location loc = body.getLocation();

        loc.setPitch(0);

        if (Math.abs(speed) > 0.05) {

            loc.setYaw(
                    loc.getYaw()
                            + (steering * 4)
            );
        }

        Vector direction =
                loc.getDirection().normalize();

        loc.add(
                direction.multiply(speed)
        );

        body.teleport(loc);

        updateSeats();
    }

    private void updateSeats() {

        Location bodyLoc =
                body.getLocation();

        driverSeat.teleport(bodyLoc);

        for (int i = 0; i < passengerSeats.size(); i++) {

            ArmorStand seat =
                    passengerSeats.get(i);

            Location seatLoc =
                    bodyLoc.clone();

            switch (i) {

                case 0 ->
                        seatLoc.add(
                                0.8,
                                0,
                                0
                        );

                case 1 ->
                        seatLoc.add(
                                -0.8,
                                0,
                                0
                        );

                case 2 ->
                        seatLoc.add(
                                0.8,
                                0,
                                -1
                        );

                case 3 ->
                        seatLoc.add(
                                -0.8,
                                0,
                                -1
                        );
            }

            seat.teleport(seatLoc);
        }
    }

    public ArmorStand getDriverSeat() {
        return driverSeat;
    }

    public List<ArmorStand> getPassengerSeats() {
        return passengerSeats;
    }

    public Slime getBody() {
        return body;
    }

    public void setSteering(int steering) {
        this.steering = steering;
    }

    public void setForward(boolean forward) {
        this.forward = forward;
    }

    public void setBackward(boolean backward) {
        this.backward = backward;
    }
}