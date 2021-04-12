package org.paumard.elevator.event;

import org.paumard.elevator.Elevator;
import org.paumard.elevator.model.Person;
import org.paumard.elevator.system.Elevators;
import org.paumard.elevator.system.ShadowElevator;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.paumard.elevator.Building.PRINTER;

public class Event {

    public static final String ARRIVES_AT_FLOOR = "Arrives at floor";
    public static final String ELEVATOR_STARTS = "Elevator starts";
    public static final String DOOR_OPENING = "Door opening";
    public static final String DOOR_CLOSING = "Door closing";
    public static final String LOADING_FIRST_PERSON = "Loading first person";
    public static final String LOADING_NEXT_PERSON = "Loading next person";
    public static final String UNLOADING_FIRST_PERSON = "Unloading first person";
    public static final String UNLOADING_NEXT_PERSON = "Unloading next person";
    public static final String STAND_BY_AT_FLOOR = "Stand by at floor";
    public static final String STOPPING_AT_FLOOR = "Stopping at floor";
    public static final String FIRST_LOADING_ATTEMPT = "First loading attempt";
    public static final String NEXT_LOADING_ATTEMPT = "Next loading attempt";

    public static NavigableMap<Duration, Long> durations = new TreeMap<>();

    private Elevator elevator;
    private String name;
    private int currentFloor;
    private List<Person> people;
    int nextFloor;
    private List<Integer> nextFloors;
    private Duration duration;

    public Event(Elevator elevator, String name, Duration duration) {
        this.elevator = elevator;
        this.name = name;
        this.duration = duration;
    }

    public Event(Elevator elevator, String name, Duration duration, int nextFloor) {
        this.elevator = elevator;
        this.name = name;
        this.duration = duration;
        this.nextFloor = nextFloor;
    }

    public Event(Elevator elevator, String name, Duration duration, List<Person> people) {
        this.elevator = elevator;
        this.name = name;
        this.duration = duration;
        this.people = people;
    }

    public Event(Elevator elevator, String name, Duration duration, int currentFloor, int nextFloor) {
        this.elevator = elevator;
        this.name = name;
        this.duration = duration;
        this.currentFloor = currentFloor;
        this.nextFloor = nextFloor;
    }

    public Event(Elevator elevator, String name, int currentFloor, List<Integer> nextFloors) {
        this.elevator = elevator;
        this.name = name;
        this.currentFloor = currentFloor;
        this.nextFloors = nextFloors;
    }

    public Event(Elevator elevator, String name) {
        this.elevator = elevator;
        this.name = name;
        this.duration = Duration.ofSeconds(0);
    }

    public static List<Event> createStartEventFor(Elevators elevators) {
        return elevators.getElevators().stream()
                .map(elevator -> new Event(elevator, ELEVATOR_STARTS))
                .collect(Collectors.toList());
    }

    public LocalTime getTimeOfArrivalFrom(LocalTime time) {
        return time.plus(this.getDuration());
    }

    public String getName() {
        return name;
    }

    public List<Person> getPeople() {
        return people;
    }

    public Duration getDuration() {
        return duration;
    }

    public static Duration computeDuration(int currentFloor, int nextFloor) {

        if (currentFloor == nextFloor) {
            return Duration.ofSeconds(0);
        } else if (Math.abs(currentFloor - nextFloor) == 1) {
            return Duration.ofSeconds(18);
        } else if (Math.abs(currentFloor - nextFloor) <= 3) {
            return Duration.ofSeconds(24 + Math.abs(currentFloor - nextFloor - 1) * 6);
        } else {
            return Duration.ofSeconds(36 + Math.abs(currentFloor - nextFloor - 3) * 3);
        }
    }

    public static DIRECTION computeDirection(int currentFloor, List<Integer> nextFloors) {
        DIRECTION direction = DIRECTION.STOP;
        if (nextFloors.isEmpty() || nextFloors.get(0) == currentFloor) {
            return DIRECTION.STOP;
        } else if (nextFloors.get(0) > currentFloor) {
            direction = DIRECTION.UP;
        } else if (nextFloors.get(0) < currentFloor) {
            direction = DIRECTION.DOWN;
        }
        return direction;
    }


