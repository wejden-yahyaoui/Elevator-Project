package org.paumard.elevator.system;

import org.paumard.elevator.Elevator;
import org.paumard.elevator.model.Person;
import org.paumard.elevator.model.WaitingList;

import java.time.LocalTime;
import java.util.List;

public class Elevators {

    private List<Elevator> elevators;

    public Elevators(List<Elevator> elevators) {
        this.elevators = elevators;
    }

    public void peopleWaiting(WaitingList waitingList) {
        this.elevators.forEach(elevator -> elevator.peopleWaiting(waitingList.getLists()));
    }

    public void timeIs(LocalTime time) {
        this.elevators.forEach(elevator -> elevator.timeIs(time));
    }

    public void lastPersonArrived() {
        this.elevators.forEach(Elevator::lastPersonArrived);
    }

    public List<Elevator> getElevators() {
        return this.elevators;
    }

    public void newPersonWaitingAtFloor(int floor, Person person) {
        elevators.stream().forEach(elevator -> elevator.newPersonWaitingAtFloor(floor, person));
    }

    public int count() {
        return this.elevators.size();
    }

    public String getElevatorId(int indexElevator) {
        return this.elevators.get(indexElevator).getId();
    }
}
