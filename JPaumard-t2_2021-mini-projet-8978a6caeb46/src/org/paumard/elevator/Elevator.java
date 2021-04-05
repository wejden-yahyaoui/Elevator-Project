package org.paumard.elevator;

import org.paumard.elevator.model.Person;

import java.time.LocalTime;
import java.util.List;

public interface Elevator {

    void startsAtFloor(LocalTime time, int initialFloor);

    void peopleWaiting(List<List<Person>> peopleByFloor);

    List<Integer> chooseNextFloors();

    void arriveAtFloor(int floor);

    void loadPeople(List<Person> person);

    void unload(List<Person> person);

    void newPersonWaitingAtFloor(int floor, Person person);

    void lastPersonArrived();

    void timeIs(LocalTime time);

    void standByAtFloor(int currentFloor);
}