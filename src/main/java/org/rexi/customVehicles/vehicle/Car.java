package org.rexi.customVehicles.vehicle;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.rexi.customVehicles.CustomVehicles;
import org.rexi.customVehicles.definition.SeatDefinition;
import org.rexi.customVehicles.definition.VehicleDefinition;
import org.rexi.customVehicles.definition.VehiclePhysics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Car {

    private static final double STOP_THRESHOLD =
            0.001;

    private static final double MAXIMUM_SEAT_HORIZONTAL_CORRECTION =
            2.0;

    private static final double MAXIMUM_SEAT_VERTICAL_CORRECTION =
            1.5;

    /*
     * Definición cargada desde:
     *
     * plugins/CustomVehicles/Cars/<vehiculo>/config.yml
     */
    private final VehicleDefinition definition;

    /*
     * El Slime principal actúa como:
     *
     * - Punto de referencia físico.
     * - Asiento del conductor.
     */
    private final Slime body;

    /*
     * Representación visual del vehículo.
     */
    private final ItemDisplay modelDisplay;

    /*
     * Slimes utilizados como plazas de acompañante.
     */
    private final List<Slime> passengerSeats =
            new ArrayList<>();

    /*
     * El índice de cada definición coincide con el índice
     * del Slime correspondiente en passengerSeats.
     */
    private final List<SeatDefinition>
            passengerSeatDefinitions;

    /*
     * Valores físicos cargados desde config.yml.
     */
    private final double maximumForwardSpeed;
    private final double maximumReverseSpeed;

    private final double acceleration;
    private final double reverseAcceleration;

    private final double friction;
    private final float handling;
    private final double minimumTurnSpeed;

    /*
     * Estado dinámico del vehículo.
     */
    private double speed;
    private float yaw;

    /*
     * Estado de los controles.
     *
     * ProtocolLib puede modificar estos campos desde
     * el listener de paquetes del jugador.
     */
    private volatile int steering;
    private volatile boolean forward;
    private volatile boolean backward;

    public Car(
            Location location,
            VehicleDefinition definition
    ) {
        if (definition == null) {
            throw new IllegalArgumentException(
                    "La definición del vehículo no puede ser null."
            );
        }

        World world =
                location.getWorld();

        if (world == null) {
            throw new IllegalArgumentException(
                    "La ubicación del vehículo no tiene mundo."
            );
        }

        this.definition =
                definition;

        this.yaw =
                location.getYaw();

        VehiclePhysics physics =
                definition.physics();

        this.maximumForwardSpeed =
                physics.maximumForwardSpeed();

        this.maximumReverseSpeed =
                physics.maximumReverseSpeed();

        this.acceleration =
                physics.acceleration();

        this.reverseAcceleration =
                physics.reverseAcceleration();

        this.friction =
                physics.friction();

        this.handling =
                physics.handling();

        this.minimumTurnSpeed =
                physics.minimumTurnSpeed();

        this.passengerSeatDefinitions =
                List.copyOf(
                        definition.passengerSeats()
                );

        /*
         * Cuerpo físico y asiento del conductor.
         */
        body = createSeatSlime(
                location,
                "driver",
                definition.driverSeat().id()
        );

        /*
         * Modelo visual del coche.
         */
        modelDisplay =
                createModelDisplay(location);

        /*
         * Creamos exactamente las plazas definidas
         * en seats.passengers.
         */
        for (SeatDefinition seatDefinition
                : passengerSeatDefinitions) {

            Slime passengerSeat =
                    createSeatSlime(
                            location,
                            "passenger",
                            seatDefinition.id()
                    );

            passengerSeats.add(
                    passengerSeat
            );
        }

        placePassengerSeatsInitially();

        /*
         * El primer posicionamiento no necesita predicción.
         */
        updateModelDisplay(
                new Vector(0, 0, 0)
        );
    }

    private Slime createSeatSlime(
            Location location,
            String role,
            String seatId
    ) {
        World world =
                location.getWorld();

        if (world == null) {
            throw new IllegalArgumentException(
                    "No se puede crear un asiento sin mundo."
            );
        }

        Slime slime = world.spawn(
                location,
                Slime.class
        );

        /*
         * Cambia temporalmente a false si necesitas
         * ver físicamente los asientos.
         */
        slime.setInvisible(CustomVehicles.getInstance().getConfig().getBoolean("hide_seats", true));

        slime.setSize(1);

        /*
         * Mantenemos IA activa para que Paper procese
         * normalmente las velocidades de los Slimes.
         */
        slime.setAI(true);
        slime.setWander(false);

        /*
         * La gravedad permite que el coche se adapte
         * a desniveles y permanezca sobre el terreno.
         */
        slime.setGravity(true);

        slime.setInvulnerable(true);
        slime.setSilent(true);
        slime.setPersistent(true);
        slime.setCollidable(false);

        NamespacedKey seatKey =
                new NamespacedKey(
                        CustomVehicles.getInstance(),
                        "vehicle_seat"
                );

        NamespacedKey roleKey =
                new NamespacedKey(
                        CustomVehicles.getInstance(),
                        "seat_role"
                );

        NamespacedKey idKey =
                new NamespacedKey(
                        CustomVehicles.getInstance(),
                        "seat_id"
                );

        NamespacedKey vehicleIdKey =
                new NamespacedKey(
                        CustomVehicles.getInstance(),
                        "vehicle_id"
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

        slime.getPersistentDataContainer().set(
                idKey,
                PersistentDataType.STRING,
                seatId
        );

        slime.getPersistentDataContainer().set(
                vehicleIdKey,
                PersistentDataType.STRING,
                definition.id()
        );

        return slime;
    }

    private ItemDisplay createModelDisplay(
            Location location
    ) {
        World world =
                location.getWorld();

        if (world == null) {
            throw new IllegalArgumentException(
                    "No se puede crear el modelo sin mundo."
            );
        }

        ItemDisplay display = world.spawn(
                location,
                ItemDisplay.class
        );

        /*
         * DIAMOND_BLOCK se utiliza como material base
         * porque funciona correctamente con item_model
         * para este modelo.
         */
        ItemStack itemStack =
                new ItemStack(
                        Material.DIAMOND_BLOCK
                );

        ItemMeta itemMeta =
                itemStack.getItemMeta();

        if (itemMeta == null) {
            display.remove();

            throw new IllegalStateException(
                    "No se pudo obtener ItemMeta "
                            + "para DIAMOND_BLOCK."
            );
        }

        String modelId =
                normalizeModelId(
                        definition.id()
                );

        NamespacedKey modelKey =
                new NamespacedKey(
                        "customvehicles",
                        modelId
                );

        itemMeta.setItemModel(
                modelKey
        );

        itemStack.setItemMeta(
                itemMeta
        );

        display.setItemStack(
                itemStack
        );

        /*
         * NONE evita aplicar transformaciones propias
         * de inventarios, manos o marcos.
         */
        display.setItemDisplayTransform(
                ItemDisplay.ItemDisplayTransform.NONE
        );

        display.setInvulnerable(true);
        display.setPersistent(true);
        display.setGravity(false);
        display.setSilent(true);

        display.setShadowRadius(0.0F);
        display.setShadowStrength(0.0F);

        /*
         * Caja de renderizado.
         */
        float displayWidth =
                (float) Math.max(
                        4.0,
                        definition.dimensions().width()
                );

        float displayHeight =
                (float) Math.max(
                        4.0,
                        definition.dimensions().height()
                );

        display.setDisplayWidth(
                displayWidth
        );

        display.setDisplayHeight(
                displayHeight
        );

        display.setViewRange(
                64.0F
        );

        display.setBrightness(
                new Display.Brightness(
                        15,
                        15
                )
        );

        /*
         * El display se actualiza cada tick usando una
         * posición prevista.
         *
         * Una teleportDuration de 1 añadía retraso visual
         * respecto al Slime principal.
         */
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(0);
        display.setTeleportDuration(1);

        applyModelTransformation(
                display
        );

        if (CustomVehicles.getInstance()
                .getConfig()
                .getBoolean("debug", false)) {

            CustomVehicles.getInstance()
                    .getLogger()
                    .info(
                            "ItemDisplay creado"
                                    + " | entidad="
                                    + display.getUniqueId()
                                    + " | material="
                                    + itemStack.getType()
                                    + " | modelo="
                                    + modelKey
                                    + " | escala="
                                    + definition.model().scale()
                    );
        }

        return display;
    }

    private void applyModelTransformation(
            ItemDisplay display
    ) {
        float scale =
                definition.model().scale();

        if (!Float.isFinite(scale)
                || scale <= 0.0F) {

            throw new IllegalArgumentException(
                    "La escala del vehículo "
                            + definition.id()
                            + " debe ser mayor que cero."
            );
        }

        Vector modelOffset =
                definition.model().offset();

        float pitchRadians =
                (float) Math.toRadians(
                        definition.model()
                                .pitchOffset()
                );

        float rollRadians =
                (float) Math.toRadians(
                        definition.model()
                                .rollOffset()
                );

        Transformation transformation =
                new Transformation(
                        new Vector3f(
                                (float) modelOffset.getX(),
                                (float) modelOffset.getY(),
                                (float) modelOffset.getZ()
                        ),

                        new AxisAngle4f(
                                pitchRadians,
                                1.0F,
                                0.0F,
                                0.0F
                        ),

                        new Vector3f(
                                scale,
                                scale,
                                scale
                        ),

                        new AxisAngle4f(
                                rollRadians,
                                0.0F,
                                0.0F,
                                1.0F
                        )
                );

        display.setTransformation(
                transformation
        );
    }

    public void update() {

        if (!body.isValid()
                || body.isDead()) {

            return;
        }

        if (!hasDriver()) {
            resetInput();
        }

        /*
         * Aceleración.
         */
        if (forward && !backward) {
            speed += acceleration;

        } else if (backward && !forward) {
            speed -= reverseAcceleration;
        }

        /*
         * Rozamiento.
         */
        speed *= friction;

        /*
         * Límites de velocidad.
         */
        speed = Math.max(
                -maximumReverseSpeed,
                Math.min(
                        speed,
                        maximumForwardSpeed
                )
        );

        if (Math.abs(speed)
                < STOP_THRESHOLD) {

            speed = 0;
        }

        /*
         * Dirección.
         */
        if (Math.abs(speed)
                > minimumTurnSpeed) {

            int effectiveSteering =
                    steering;

            if (speed < 0) {
                effectiveSteering *= -1;
            }

            yaw +=
                    effectiveSteering
                            * handling;

            yaw = normalizeYaw(yaw);
        }

        /*
         * Movimiento horizontal.
         */
        double radians =
                Math.toRadians(yaw);

        Vector movement =
                new Vector(
                        -Math.sin(radians),
                        0,
                        Math.cos(radians)
                ).multiply(speed);

        /*
         * Conservamos el movimiento vertical del
         * cuerpo principal.
         */
        movement.setY(
                body.getVelocity().getY()
        );

        body.setVelocity(
                movement
        );

        body.setRotation(
                yaw,
                0
        );

        /*
         * Tanto los asientos como el modelo utilizan
         * la misma posición prevista.
         */
        updatePassengerSeats(
                movement,
                yaw
        );

        updateModelDisplay(
                movement
        );
    }

    private void updatePassengerSeats(
            Vector carVelocity,
            float currentYaw
    ) {
        Location bodyLocation =
                body.getLocation();

        /*
         * Posición prevista completa del cuerpo
         * durante el siguiente tick.
         */
        Location nextBodyLocation =
                bodyLocation.clone()
                        .add(
                                carVelocity.getX(),
                                carVelocity.getY(),
                                carVelocity.getZ()
                        );

        for (int index = 0;
             index < passengerSeats.size();
             index++) {

            Slime seat =
                    passengerSeats.get(index);

            if (!seat.isValid()
                    || seat.isDead()) {

                continue;
            }

            SeatDefinition seatDefinition =
                    passengerSeatDefinitions.get(
                            index
                    );

            Vector rotatedOffset =
                    rotateOffset(
                            seatDefinition
                                    .offset()
                                    .clone(),
                            currentYaw
                    );

            Location targetLocation =
                    nextBodyLocation.clone()
                            .add(
                                    rotatedOffset
                            );

            /*
             * Corrección exacta hacia la posición objetivo
             * en los tres ejes.
             *
             * No conservamos la velocidad Y independiente
             * del asiento porque eso provocaba que las
             * plazas variasen de altura.
             */
            Vector requiredVelocity =
                    targetLocation
                            .toVector()
                            .subtract(
                                    seat.getLocation()
                                            .toVector()
                            );

            limitHorizontalCorrection(
                    requiredVelocity
            );

            limitVerticalCorrection(
                    requiredVelocity
            );

            seat.setRotation(
                    currentYaw,
                    0
            );

            seat.setVelocity(
                    requiredVelocity
            );
        }
    }

    private void limitHorizontalCorrection(
            Vector requiredVelocity
    ) {
        Vector horizontalVelocity =
                new Vector(
                        requiredVelocity.getX(),
                        0,
                        requiredVelocity.getZ()
                );

        double maximumSquared =
                MAXIMUM_SEAT_HORIZONTAL_CORRECTION
                        * MAXIMUM_SEAT_HORIZONTAL_CORRECTION;

        if (horizontalVelocity.lengthSquared()
                <= maximumSquared) {

            return;
        }

        horizontalVelocity
                .normalize()
                .multiply(
                        MAXIMUM_SEAT_HORIZONTAL_CORRECTION
                );

        requiredVelocity.setX(
                horizontalVelocity.getX()
        );

        requiredVelocity.setZ(
                horizontalVelocity.getZ()
        );
    }

    private void limitVerticalCorrection(
            Vector requiredVelocity
    ) {
        double limitedY =
                Math.max(
                        -MAXIMUM_SEAT_VERTICAL_CORRECTION,
                        Math.min(
                                requiredVelocity.getY(),
                                MAXIMUM_SEAT_VERTICAL_CORRECTION
                        )
                );

        requiredVelocity.setY(
                limitedY
        );
    }

    private void placePassengerSeatsInitially() {

        Location bodyLocation =
                body.getLocation();

        bodyLocation.setYaw(yaw);
        bodyLocation.setPitch(0);

        for (int index = 0;
             index < passengerSeats.size();
             index++) {

            Slime seat =
                    passengerSeats.get(index);

            SeatDefinition seatDefinition =
                    passengerSeatDefinitions.get(
                            index
                    );

            Vector rotatedOffset =
                    rotateOffset(
                            seatDefinition
                                    .offset()
                                    .clone(),
                            yaw
                    );

            Location seatLocation =
                    bodyLocation.clone()
                            .add(
                                    rotatedOffset
                            );

            seatLocation.setYaw(yaw);
            seatLocation.setPitch(0);

            seat.teleport(
                    seatLocation
            );

            seat.setVelocity(
                    new Vector(0, 0, 0)
            );
        }
    }

    private void updateModelDisplay(
            Vector carVelocity
    ) {
        if (!modelDisplay.isValid()
                || modelDisplay.isDead()) {

            return;
        }

        /*
         * body.getLocation() todavía representa la
         * posición del tick actual.
         *
         * Sumamos la velocidad para colocar el modelo
         * donde estará el cuerpo en el siguiente tick.
         */
        Location predictedLocation =
                body.getLocation()
                        .clone()
                        .add(
                                carVelocity.getX(),
                                carVelocity.getY(),
                                carVelocity.getZ()
                        );

        predictedLocation.setYaw(
                yaw
                        + definition.model()
                        .yawOffset()
        );

        predictedLocation.setPitch(0);

        modelDisplay.teleport(
                predictedLocation
        );
    }

    private Vector rotateOffset(
            Vector offset,
            float currentYaw
    ) {
        double radians =
                Math.toRadians(currentYaw);

        double cos =
                Math.cos(radians);

        double sin =
                Math.sin(radians);

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

    private float normalizeYaw(
            float value
    ) {
        while (value > 180.0F) {
            value -= 360.0F;
        }

        while (value < -180.0F) {
            value += 360.0F;
        }

        return value;
    }

    private String normalizeModelId(
            String value
    ) {
        return value
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_');
    }

    public boolean mountDriver(
            Player player
    ) {
        if (hasDriver()) {
            return false;
        }

        return body.addPassenger(
                player
        );
    }

    public boolean mountPassenger(
            Player player
    ) {
        for (Slime seat
                : passengerSeats) {

            if (seat.getPassengers()
                    .isEmpty()) {

                return seat.addPassenger(
                        player
                );
            }
        }

        return false;
    }

    public boolean hasDriver() {
        return !body.getPassengers()
                .isEmpty();
    }

    public boolean isDriverSeat(
            Slime seat
    ) {
        return body.getUniqueId()
                .equals(
                        seat.getUniqueId()
                );
    }

    public boolean isPassengerSeat(
            Slime seat
    ) {
        for (Slime passengerSeat
                : passengerSeats) {

            if (passengerSeat
                    .getUniqueId()
                    .equals(
                            seat.getUniqueId()
                    )) {

                return true;
            }
        }

        return false;
    }

    public boolean containsSeat(
            Slime seat
    ) {
        return isDriverSeat(seat)
                || isPassengerSeat(seat);
    }

    public void resetInput() {
        forward = false;
        backward = false;
        steering = 0;
    }

    public void stop() {

        resetInput();

        speed = 0;

        body.setVelocity(
                new Vector(0, 0, 0)
        );

        for (Slime seat
                : passengerSeats) {

            seat.setVelocity(
                    new Vector(0, 0, 0)
            );
        }
    }

    public void remove() {

        stop();

        body.eject();

        for (Slime seat
                : passengerSeats) {

            seat.eject();
            seat.remove();
        }

        if (modelDisplay.isValid()) {
            modelDisplay.remove();
        }

        body.remove();
    }

    public VehicleDefinition getDefinition() {
        return definition;
    }

    public Slime getBody() {
        return body;
    }

    public Slime getDriverSeat() {
        return body;
    }

    public ItemDisplay getModelDisplay() {
        return modelDisplay;
    }

    public List<Slime> getPassengerSeats() {
        return Collections.unmodifiableList(
                passengerSeats
        );
    }

    public double getSpeed() {
        return speed;
    }

    public float getYaw() {
        return yaw;
    }

    public int getSteering() {
        return steering;
    }

    public void setSteering(
            int steering
    ) {
        this.steering =
                Math.max(
                        -1,
                        Math.min(
                                steering,
                                1
                        )
                );
    }

    public void setForward(
            boolean forward
    ) {
        this.forward =
                forward;
    }

    public void setBackward(
            boolean backward
    ) {
        this.backward =
                backward;
    }
}