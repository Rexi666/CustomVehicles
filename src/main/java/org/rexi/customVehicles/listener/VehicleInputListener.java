package org.rexi.customVehicles.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.rexi.customVehicles.CustomVehicles;
import org.rexi.customVehicles.vehicle.Car;

public class VehicleInputListener {

    public void register() {

        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(
                        CustomVehicles.getInstance(),
                        PacketType.Play.Client.STEER_VEHICLE
                ) {

                    @Override
                    public void onPacketReceiving(PacketEvent event) {

                        Player player = event.getPlayer();

                        player.sendMessage(
                                String.valueOf(player.getVehicle())
                        );

                        if (!(player.getVehicle() instanceof ArmorStand stand)) {
                            return;
                        }

                        Car car = CustomVehicles
                                .getInstance()
                                .getVehicleManager()
                                .getCarBySeat(stand);

                        if (car == null) {
                            return;
                        }


                        try {

                            Object input = event.getPacket()
                                    .getModifier()
                                    .read(0);

                            boolean forward = (boolean)
                                    input.getClass()
                                            .getMethod("forward")
                                            .invoke(input);

                            boolean backward = (boolean)
                                    input.getClass()
                                            .getMethod("backward")
                                            .invoke(input);

                            boolean left = (boolean)
                                    input.getClass()
                                            .getMethod("left")
                                            .invoke(input);

                            boolean right = (boolean)
                                    input.getClass()
                                            .getMethod("right")
                                            .invoke(input);

                            car.setForward(forward);
                            car.setBackward(backward);

                            if (left) {
                                car.setSteering(-1);
                            } else if (right) {
                                car.setSteering(1);
                            } else {
                                car.setSteering(0);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }
}