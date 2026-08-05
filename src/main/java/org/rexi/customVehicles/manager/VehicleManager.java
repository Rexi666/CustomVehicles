package org.rexi.customVehicles.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.rexi.customVehicles.CustomVehicles;
import org.rexi.customVehicles.vehicle.Car;

import java.util.ArrayList;
import java.util.List;

public class VehicleManager {

    private final List<Car> vehicles = new ArrayList<>();

    public void start() {

        Bukkit.getScheduler().runTaskTimer(
                CustomVehicles.getInstance(),
                () -> {
                    for (Car car : vehicles) {
                        car.update();
                    }
                },
                1L,
                1L
        );
    }

    public void addVehicle(Car car) {
        vehicles.add(car);
    }

    public Car getCarBySeat(
            ArmorStand seat
    ) {

        for (Car car : vehicles) {

            if (car.getDriverSeat()
                    .getUniqueId()
                    .equals(seat.getUniqueId())) {

                return car;
            }

            for (ArmorStand passenger :
                    car.getPassengerSeats()) {

                if (passenger.getUniqueId()
                        .equals(
                                seat.getUniqueId()
                        )) {

                    return car;
                }
            }
        }

        return null;
    }
}