    public static Event fromElevatorStartAtFloor(LocalTime time, Elevator elevator, ShadowElevator shadowElevator, int startingFloor) {

        PRINTER.printf("\n[%s] Elevator [%s] starting at floor %d\n", time, elevator.getId(), startingFloor);

        elevator.startsAtFloor(time, startingFloor);
        shadowElevator.startsAtFloor(startingFloor);

        List<Integer> nextFloors = elevator.chooseNextFloors();
        printElevatorGoingTo(time, elevator, startingFloor, nextFloors);

        DIRECTION direction = computeDirection(startingFloor, nextFloors);

        int nextFloor = direction == DIRECTION.STOP ? startingFloor : nextFloors.get(0);
        shadowElevator.setNextFloors(nextFloors);

        if (direction == DIRECTION.STOP) {
            if (shadowElevator.hasLastPersonArrived()) {
                shadowElevator.stopping();
                PRINTER.printf("\n[%s] Elevator [%s] stopping at floor %d\n", time, elevator.getId(), startingFloor);
                return new StoppingAtFloor(elevator, startingFloor);
            } else {
                PRINTER.printf("\n[%s] Elevator [%s] standby at floor %d\n", time, elevator.getId(), startingFloor);
                return new StandByAtFloor(elevator);
            }
        }

        PRINTER.printf("[%s] Elevator [%s] going %s to floor %d from floor %d\n", time, elevator.getId(), direction, nextFloor, startingFloor);

        return new AttemptToLoadFirstPerson(elevator, startingFloor, nextFloors);
    }

    public static Event fromElevatorStart(LocalTime time, Elevator elevator, ShadowElevator shadowElevator) {
        return fromElevatorStartAtFloor(time, elevator, shadowElevator, 1);
    }

    public static Event fromArrivesAtFloor(LocalTime time, Elevator elevator, ShadowElevator shadowElevator) {
        List<Integer> currentFloors = shadowElevator.getNextFloors();

        int currentFloor = currentFloors.get(0);
        elevator.arriveAtFloor(currentFloor);
        shadowElevator.moveTo(currentFloor);

        PRINTER.printf("\n[%s] Elevator [%s] arrived at floor %d\n", time, elevator.getId(), currentFloor);

        return new DoorOpening(elevator);
    }

    public static Event fromDoorOpening(LocalTime time, Elevator elevator, ShadowElevator shadowElevator) {

        int currentFloor = shadowElevator.getCurrentFloor();

        PRINTER.printf("[%s] Elevator [%s] door opened at floor %d\n", time, elevator.getId(), currentFloor);

        List<Person> nextPersonToUnload = shadowElevator.getNextPeopleToUnload(currentFloor);
        if (!nextPersonToUnload.isEmpty()) {

            return new UnloadingFirstPerson(elevator, nextPersonToUnload);

        } else {

            List<Integer> nextFloors = elevator.chooseNextFloors();
            printElevatorGoingTo(time, elevator, currentFloor, nextFloors);

            DIRECTION direction = computeDirection(currentFloor, nextFloors);
            int nextFloor = direction == DIRECTION.STOP ? currentFloor : nextFloors.get(0);
            shadowElevator.setNextFloors(nextFloors);

            if (direction == DIRECTION.STOP) {
                if (shadowElevator.hasLastPersonArrived()) {
                    shadowElevator.stopping();
                    PRINTER.printf("\n[%s] Elevator [%s] stopping at floor %d\n", time, elevator.getId(), currentFloor);
                    return new StoppingAtFloor(elevator, currentFloor);
                } else {
                    PRINTER.printf("\n[%s] Elevator [%s] standby at floor %d\n", time, elevator.getId(), currentFloor);
                    return new StandByAtFloor(elevator);
                }
            }

            PRINTER.printf("[%s] Elevator [%s] going %s to floor %d from floor %d\n", time, elevator.getId(), direction, nextFloor, currentFloor);

            return new AttemptToLoadFirstPerson(elevator, currentFloor, nextFloors);
        }
    }

    public static Event fromDoorClosing(LocalTime time, Elevator elevator, ShadowElevator shadowElevator) {
        int currentFloor = shadowElevator.getCurrentFloor();
        List<Integer> nextFloors = shadowElevator.getNextFloors();
        int nextFloor = nextFloors.get(0);

        PRINTER.printf("[%s] Elevator [%s] door closed at floor %d, going to floor %d\n", time, elevator.getId(), currentFloor, nextFloor);

        Duration duration = computeDuration(currentFloor, nextFloor);
        return new ArriveAtFloor(elevator, duration, nextFloor);
    }

