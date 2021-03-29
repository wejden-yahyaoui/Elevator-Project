package org.paumard.elevator.student;
import org.paumard.elevator.model.Direction;

import org.paumard.elevator.Elevator;
import org.paumard.elevator.model.Person;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class DumbElevator implements Elevator {

	private Random random = new Random(314L);
	
    private int elevatorCapacity;
	private boolean lastPersonArrived = false;

	private int peopleInElevator = 0;
	private int peopleWaitingAtFloors = 0;
	
	 private Direction direction;
	 private int currentFloor =1;
	 private Person newPersonWaitingAtFloor; 
	 private int floorNumber;
	 private LocalTime time;
	 

    public DumbElevator(int elevatorCapacity) {
        this.elevatorCapacity = elevatorCapacity;
    }

    @Override
    public void startsAtFloor(LocalTime time, int initialFloor) {
    	this.floorNumber=initialFloor;  
		this.setTime(time);
    }
    

	public void setTime(LocalTime time) {
		this.time = time;
	}

    

	public int getFloorNumber() {
		return floorNumber;
	}


	public void setFloorNumber(int floorNumber) {
		this.floorNumber = floorNumber;
	}

    @Override
    public void peopleWaiting(List<List<Person>> peopleByFloor) {
		this.peopleWaitingAtFloors = 
				peopleByFloor.stream()
							.mapToInt(List::size)
							.sum();
    }

    @Override
    public int chooseNextFloor() {
        	
        	if(peopleWaitingAtFloors == 0)
        		this.currentFloor = 1;  
        		
        	if(peopleWaitingAtFloors!=0 && this.currentFloor==1)
        		this.direction=Direction.UP;
        	
        	if(peopleWaitingAtFloors!=0 && this.currentFloor==4)
        		this.direction=Direction.DOWN; 

        	if(this.direction == Direction.UP)
        	{
            	if(peopleWaitingAtFloors!=0 && (this.currentFloor==1||this.currentFloor==2||this.currentFloor==3||this.currentFloor==4))
            		this.currentFloor=currentFloor + 1; 
            	
        	}
        	if(this.direction==Direction.DOWN)
        	{
        		if(peopleWaitingAtFloors!=0 && (this.currentFloor==1||this.currentFloor==2||this.currentFloor==3||this.currentFloor==4))
           		 this.currentFloor=currentFloor - 1;  
        	}
        	 
          
    	return currentFloor;
    
    		
    }
    
   
   

    @Override
    public void arriveAtFloor(int floor) {
    	this.currentFloor=floor;
    	
    }

    
	@Override
    public void loadPerson(Person person) {
    	this.peopleInElevator++;
    	this.peopleWaitingAtFloors--; 	
    	
    	
    }
    

    @Override
    public void unloadPerson(Person person) {
    	this.peopleInElevator--; 
    
        
    }

    @Override
    public void newPersonWaitingAtFloor(int floor, Person person) {
    	//this.setCurrentFloor(floor);
		this.peopleWaitingAtFloors++;
    	

    }
   
//   public void setCurrentFloor(int currentFloor) {
//		if (this.currentFloor > currentFloor) {
//           setDirection(Direction.DOWN);
//        } else {
//            setDirection(Direction.UP);
//       }
//        this.currentFloor = currentFloor;
//		
//	}
//    
//    public Direction getDirection() {
//		return direction;
//	}
//
//
//	  public void setDirection(Direction direction) {
//		this.direction = direction;
//	}
	
	

    @Override
    public void lastPersonArrived() {
    	this.lastPersonArrived  = true;
    }
}
