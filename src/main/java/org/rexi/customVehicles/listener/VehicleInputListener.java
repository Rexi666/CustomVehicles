package org.rexi.customVehicles.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.rexi.customVehicles.CustomVehicles;
import org.rexi.customVehicles.vehicle.Car;

import java.lang.reflect.Method;

public class VehicleInputListener {

    private Method forwardMethod;
    private Method backwardMethod;
    private Method leftMethod;
    private Method rightMethod;

    public void register() {
        ProtocolLibrary
                .getProtocolManager()
                .addPacketListener(
                        new PacketAdapter(
                                CustomVehicles.getInstance(),
                                PacketType.Play.Client.STEER_VEHICLE
                        ) {
                            @Override
                            public void onPacketReceiving(
                                    PacketEvent event
                            ) {
                                handleInput(event);
                            }
                        }
                );
    }

    private void handleInput(PacketEvent event) {
        Player player = event.getPlayer();

        if (!(player.getVehicle()
                instanceof Slime seat)) {
            return;
        }

        Car car = CustomVehicles
                .getInstance()
                .getVehicleManager()
                .getCarBySeat(seat);

        if (car == null) {
            return;
        }

        /*
         * Los acompañantes no controlan el vehículo.
         */
        if (!car.isDriverSeat(seat)) {
            return;
        }

        try {
            Object input = event.getPacket()
                    .getModifier()
                    .read(0);

            cacheMethods(input);

            boolean forward =
                    (boolean) forwardMethod.invoke(input);

            boolean backward =
                    (boolean) backwardMethod.invoke(input);

            boolean left =
                    (boolean) leftMethod.invoke(input);

            boolean right =
                    (boolean) rightMethod.invoke(input);

            car.setForward(forward);
            car.setBackward(backward);

            if (left == right) {
                car.setSteering(0);
            } else if (left) {
                car.setSteering(-1);
            } else {
                car.setSteering(1);
            }

        } catch (ReflectiveOperationException
                 | IndexOutOfBoundsException exception) {

            CustomVehicles.getInstance()
                    .getLogger()
                    .warning(
                            "No se pudo leer el input de "
                                    + player.getName()
                                    + ": "
                                    + exception.getMessage()
                    );
        }
    }

    private void cacheMethods(Object input)
            throws NoSuchMethodException {

        if (forwardMethod != null) {
            return;
        }

        Class<?> inputClass = input.getClass();

        forwardMethod =
                inputClass.getMethod("forward");

        backwardMethod =
                inputClass.getMethod("backward");

        leftMethod =
                inputClass.getMethod("left");

        rightMethod =
                inputClass.getMethod("right");
    }
}