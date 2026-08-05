package org.rexi.customVehicles.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Slime;
import org.bukkit.scheduler.BukkitTask;
import org.rexi.customVehicles.CustomVehicles;
import org.rexi.customVehicles.vehicle.Car;

import java.util.ArrayList;
import java.util.List;

public class VehicleManager {

    private final List<Car> vehicles =
            new ArrayList<>();

    private BukkitTask updateTask;

    public void start() {
        updateTask = Bukkit
                .getScheduler()
                .runTaskTimer(
                        CustomVehicles.getInstance(),
                        () -> {
                            vehicles.removeIf(car -> {
                                if (!car.getBody().isValid()
                                        || car.getBody().isDead()) {

                                    car.remove();
                                    return true;
                                }

                                car.update();
                                return false;
                            });
                        },
                        1L,
                        1L
                );
    }

    public void addVehicle(Car car) {
        vehicles.add(car);
    }

    public Car getCarBySeat(Slime seat) {
        for (Car car : vehicles) {
            if (car.containsSeat(seat)) {
                return car;
            }
        }

        return null;
    }

    public void removeAll() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        for (Car car : new ArrayList<>(vehicles)) {
            car.remove();
        }

        vehicles.clear();
    }
}