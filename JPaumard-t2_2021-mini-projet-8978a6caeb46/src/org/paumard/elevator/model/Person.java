package org.paumard.elevator.model;

import java.time.LocalTime;

public class Person {
    private LocalTime arrivalTime;
    private String name;
    private int destinationFloor;

    public Person(LocalTime arrivalTime, String name, int destinationFloor) {
        this.arrivalTime = arrivalTime;
        this.name = name;
        this.destinationFloor = destinationFloor;
    }

    @Override
    public String toString() {
        return name + " arrived at " + arrivalTime + " going to " + destinationFloor;
    }

    public int getDestinationFloor() {
        return this.destinationFloor;
    }

    public String getName() {
        return this.name;
    }

    public LocalTime getArrivalTime() {
        return this.arrivalTime;
    }
}
