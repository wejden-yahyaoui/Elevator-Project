package org.paumard.elevator.student;

import org.paumard.elevator.Building;
import org.paumard.elevator.Elevator;
import org.paumard.elevator.event.DIRECTION;
import org.paumard.elevator.model.Person;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DumbElevator implements Elevator {
    private DIRECTION direction = DIRECTION.UP;
    private int currentFloor = 1;
    private final String id;
    private final int capacity;
    private LocalTime time; 
    private List<List<Person>> peopleByFloor;
    private List<Person> people = new ArrayList<>();
    private Person newPersonWaitingAtFloor; 
    private Map<Integer, Integer> peopleByFloorS = new HashMap<Integer, Integer>();
    private boolean lastPersonArrived = false;


    public DumbElevator(int capacity, String id) {
        this.id = id;
        this.capacity = capacity;
    }
    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void startsAtFloor(LocalTime time, int initialFloor) {
        this.currentFloor = initialFloor;
        this.setTime(time);
    }
    
    public LocalTime getTime() {
		return time;
	}


	public void setTime(LocalTime time) {
		this.time = time;
	}

    @Override
    public void peopleWaiting(List<List<Person>> peopleByFloor) {
    	this.setPeopleByFloor(peopleByFloor);

    }
    
    public List<List<Person>> getPeopleByFloor() {
		return peopleByFloor;
	}


	public void setPeopleByFloor(List<List<Person>> peopleByFloor) {
		this.peopleByFloor = peopleByFloor;
	}

  
		
	
	@Override
    public List<Integer> chooseNextFloors() {
		

    int peopleLeftAtFloors =(int) peopleByFloor.stream()
    											.map(list -> list.size())
    											.reduce(0, (a,b)->(a+b));
    
    if(peopleLeftAtFloors == 0)
		this.currentFloor = 1;  
		
	if(peopleLeftAtFloors !=0 && this.currentFloor==1)
		this.direction=DIRECTION.UP;
	
	if(peopleLeftAtFloors !=0 && this.currentFloor==10)
		this.direction=DIRECTION.DOWN; 
	
	if(this.direction == DIRECTION.UP)
	{
    	if(peopleLeftAtFloors !=0 && (this.currentFloor==1||this.currentFloor==2||this.currentFloor==3||this.currentFloor==4||this.currentFloor==5||this.currentFloor==6||this.currentFloor==7||this.currentFloor==8||this.currentFloor==9 ))
    		this.currentFloor++; 
    	
	}
	if(this.direction==DIRECTION.DOWN)
	{
		if(peopleLeftAtFloors !=0 && (this.currentFloor==1||this.currentFloor==2||this.currentFloor==3||this.currentFloor==4||this.currentFloor==5||this.currentFloor==6||this.currentFloor==7||this.currentFloor==8||this.currentFloor==9||this.currentFloor==10))
   		 this.currentFloor--;  
	}
        
        return List.of(currentFloor);
    }

    @Override
    public void arriveAtFloor(int floor) {
    	this.currentFloor= floor;
    }

    @Override
    public void loadPeople(List<Person> person) {
    	people.addAll(person);
    }

    @Override
    public void unload(List<Person> person) {
    	peopleByFloor.forEach(p ->p.remove(person));
    }

    @Override
    public void newPersonWaitingAtFloor(int floor, Person person) {
    	this.setCurrentFloor(floor);
		this.setNewPersonWaitingAtFloor(person);
		peopleByFloor.forEach(p ->p.add(person));
    }
    
    public void setCurrentFloor(int currentFloor) {
		if (this.currentFloor > currentFloor) {
            setDirection(DIRECTION.DOWN);
        } else {
            setDirection(DIRECTION.UP);
        }
        this.currentFloor = currentFloor;
		
	}
    
    public void setDirection(DIRECTION direction) {
		this.direction = direction;
	}
    
    public void setNewPersonWaitingAtFloor(Person person) {
		this.newPersonWaitingAtFloor = person;
	}

    @Override
    public void lastPersonArrived() {
    	 this.lastPersonArrived = true;
    }
    
    public boolean isLastPersonArrived() {
		return lastPersonArrived;
	}

    @Override
    public void timeIs(LocalTime time) {
    	this.time=time;
    }

    @Override
    public void standByAtFloor(int currentFloor) {
    }
}