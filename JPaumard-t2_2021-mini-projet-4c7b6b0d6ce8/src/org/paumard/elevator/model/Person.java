package org.paumard.elevator.model;

import org.paumard.elevator.Building;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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
        Duration waitingTime = Duration.between(arrivalTime, Building.time);
        List<String> waitingTimeElements = new ArrayList<>();
        if (waitingTime.toSeconds() >= 3600) {
            waitingTimeElements.add(waitingTime.toHoursPart() + "h");
        }
        if (waitingTime.toSeconds() >= 60) {
            waitingTimeElements.add(waitingTime.toMinutesPart() + "mn");
        }
        waitingTimeElements.add(waitingTime.toSecondsPart() + "s");
        String waitingTimeAsString = String.join(" ", waitingTimeElements);
        return name + " arrived at " + arrivalTime +
                " [waited for " + waitingTimeAsString + "]" +
                " going to " + destinationFloor;
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
