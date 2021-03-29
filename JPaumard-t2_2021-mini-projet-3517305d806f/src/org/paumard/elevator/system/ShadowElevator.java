package org.paumard.elevator.system;

import org.paumard.elevator.model.Direction;
import org.paumard.elevator.model.Person;
import org.paumard.elevator.model.WaitingList;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShadowElevator {

    private final int elevatorCapacity;
    private WaitingList waitingList;
    private int currentFloor = 1;
    private List<Person> people = new ArrayList<>();
    private int nextFloor;
    private boolean lastPersonArrived = false;
    private boolean stopped;

    public ShadowElevator(int elevatorCapacity, WaitingList peopleWaitingPerFloor) {
        this.elevatorCapacity = elevatorCapacity;
        this.waitingList = peopleWaitingPerFloor;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public Optional<Person> getNextPersonToUnload(int currentFloor) {
        return people.stream().filter(p -> p.getDestinationFloor() == currentFloor).findFirst();
    }

    public void printPeople() {
        if (people.isEmpty()) {
            System.out.printf("Elevator stopped at floor %d No one left in the elevator\n", this.currentFloor);
        } else {
            System.out.printf("Elevator stopped at floor %d\n", this.currentFloor);
            people.forEach(p -> System.out.println("\t" + p));
        }
    }

    public void startsAtFloor(int floor) {
        this.currentFloor = floor;
    }

    public void moveTo(int nextFloor) {
        this.currentFloor = nextFloor;
    }

    public void setNextFloor(int nextFloor) {
        this.nextFloor = nextFloor;
    }

    public int getNextFloor() {
        return nextFloor;
    }

    public void loadPerson(Person person) {
        people.add(person);
    }

    public void unload(Person person) {
        people.remove(person);
    }

    public Optional<Person> getNextPersonToLoadFromCurrentFloor() {
        if (this.people.size() < this.elevatorCapacity) {
            return waitingList.getNextPersonToLoad(this.currentFloor);
        } else {
            return Optional.empty();
        }
    }

    public Optional<Person> getNextPersonToLoad(int nextFloor, int currentFloor) {
        if (this.people.size() < this.elevatorCapacity) {
            return waitingList.getNextPersonToLoad(nextFloor, currentFloor);
        } else {
            return Optional.empty();
        }
    }

    public boolean isAnyoneWaitingAtOtherFloor() {
        return this.waitingList.countPeopleAtOtherFloor(this.currentFloor) > 0;
    }

    public boolean isAnyoneWaitingAtCurrentFloor() {
        return !this.waitingList.getListFor(this.currentFloor).isEmpty();
    }

    public void lastPersonArrived() {
        this.lastPersonArrived = true;
    }

    public boolean hasLastPersonArrived() {
        return this.lastPersonArrived;
    }

    public void stopping() {
        this.stopped = true;
    }

    public boolean isStopped() {
        return stopped;
    }
}
