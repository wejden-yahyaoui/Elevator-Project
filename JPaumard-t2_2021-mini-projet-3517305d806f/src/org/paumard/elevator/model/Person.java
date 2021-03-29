package org.paumard.elevator.model;

public class Person {
    private String name;
    private int destinationFloor;

    public Person(String name, int destinationFloor) {
        this.name = name;
        this.destinationFloor = destinationFloor;
    }

    @Override
    public String toString() {
        return name + " going to " + destinationFloor;
    }

    public int getDestinationFloor() {
        return this.destinationFloor;
    }

    public String getName() {
        return this.name;
    }
}
