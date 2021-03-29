package org.paumard.elevator;

import org.paumard.elevator.event.Event;
import org.paumard.elevator.model.Person;
import org.paumard.elevator.model.WaitingList;
import org.paumard.elevator.student.DumbElevator;
import org.paumard.elevator.system.ShadowElevator;

import java.time.LocalTime;
import java.util.*;

public class Building {

    public static final int ELEVATOR_CAPACITY = 5;
    public static final int MAX_FLOOR = 4;
    public static final LocalTime START_TIME = LocalTime.of(6, 0, 0); // 6h 0mn 0s
    public static final int MAX_PEOPLE_ARRIVING = 50;
    public static final int MAX_NUMBER_OF_PEOPLE_IN_LINE = 7;

    public static final float PROBABILITY_TO_SEE_A_NEW_PERSON_IN_LINE = 0.025f;
    public static Random random = new Random(10L);

    public static void main(String[] args) {

        NavigableMap<LocalTime, Event> events = new TreeMap<>();

        LocalTime time = START_TIME;
        Event startEvent = new Event(Event.ELEVATOR_STARTS);
        events.put(time, startEvent);

        WaitingList peopleWaitingPerFloor = new WaitingList();
        Elevator elevator = new DumbElevator(ELEVATOR_CAPACITY);

        int totalNumberOfPeople = peopleWaitingPerFloor.countPeople();
        elevator.peopleWaiting(peopleWaitingPerFloor.getLists());
        ShadowElevator shadowElevator = new ShadowElevator(ELEVATOR_CAPACITY, peopleWaitingPerFloor);

        printInitInfos(peopleWaitingPerFloor, totalNumberOfPeople);

        while (totalNumberOfPeople < MAX_PEOPLE_ARRIVING || !shadowElevator.isStopped()) {

            if (!events.containsKey(time)) {
                if (totalNumberOfPeople < MAX_PEOPLE_ARRIVING) {
                    totalNumberOfPeople += addNewPersonToWaitingLists(time, peopleWaitingPerFloor, elevator);
                    if (totalNumberOfPeople == MAX_PEOPLE_ARRIVING) {
                        shadowElevator.lastPersonArrived();
                        elevator.lastPersonArrived();
                    }
                }
                time = time.plusSeconds(3);
                continue;
            }

            Event nextEvent = events.get(time);
            events.remove(time);

            if (nextEvent.getName().equals(Event.ELEVATOR_STARTS)) {

                Event event = Event.fromElevatorStart(time, elevator, shadowElevator);
                LocalTime arrivalTime = event.getTimeOfArrivalFrom(time);
                events.put(arrivalTime, event);

            } else if (nextEvent.getName().equals(Event.ARRIVES_AT_FLOOR)) {

                Event event = Event.fromArrivesAtFloor(time, elevator, shadowElevator);
                LocalTime arrivalTime = event.getTimeOfArrivalFrom(time);
                events.put(arrivalTime, event);

            } else if (nextEvent.getName().equals(Event.DOOR_OPENING)) {

                Event event = Event.fromDoorOpening(time, elevator, shadowElevator);
                LocalTime arrivalTime = event.getTimeOfArrivalFrom(time);
                events.put(arrivalTime, event);

            } else if (nextEvent.getName().equals(Event.DOOR_CLOSING)) {

                Event event = Event.fromDoorClosing(time, shadowElevator);
                LocalTime arrivalTime = time.plus(event.getDuration());
                events.put(arrivalTime, event);

            } else if (nextEvent.getName().equals(Event.LOADING_FIRST_PERSON)) {

                Event event = Event.fromLoadingFirstPerson(time, shadowElevator, elevator, nextEvent);
                LocalTime arrivalTime = event.getTimeOfArrivalFrom(time);
                events.put(arrivalTime, event);

            } else if (nextEvent.getName().equals(Event.LOADING_NEXT_PERSON)) {

                Event event = Event.fromLoadingNextPerson(time, shadowElevator, elevator, nextEvent);
                LocalTime arrivalTime = event.getTimeOfArrivalFrom(time);
                events.put(arrivalTime, event);

            } else if (nextEvent.getName().equals(Event.UNLOADING_FIRST_PERSON)) {

                Event event = Event.fromUnloadingFirstPerson(time, elevator, shadowElevator, nextEvent);

                LocalTime arrivalTime = event.getTimeOfArrivalFrom(time);
                events.put(arrivalTime, event);

            } else if (nextEvent.getName().equals(Event.UNLOADING_NEXT_PERSON)) {

                Event event = Event.fromUnloadingNextPerson(time, elevator, shadowElevator, nextEvent);

                LocalTime arrivalTime = event.getTimeOfArrivalFrom(time);
                events.put(arrivalTime, event);

            } else if (nextEvent.getName().equals(Event.STAND_BY_AT_FLOOR)) {

                Event event = Event.fromStandByAtFloor(time, elevator, shadowElevator);

                LocalTime arrivalTime = event.getTimeOfArrivalFrom(time);
                events.put(arrivalTime, event);

            } else if (nextEvent.getName().equals(Event.STOPPING_AT_FLOOR)) {

                shadowElevator.stopping();
            }

            if (totalNumberOfPeople < MAX_PEOPLE_ARRIVING) {
                totalNumberOfPeople += addNewPersonToWaitingLists(time, peopleWaitingPerFloor, elevator);
                if (totalNumberOfPeople == MAX_PEOPLE_ARRIVING) {
                    shadowElevator.lastPersonArrived();
                    elevator.lastPersonArrived();
                }
            }
            time = time.plusSeconds(3);
        }
        peopleWaitingPerFloor.print();
        shadowElevator.printPeople();
        System.out.printf("[%s] Times up\n", time);
    }

    private static void printInitInfos(WaitingList peopleWaitingPerFloor, int totalNumberOfPeople) {
        double averageNumberOfPeoplePerMinute = 1d - Math.pow(1d - PROBABILITY_TO_SEE_A_NEW_PERSON_IN_LINE, 20d);
        System.out.printf("Average number of people in line per minute = %4.2f\n", averageNumberOfPeoplePerMinute);
        System.out.printf("Number of people waiting = %d\n", totalNumberOfPeople);
        peopleWaitingPerFloor.print();
    }

    private static int addNewPersonToWaitingLists(LocalTime time, WaitingList peopleWaitingPerFloor, Elevator elevator) {
        Optional<Map.Entry<Integer, Person>> newPersonWaiting = peopleWaitingPerFloor.addNewPeopleToLists();
        if (newPersonWaiting.isPresent()) {
            int floor = newPersonWaiting.orElseThrow().getKey();
            Person person = newPersonWaiting.orElseThrow().getValue();
            elevator.newPersonWaitingAtFloor(floor, person);
            System.out.printf("\n[%s] %s calls the elevator from floor %d to go to floor %d\n", time, person.getName(), floor, person.getDestinationFloor());
            System.out.printf("Waiting list is now:\n");
            peopleWaitingPerFloor.print();
            return 1;
        } else {
            return 0;
        }
    }
}
