package org.paumard.elevator.student;

import org.paumard.elevator.Building;
import org.paumard.elevator.Elevator;
import org.paumard.elevator.event.DIRECTION;
import org.paumard.elevator.model.Person;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LessDumbElevator implements Elevator {
    private DIRECTION direction = DIRECTION.UP;
    private int currentFloor = 1;
	private final int capacity;
	private LocalTime time;
	private List<List<Person>> peopleByFloor = List.of();
	private List<Person> people = new ArrayList<Person>();
	List<Person> ListOfPeopleWaitingSomewhere = new ArrayList<>();
	private List<Integer> destinations = List.of(0);

	

    public LessDumbElevator(int capacity) {
    	this.capacity=capacity;
    }
    

    @Override
    public void startsAtFloor(LocalTime time, int initialFloor) {
    	this.time=time;
    }

    @Override
    public void peopleWaiting(List<List<Person>> peopleByFloor) {
    	this.peopleByFloor = peopleByFloor;
    }

    @Override
    public List<Integer> chooseNextFloors() {
    	
    	int numberOfPeopleWaiting =  countWaitingPeople();
    	
    	if ( numberOfPeopleWaiting > 0) {
    
    		List<Integer> nonEmptyFloors = findNonEmptyFloor();
    		Integer nonEmptyFloor = nonEmptyFloors.get(0);
    		
    		
			if (nonEmptyFloor != this.currentFloor) {
    			return nonEmptyFloors;
    		}
    		else {
    			int indexOfCurrentFloor = this.currentFloor -1;
    			
				List<Person> waitingListForCurrentFloor = this.peopleByFloor.get(indexOfCurrentFloor);
				List <Integer> destinationFloorsForCurrentFloor = findDestinationFloors(waitingListForCurrentFloor);
				
				this.destinations = destinationFloorsForCurrentFloor;
		
				return this.destinations ;
    			
    		}
			
    	}
    	
    	return List.of(1);

    	
    	
    }


	private List<Integer> findDestinationFloors(List<Person> waitingListForCurrentFloor) {
		return waitingListForCurrentFloor.stream()
								.map(person -> person.getDestinationFloor())
								.distinct()
								.collect(Collectors.toList());
	}


	private List<Integer> findNonEmptyFloor() {
		for ( int indexFloor = 0 ; indexFloor< Building.MAX_FLOOR ; indexFloor++ ) {
			if(!peopleByFloor.get(indexFloor).isEmpty()) {
				return  List.of(indexFloor+1);
			}
		}
		return List.of(-1);
	}


	private int countWaitingPeople() {
		return peopleByFloor.stream()
					.mapToInt(list -> list.size())
					.sum();
	}

    @Override
    public void arriveAtFloor(int floor) {
    	

    	this.currentFloor= floor;
    	
    }

    @Override
    public void loadPeople(List<Person> people) {
    	this.people.addAll(people);
    	int indexFloor = this.currentFloor - 1;
    	this.peopleByFloor.get(indexFloor).removeAll(people);
    	
    
    	
    }

    @Override
    public void unload(List<Person> people) {
    	this.people.removeAll(people);
    }

    @Override
    public void newPersonWaitingAtFloor(int floor, Person person) {
    	int indexFloor = floor - 1;
    	this.peopleByFloor.get(indexFloor).add(person);

    }

    @Override
    public void lastPersonArrived() {
    }

    @Override
    public void timeIs(LocalTime time) {
    	
    	LocalTime arrivalTime = LocalTime.of(5,0,0);
    	Duration between = Duration.between(time, arrivalTime);
    	
    	this.time= time;
    }

    @Override
    public void standByAtFloor(int currentFloor) {
    }
}