    public static Event fromStandByAtFloor(LocalTime time, Elevator elevator, ShadowElevator shadowElevator) {

        int currentFloor = shadowElevator.getCurrentFloor();
        elevator.standByAtFloor(currentFloor);

        List<Integer> nextFloors = elevator.chooseNextFloors();
        printElevatorGoingTo(time, elevator, currentFloor, nextFloors);

        shadowElevator.setNextFloors(nextFloors);

        if (shadowElevator.isAnyoneWaitingAtCurrentFloor()) {

            boolean hasNextPeopleToLoad = shadowElevator.hasNextPeopleToLoad(nextFloors, currentFloor);

            if (hasNextPeopleToLoad) {
                return new AttemptToLoadFirstPerson(elevator, currentFloor, nextFloors);
            } else {
                int nextFloor = nextFloors.get(0);
                if (nextFloor != currentFloor) {
                    return new DoorClosing(elevator, currentFloor, nextFloor);
                }
            }

        } else {
            int nextFloor = nextFloors.get(0);
            if (nextFloor != currentFloor) {
                return new DoorClosing(elevator, currentFloor, nextFloor);
            }
        }

        if (shadowElevator.hasLastPersonArrived()) {
            PRINTER.printf("\n[%s] Elevator [%s] stopping at floor %d\n", time, elevator.getId(), currentFloor);
            return new StoppingAtFloor(elevator, currentFloor);
        } else {
            return new StandByAtFloor(elevator);
        }
    }

    private static void printElevatorGoingTo(LocalTime time, Elevator elevator, int currentFloor, List<Integer> nextFloors) {
        if (nextFloors.get(0) != currentFloor) {
            PRINTER.printf("[%s] Elevator [%s] decides to go to floor %s\n", time, elevator.getId(), nextFloors.toString());
        }
    }

    public static Event fromLoadingFirstPerson(LocalTime time, ShadowElevator shadowElevator, Elevator elevator, Event nextEvent) {

        int currentFloor = shadowElevator.getCurrentFloor();
        List<Person> people = nextEvent.getPeople();
        shadowElevator.loadPeople(people);
        elevator.loadPeople(people);
        List<Integer> nextFloors = shadowElevator.getNextFloors();

        for (Person person : people) {
            PRINTER.printf("[%s] Elevator [%s] person loaded [%s] at floor %d\n", time, elevator.getId(), person.toString(), currentFloor);
        }

        boolean hasNextPeopleToLoad = shadowElevator.hasNextPeopleToLoad(nextFloors, currentFloor);
        if (hasNextPeopleToLoad) {

            return new AttemptToLoadFirstPerson(elevator, currentFloor, nextFloors);

        } else {

            return new DoorClosing(elevator, currentFloor, nextFloors.get(0));
        }
    }

    public static Event fromLoadingNextPerson(LocalTime time, ShadowElevator shadowElevator, Elevator elevator, Event nextEvent) {

        int currentFloor = shadowElevator.getCurrentFloor();
        List<Integer> nextFloors = shadowElevator.getNextFloors();

        return new AttemptToLoadNextPerson(elevator, currentFloor, nextFloors);
    }

    public static Event fromUnloadingFirstPerson(LocalTime time, ShadowElevator shadowElevator, Elevator elevator, Event nextEvent) {

        return fromUnloadingPerson(time, elevator, shadowElevator, nextEvent);
    }

    public static Event fromUnloadingNextPerson(LocalTime time, ShadowElevator shadowElevator, Elevator elevator, Event nextEvent) {

        return fromUnloadingPerson(time, elevator, shadowElevator, nextEvent);
    }

