package org.paumard.elevator;

import org.paumard.elevator.model.Person;

import java.time.LocalTime;
import java.util.List;

public interface Elevator {

    void startsAtFloor(LocalTime time, int initialFloor);

    void peopleWaiting(List<List<Person>> peopleByFloor);

    int chooseNextFloor();

    void arriveAtFloor(int floor);

    void loadPerson(Person person);

    void unloadPerson(Person person);

    void newPersonWaitingAtFloor(int floor, Person person);

    void lastPersonArrived();

	


	


	
}