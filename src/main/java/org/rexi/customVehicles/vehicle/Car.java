package org.rexi.customVehicles.vehicle;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.rexi.customVehicles.CustomVehicles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Car {

    private static final double ACCELERATION = 0.01;
    private static final double FRICTION = 0.98;

    private static final double MAX_FORWARD_SPEED = 0.5;
    private static final double MAX_BACKWARD_SPEED = -0.2;

    private static final double MIN_TURN_SPEED = 0.05;
    private static final float TURN_SPEED = 4.0F;

    /*
     * El Slime principal también actúa como asiento del conductor.
     */
    private final Slime body;

    /*
     * Cada acompañante tiene su propio Slime.
     */
    private final List<Slime> passengerSeats =
            new ArrayList<>();

    private final VehicleType type;

    private double speed;

    private volatile int steering;
    private volatile boolean forward;
    private volatile boolean backward;

    public Car(Location location, VehicleType type) {
        this.type = type;

        World world = location.getWorld();

        if (world == null) {
            throw new IllegalArgumentException(
                    "La ubicación no tiene mundo."
            );
        }

        /*
         * Slime principal: cuerpo y asiento del conductor.
         */
        body = createSeatSlime(
                location,
                "driver"
        );

        /*
         * Creamos una plaza adicional por cada acompañante.
         * El número total de plazas incluye al conductor.
         */
        for (int i = 1; i < type.getSeats(); i++) {
            Slime passengerSeat = createSeatSlime(
                    location,
                    "passenger"
            );

            passengerSeats.add(passengerSeat);
        }

        placePassengerSeatsInitially();
    }

    private Slime createSeatSlime(
            Location location,
            String role
    ) {
        Slime slime = location.getWorld().spawn(
                location,
                Slime.class
        );

        slime.setSize(1);
        slime.setInvisible(false);

        /*
         * IMPORTANTE:
         * La IA debe estar activa para que el Slime procese
         * correctamente la velocidad.
         */
        slime.setAI(true);

        /*
         * Impide que salte o deambule por sí mismo.
         * Papel 1.21.x.
         */
        slime.setWander(false);

        /*
         * Dejamos gravedad activa para que permanezca
         * apoyado sobre el suelo.
         */
        slime.setGravity(true);

        slime.setInvulnerable(true);
        slime.setSilent(true);
        slime.setPersistent(true);

        /*
         * Evita que los distintos asientos se empujen
         * entre sí o bloqueen el movimiento.
         */
        slime.setCollidable(false);

        NamespacedKey seatKey = new NamespacedKey(
                CustomVehicles.getInstance(),
                "vehicle_seat"
        );

        NamespacedKey roleKey = new NamespacedKey(
                CustomVehicles.getInstance(),
                "seat_role"
        );

        slime.getPersistentDataContainer().set(
                seatKey,
                PersistentDataType.STRING,
                "seat"
        );

        slime.getPersistentDataContainer().set(
                roleKey,
                PersistentDataType.STRING,
                role
        );

        return slime;
    }

    public void update() {

        if (!body.isValid() || body.isDead()) {
            return;
        }

        if (!hasDriver()) {
            resetInput();
        }

        if (forward && !backward) {
            speed += ACCELERATION;
        } else if (backward && !forward) {
            speed -= ACCELERATION;
        }

        speed *= FRICTION;

        speed = Math.max(
                MAX_BACKWARD_SPEED,
                Math.min(speed, MAX_FORWARD_SPEED)
        );

        if (Math.abs(speed) < 0.001) {
            speed = 0;
        }

        Location bodyLocation = body.getLocation();

        float yaw = bodyLocation.getYaw();

        if (Math.abs(speed) > MIN_TURN_SPEED) {

            int effectiveSteering = steering;

            if (speed < 0) {
                effectiveSteering *= -1;
            }

            yaw += effectiveSteering * TURN_SPEED;
        }

        body.setRotation(yaw, 0);

        /*
         * Calculamos manualmente la dirección horizontal.
         * Así no dependemos del pitch de la entidad.
         */
        double radians = Math.toRadians(yaw);

        Vector movement = new Vector(
                -Math.sin(radians),
                0,
                Math.cos(radians)
        ).multiply(speed);

        /*
         * Conservamos temporalmente la velocidad vertical para
         * que la gravedad siga funcionando correctamente.
         */
        movement.setY(body.getVelocity().getY());

        body.setVelocity(movement);

        CustomVehicles.getInstance()
                .getLogger()
                .info(
                        "speed=" + speed
                                + " requestedVelocity=" + movement
                                + " actualVelocity=" + body.getVelocity()
                                + " position=" + body.getLocation().toVector()
                );

        updatePassengerSeats(movement, yaw);
    }

    private void updatePassengerSeats(
            Vector carVelocity,
            float yaw
    ) {
        Location bodyLocation = body.getLocation();

        for (int i = 0; i < passengerSeats.size(); i++) {

            Slime seat = passengerSeats.get(i);

            if (!seat.isValid() || seat.isDead()) {
                continue;
            }

            Vector localOffset =
                    getPassengerOffset(i);

            Vector rotatedOffset = rotateOffset(
                    localOffset,
                    yaw
            );

            Location targetLocation = bodyLocation
                    .clone()
                    .add(rotatedOffset);

            Vector correction = targetLocation
                    .toVector()
                    .subtract(
                            seat.getLocation().toVector()
                    );

            /*
             * Sólo corregimos la posición horizontal.
             * La gravedad controla la altura.
             */
            correction.setY(0);
            correction.multiply(0.45);

            double maximumCorrection = 1.0;

            if (correction.lengthSquared()
                    > maximumCorrection * maximumCorrection) {

                correction.normalize()
                        .multiply(maximumCorrection);
            }

            Vector seatVelocity = new Vector(
                    carVelocity.getX(),
                    seat.getVelocity().getY(),
                    carVelocity.getZ()
            ).add(correction);

            seat.setRotation(yaw, 0);
            seat.setVelocity(seatVelocity);
        }
    }

    private void placePassengerSeatsInitially() {

        Location bodyLocation = body.getLocation();

        for (int i = 0; i < passengerSeats.size(); i++) {

            Slime seat = passengerSeats.get(i);

            Vector offset = rotateOffset(
                    getPassengerOffset(i),
                    bodyLocation.getYaw()
            );

            Location seatLocation = bodyLocation
                    .clone()
                    .add(offset);

            seatLocation.setYaw(bodyLocation.getYaw());
            seatLocation.setPitch(0);

            /*
             * Aquí sí podemos usar teleport porque los asientos
             * todavía no tienen jugadores montados.
             */
            seat.teleport(seatLocation);
        }
    }

    private Vector getPassengerOffset(int index) {
        /*
         * X representa izquierda/derecha.
         * Z representa delante/detrás.
         *
         * Puedes ajustar estos valores más adelante
         * al tamaño de tu modelo 3D.
         */
        return switch (index) {
            case 0 -> new Vector(0.9, 0, 0);
            case 1 -> new Vector(-0.9, 0, -1.2);
            case 2 -> new Vector(0.9, 0, -1.2);
            case 3 -> new Vector(-0.9, 0, -2.4);
            case 4 -> new Vector(0.9, 0, -2.4);
            case 5 -> new Vector(-0.9, 0, -3.6);
            case 6 -> new Vector(0.9, 0, -3.6);
            default -> new Vector(
                    0,
                    0,
                    -1.2 * index
            );
        };
    }

    private Vector rotateOffset(
            Vector offset,
            float yaw
    ) {
        double radians = Math.toRadians(-yaw);

        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double rotatedX =
                offset.getX() * cos
                        - offset.getZ() * sin;

        double rotatedZ =
                offset.getX() * sin
                        + offset.getZ() * cos;

        return new Vector(
                rotatedX,
                offset.getY(),
                rotatedZ
        );
    }

    public boolean mountDriver(Player player) {
        if (hasDriver()) {
            return false;
        }

        return body.addPassenger(player);
    }

    public boolean mountPassenger(Player player) {
        for (Slime seat : passengerSeats) {
            if (seat.getPassengers().isEmpty()) {
                return seat.addPassenger(player);
            }
        }

        return false;
    }

    public boolean hasDriver() {
        return !body.getPassengers().isEmpty();
    }

    public boolean isDriverSeat(Slime seat) {
        return body.getUniqueId().equals(
                seat.getUniqueId()
        );
    }

    public boolean isPassengerSeat(Slime seat) {
        for (Slime passengerSeat : passengerSeats) {
            if (passengerSeat.getUniqueId().equals(
                    seat.getUniqueId()
            )) {
                return true;
            }
        }

        return false;
    }

    public boolean containsSeat(Slime seat) {
        return isDriverSeat(seat)
                || isPassengerSeat(seat);
    }

    public void resetInput() {
        forward = false;
        backward = false;
        steering = 0;
    }

    public void remove() {
        resetInput();

        body.eject();

        for (Slime seat : passengerSeats) {
            seat.eject();
            seat.remove();
        }

        body.remove();
    }

    public Slime getBody() {
        return body;
    }

    public Slime getDriverSeat() {
        return body;
    }

    public List<Slime> getPassengerSeats() {
        return Collections.unmodifiableList(
                passengerSeats
        );
    }

    public VehicleType getType() {
        return type;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSteering(int steering) {
        this.steering = Math.max(
                -1,
                Math.min(steering, 1)
        );
    }

    public void setForward(boolean forward) {
        this.forward = forward;
    }

    public void setBackward(boolean backward) {
        this.backward = backward;
    }
}