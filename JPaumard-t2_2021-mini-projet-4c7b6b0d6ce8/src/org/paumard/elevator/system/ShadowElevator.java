package org.paumard.elevator.system;

import org.paumard.elevator.Building;
import org.paumard.elevator.model.Person;
import org.paumard.elevator.model.WaitingList;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.paumard.elevator.Building.ELEVATOR_LOADING_CAPACITY;
import static org.paumard.elevator.Building.PRINTER;

public class ShadowElevator {

    private final int elevatorCapacity;
    private int currentFloor = 1;
    private List<Person> people = new ArrayList<>();
    private List<Integer> nextFloors;
    private boolean lastPersonArrived = false;
    private boolean stopped;
    private long countLoadedPeople = 0L;
    private long countUnloadedPeople = 0L;
    private int maxLoad;
    private String elevatorId;
    private WaitingList waitingList;

    public ShadowElevator(int elevatorCapacity, String elevatorId, WaitingList waitingList) {
        this.elevatorCapacity = elevatorCapacity;
        this.elevatorId = elevatorId;
        this.waitingList = waitingList;
    }

    public void print(LocalTime time) {
        Duration totalDuration = Duration.between(Building.START_TIME, time);
        int s = totalDuration.toSecondsPart();
        int mn = totalDuration.toMinutesPart();
        PRINTER.printf("Total duration = %dmn %ds\n", mn, s);
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public List<Person> getPeople() {
        return new ArrayList<>(people);
    }

    public int numberOfPeopleInElevator() {
        return this.people.size();
    }

    public List<Person> getNextPeopleToUnload(int currentFloor) {
        List<Person> peopleToUnload =
                people.stream()
                        .filter(p -> p.getDestinationFloor() == currentFloor)
                        .collect(Collectors.toList());
        if (peopleToUnload.size() > ELEVATOR_LOADING_CAPACITY) {
            peopleToUnload = peopleToUnload.subList(0, ELEVATOR_LOADING_CAPACITY);
        }
        return peopleToUnload;
    }

    public void printPeople() {
        if (people.isEmpty()) {
            PRINTER.printf("Elevator [%s] stopped at floor %d No one left in the elevator\n", this.elevatorId, this.currentFloor);
        } else {
            PRINTER.printf("Elevator [%s] stopped at floor %d\n", this.elevatorId, this.currentFloor);
            people.forEach(p -> PRINTER.println("\t" + p));
        }
    }

    public void startsAtFloor(int floor) {
        this.currentFloor = floor;
    }

    public void moveTo(int nextFloor) {
        this.currentFloor = nextFloor;
        this.nextFloors.remove(0);
    }

    public void setNextFloors(List<Integer> nextFloors) {
        this.nextFloors = new ArrayList<>(nextFloors);
    }

    public List<Integer> getNextFloors() {
        return new ArrayList<>(nextFloors);
    }

    public void loadPeople(List<Person> people) {
        countLoadedPeople += people.size();
        this.people.addAll(people);
        maxLoad = Integer.max(maxLoad, this.people.size());
    }

    public void unload(List<Person> people) {
        countUnloadedPeople += people.size();
        this.people.removeAll(people);
    }

    public List<Person> getNextPeopleToLoad(List<Integer> nextFloors, int currentFloor) {
        if (this.people.size() < this.elevatorCapacity) {
            List<Person> nextLoadablePeople = waitingList.getNextLoadablePeople(nextFloors, currentFloor);
            int availableRoom = this.elevatorCapacity - this.people.size();
            if (nextLoadablePeople.size() > availableRoom) {
                nextLoadablePeople = nextLoadablePeople.subList(0, availableRoom);
            }
            waitingList.removePersonFromFloor(currentFloor, nextLoadablePeople);
            return nextLoadablePeople;
        } else {
            return List.of();
        }
    }

    public boolean hasNextPeopleToLoad(List<Integer> nextFloors, int currentFloor) {
        if (this.people.size() < this.elevatorCapacity) {
            boolean hasLoadablePeople = waitingList.hasLoadablePeople(nextFloors, currentFloor);
            return hasLoadablePeople;
        } else {
            return false;
        }
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

    public List<Person> getPeopleWaitingAtCurrentFloor() {
        return new ArrayList<>(this.waitingList.getListFor(this.currentFloor));
    }

    public void stopping() {
        this.stopped = true;
    }

    public boolean isStopped() {
        return stopped;
    }

    public boolean isRunning() {
        return !stopped;
    }

    public long getCountLoadedPeople() {
        return countLoadedPeople;
    }

    public long getCountUnloadedPeople() {
        return countUnloadedPeople;
    }

    public int getMaxLoad() {
        return this.maxLoad;
    }

    public String getId() {
        return this.elevatorId;
    }

    public int getNumberOfPeople() {
        return this.people.size();
    }

    public boolean availableRoom() {
        return this.people.size() < Building.ELEVATOR_CAPACITY;
    }
}