    private static Event fromUnloadingPerson(LocalTime time, Elevator elevator, ShadowElevator shadowElevator, Event nextEvent) {
        int currentFloor = shadowElevator.getCurrentFloor();
        List<Person> people = nextEvent.getPeople();
        shadowElevator.unload(people);
        elevator.unload(people);

        for (Person person : people) {
            Duration travelDuration = Duration.between(person.getArrivalTime(), time);
            durations.merge(travelDuration, 1L, Long::sum);

            PRINTER.printf("[%s] Elevator [%s] person unloaded [%s] at floor %d\n", time, elevator.getId(), person.toString(), currentFloor);
        }

        List<Person> nextPeopleToUnload = shadowElevator.getNextPeopleToUnload(currentFloor);
        if (!nextPeopleToUnload.isEmpty()) {

            return new UnloadingNextPerson(elevator, nextPeopleToUnload);

        } else {

            List<Integer> nextFloors = elevator.chooseNextFloors();
            printElevatorGoingTo(time, elevator, currentFloor, nextFloors);

            DIRECTION direction = computeDirection(currentFloor, nextFloors);
            int nextFloor = direction == DIRECTION.STOP ? currentFloor : nextFloors.get(0);
            shadowElevator.setNextFloors(nextFloors);

            if (direction == DIRECTION.STOP) {
                if (shadowElevator.hasLastPersonArrived()) {
                    shadowElevator.stopping();
                    PRINTER.printf("\n[%s] Elevator [%s] stopping at floor %d\n", time, elevator.getId(), currentFloor);
                    return new StoppingAtFloor(elevator, currentFloor);
                } else {
                    PRINTER.printf("\n[%s] Elevator [%s] standing by at floor %d\n", time, elevator.getId(), currentFloor);
                    return new StandByAtFloor(elevator);
                }
            }

            return new AttemptToLoadFirstPerson(elevator, currentFloor, nextFloors);
        }
    }

    public Elevator getElevator() {
        return this.elevator;
    }

    public int getCurrentFloor() {
        return this.currentFloor;
    }

    public List<Integer> getNextFloors() {
        return this.nextFloors;
    }

    public static class ArriveAtFloor extends Event {

        public ArriveAtFloor(Elevator elevator, Duration duration, int nextFloor) {
            super(elevator, Event.ARRIVES_AT_FLOOR, duration, nextFloor);
        }

    }

    public static class DoorOpening extends Event {

        public DoorOpening(Elevator elevator) {
            super(elevator, Event.DOOR_OPENING, Duration.ofSeconds(3));
        }

    }

    public static class LoadingFirstPerson extends Event {

        public LoadingFirstPerson(Elevator elevator, List<Person> people) {
            super(elevator, Event.LOADING_FIRST_PERSON, Duration.ofSeconds(9), people);
        }

    }

    public static class UnloadingFirstPerson extends Event {

        public UnloadingFirstPerson(Elevator elevator, List<Person> people) {
            super(elevator, UNLOADING_FIRST_PERSON, Duration.ofSeconds(9), people);
        }
    }

    public static class DoorClosing extends Event {
        public DoorClosing(Elevator elevator, int currentFloor, int nextFloor) {
            super(elevator, DOOR_CLOSING, Duration.ofSeconds(3), currentFloor, nextFloor);
        }
    }

    public static class LoadingNextPerson extends Event {
        public LoadingNextPerson(Elevator elevator, List<Person> people) {
            super(elevator, LOADING_NEXT_PERSON, Duration.ofSeconds(6), people);
        }

    }

    public static class UnloadingNextPerson extends Event {
        public UnloadingNextPerson(Elevator elevator, List<Person> people) {
            super(elevator, UNLOADING_NEXT_PERSON, Duration.ofSeconds(6), people);
        }
    }

    public static class AttemptToLoadFirstPerson extends Event {

        private List<Person> peopleToLoad = new ArrayList<>();

        public AttemptToLoadFirstPerson(Elevator elevator, int currentFloor, List<Integer> nextFloors) {
            super(elevator, FIRST_LOADING_ATTEMPT, currentFloor, nextFloors);
        }

        public void addPerson(Person person) {
            this.peopleToLoad.add(person);
        }

        public List<Person> getPeopleToLoad() {
            return this.peopleToLoad;
        }
    }

    public static class AttemptToLoadNextPerson extends Event {

        private List<Person> peopleToLoad = new ArrayList<>();

        public AttemptToLoadNextPerson(Elevator elevator, int currentFloor, List<Integer> nextFloors) {
            super(elevator, NEXT_LOADING_ATTEMPT, currentFloor, nextFloors);
        }

        public void addPerson(Person person) {
            this.peopleToLoad.add(person);
        }

        public List<Person> getPeopleToLoad() {
            return this.peopleToLoad;
        }
    }

    public static class StandByAtFloor extends Event {

        public StandByAtFloor(Elevator elevator) {
            super(elevator, STAND_BY_AT_FLOOR, Duration.ofSeconds(3));
        }
    }

    private static class StoppingAtFloor extends Event {
        public StoppingAtFloor(Elevator elevator, int currentFloor) {
            super(elevator, STOPPING_AT_FLOOR, Duration.ofSeconds(3), currentFloor);
        }
    }
}
