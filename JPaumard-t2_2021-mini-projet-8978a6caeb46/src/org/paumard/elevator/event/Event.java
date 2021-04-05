package org.paumard.elevator.event;

import org.paumard.elevator.Building;
import org.paumard.elevator.Elevator;
import org.paumard.elevator.model.Person;
import org.paumard.elevator.system.ShadowElevator;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.paumard.elevator.Building.ELEVATOR_LOADING_CAPACITY;
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

    public static NavigableMap<Duration, Long> durations = new TreeMap<>();

    private String name;
    private int currentFloor;
    private List<Person> people;
    private int nextFloor;
    private Duration duration;

    public Event(String name, Duration duration) {
        this.name = name;
        this.duration = duration;
    }

    public Event(String name, Duration duration, int nextFloor) {
        this.name = name;
        this.duration = duration;
        this.nextFloor = nextFloor;
    }

    public Event(String name, Duration duration, List<Person> people) {
        this.name = name;
        this.duration = duration;
        this.people = people;
    }

    public Event(String name, Duration duration, int currentFloor, int nextFloor) {
        this.name = name;
        this.duration = duration;
        this.currentFloor = currentFloor;
        this.nextFloor = nextFloor;
    }

    public Event(String name) {
        this.name = name;
        this.duration = Duration.ofSeconds(0);
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

    private static DIRECTION computeDirection(int currentFloor, List<Integer> nextFloors) {
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

    public static Event fromElevatorStart(LocalTime time, Elevator elevator, ShadowElevator shadowElevator) {
        Event event;
        int currentFloor = 1;
        PRINTER.printf("\n[%s] Starting at floor %d\n", time, currentFloor);

        elevator.startsAtFloor(time, currentFloor);
        shadowElevator.startsAtFloor(currentFloor);

        List<Integer> nextFloors = chooseNextFloors(elevator);
        printElevatorGoingTo(time, currentFloor, nextFloors);

        DIRECTION direction = computeDirection(currentFloor, nextFloors);

        int nextFloor = direction == DIRECTION.STOP ? currentFloor : nextFloors.get(0);
        shadowElevator.setNextFloors(nextFloors);

        if (direction == DIRECTION.STOP) {
            if (shadowElevator.hasLastPersonArrived()) {
                shadowElevator.stopping();
                PRINTER.printf("\n[%s] Stopping at floor %d\n", time, currentFloor);
                return new StoppingAtFloor(currentFloor);
            } else {
                PRINTER.printf("\n[%s] Standby at floor %d\n", time, currentFloor);
                return new StandByAtFloor();
            }
        }

        PRINTER.printf("[%s] Going %s to floor %d from floor %d\n", time, direction, nextFloor, currentFloor);

        List<Person> nextPeopleToLoad = shadowElevator.getNextPeopleToLoad(nextFloors, currentFloor);
        if (!nextPeopleToLoad.isEmpty()) {

            event = new LoadingFirstPerson(nextPeopleToLoad);

        } else {

            Duration duration = computeDuration(currentFloor, nextFloor);
            PRINTER.printf("\n[%s] Going %s to floor %d, arrival in %ds\n", time, direction, nextFloor, duration.getSeconds());
            event = new ArriveAtFloor(duration, nextFloor);
        }
        return event;
    }

    private static List<Integer> chooseNextFloors(Elevator elevator) {
        List<Integer> nextFloors = elevator.chooseNextFloors();
        if (nextFloors.isEmpty()) {
            throw new IllegalStateException("The elevator did not chose any next floor");
        }
        if (nextFloors.size() > Building.MAX_DISPLAYED_FLOORS) {
            throw new IllegalStateException("The elevator returned too many next floors: " + nextFloors);
        }
        return nextFloors;
    }

    public static Event fromArrivesAtFloor(LocalTime time, Elevator elevator, ShadowElevator shadowElevator) {
        List<Integer> currentFloors = shadowElevator.getNextFloors();

        int currentFloor = currentFloors.get(0);
        elevator.arriveAtFloor(currentFloor);
        shadowElevator.moveTo(currentFloor);

        PRINTER.printf("\n[%s] Arrived at floor %d\n", time, currentFloor);

        return new DoorOpening();
    }

    public static Event fromDoorOpening(LocalTime time, Elevator elevator, ShadowElevator shadowElevator) {

        int currentFloor = shadowElevator.getCurrentFloor();

        PRINTER.printf("[%s] Door opened at floor %d\n", time, currentFloor);

        List<Person> nextPersonToUnload = shadowElevator.getNextPeopleToUnload(currentFloor);
        if (!nextPersonToUnload.isEmpty()) {

            return new UnloadingFirstPerson(nextPersonToUnload);

        } else {

            List<Integer> nextFloors = chooseNextFloors(elevator);
            if (nextFloors.isEmpty()) {
                throw new IllegalStateException("No next floors returned");
            }
            printElevatorGoingTo(time, currentFloor, nextFloors);

            DIRECTION direction = computeDirection(currentFloor, nextFloors);
            int nextFloor = direction == DIRECTION.STOP ? currentFloor : nextFloors.get(0);
            shadowElevator.setNextFloors(nextFloors);

            if (direction == DIRECTION.STOP) {
                if (shadowElevator.hasLastPersonArrived()) {
                    shadowElevator.stopping();
                    PRINTER.printf("\n[%s] Stopping at floor %d\n", time, currentFloor);
                    return new StoppingAtFloor(currentFloor);
                } else {
                    PRINTER.printf("\n[%s] Standby at floor %d\n", time, currentFloor);
                    return new StandByAtFloor();
                }
            }

            PRINTER.printf("[%s] Going %s to floor %d from floor %d\n", time, direction, nextFloor, currentFloor);

            List<Person> nextPersonToLoad = shadowElevator.getNextPeopleToLoad(nextFloors, currentFloor);
            if (!nextPersonToLoad.isEmpty()) {

                return new LoadingFirstPerson(nextPersonToLoad);


            } else {

                Duration duration = computeDuration(currentFloor, nextFloor);
                PRINTER.printf("\n[%s] Going %s to floor %d, arrival in %ds\n", time, direction, nextFloor, duration.getSeconds());

                return new DoorClosing(currentFloor, nextFloor);
            }
        }
    }

    public static Event fromDoorClosing(LocalTime time, ShadowElevator shadowElevator) {
        int currentFloor = shadowElevator.getCurrentFloor();
        List<Integer> nextFloors = shadowElevator.getNextFloors();
        int nextFloor = nextFloors.get(0);

        PRINTER.printf("[%s] Door closed at floor %d, going to floor %d\n",
                time, currentFloor, nextFloor);

        Duration duration = computeDuration(currentFloor, nextFloor);
        return new ArriveAtFloor(duration, nextFloor);
    }

    public static Event fromStandByAtFloor(LocalTime time, Elevator elevator, ShadowElevator shadowElevator) {

        int currentFloor = shadowElevator.getCurrentFloor();
        elevator.standByAtFloor(currentFloor);

        List<Integer> nextFloors = chooseNextFloors(elevator);
        printElevatorGoingTo(time, currentFloor, nextFloors);

        DIRECTION direction = computeDirection(currentFloor, nextFloors);
        shadowElevator.setNextFloors(nextFloors);

        if (shadowElevator.isAnyoneWaitingAtCurrentFloor()) {

            List<Person> nextPeopleToLoad = shadowElevator.getNextPeopleToLoad(nextFloors, currentFloor);

            if (nextPeopleToLoad.isEmpty()) {
                int nextFloor = nextFloors.get(0);
                if (nextFloor != currentFloor) {
                    return new DoorClosing(currentFloor, nextFloor);
                }
            } else {
                if (nextPeopleToLoad.size() > ELEVATOR_LOADING_CAPACITY) {
                    nextPeopleToLoad = nextPeopleToLoad.subList(0, ELEVATOR_LOADING_CAPACITY);
                }

                return new LoadingFirstPerson(nextPeopleToLoad);
            }

        } else {
            int nextFloor = nextFloors.get(0);
            if (nextFloor != currentFloor) {
                return new DoorClosing(currentFloor, nextFloor);
            }
        }

        if (shadowElevator.hasLastPersonArrived()) {
            PRINTER.printf("\n[%s] Stopping at floor %d\n", time, currentFloor);
            return new StoppingAtFloor(currentFloor);
        } else {
            return new StandByAtFloor();
        }
    }

    private static void printElevatorGoingTo(LocalTime time, int currentFloor, List<Integer> nextFloors) {
        if (nextFloors.get(0) != currentFloor) {
            PRINTER.printf("[%s] Elevator decides to go to floor %s\n", time, nextFloors.toString());
        }
    }

    public static Event fromLoadingFirstPerson(LocalTime time, ShadowElevator shadowElevator, Elevator elevator, Event nextEvent) {

        return fromLoadingPerson(time, shadowElevator, elevator, nextEvent);
    }

    public static Event fromLoadingNextPerson(LocalTime time, ShadowElevator shadowElevator, Elevator elevator, Event nextEvent) {

        return fromLoadingPerson(time, shadowElevator, elevator, nextEvent);
    }

    public static Event fromUnloadingFirstPerson(LocalTime time, Elevator elevator, ShadowElevator shadowElevator, Event nextEvent) {

        return fromUnloadingPerson(time, elevator, shadowElevator, nextEvent);
    }

    public static Event fromUnloadingNextPerson(LocalTime time, Elevator elevator, ShadowElevator shadowElevator, Event nextEvent) {

        return fromUnloadingPerson(time, elevator, shadowElevator, nextEvent);
    }

    private static Event fromLoadingPerson(LocalTime time, ShadowElevator shadowElevator, Elevator elevator, Event nextEvent) {
        int currentFloor = shadowElevator.getCurrentFloor();
        List<Person> people = nextEvent.getPeople();
        shadowElevator.loadPeople(people);
        elevator.loadPeople(people);
        List<Integer> nextFloors = shadowElevator.getNextFloors();

        for (Person person : people) {
            PRINTER.printf("[%s] Person loaded [%s] at floor %d\n", time, person.toString(), currentFloor);
        }

        List<Person> nextPeopleToLoad = shadowElevator.getNextPeopleToLoad(nextFloors, currentFloor);
        if (!nextPeopleToLoad.isEmpty()) {

            return new LoadingNextPerson(nextPeopleToLoad);

        } else {

            return new DoorClosing(currentFloor, nextFloors.get(0));
        }
    }

    private static Event fromUnloadingPerson(LocalTime time, Elevator elevator, ShadowElevator shadowElevator, Event nextEvent) {
        int currentFloor = shadowElevator.getCurrentFloor();
        List<Person> people = nextEvent.getPeople();
        shadowElevator.unload(people);
        elevator.unload(people);

        for (Person person : people) {
            Duration waitDuration = Duration.between(person.getArrivalTime(), time);
            durations.merge(waitDuration, 1L, Long::sum);

            PRINTER.printf("[%s] Person unloaded [%s] at floor %d\n", time, person.toString(), currentFloor);
        }

        List<Person> nextPeopleToUnload = shadowElevator.getNextPeopleToUnload(currentFloor);
        if (!nextPeopleToUnload.isEmpty()) {

            return new UnloadingNextPerson(nextPeopleToUnload);

        } else {

            List<Integer> nextFloors = chooseNextFloors(elevator);
            printElevatorGoingTo(time, currentFloor, nextFloors);

            DIRECTION direction = computeDirection(currentFloor, nextFloors);
            int nextFloor = direction == DIRECTION.STOP ? currentFloor : nextFloors.get(0);
            shadowElevator.setNextFloors(nextFloors);

            if (direction == DIRECTION.STOP) {
                if (shadowElevator.hasLastPersonArrived()) {
                    shadowElevator.stopping();
                    PRINTER.printf("\n[%s] Stopping at floor %d\n", time, currentFloor);
                    return new StoppingAtFloor(currentFloor);
                } else {
                    PRINTER.printf("\n[%s] Standing by at floor %d\n", time, currentFloor);
                    return new StandByAtFloor();
                }
            }

            List<Person> nextPeopleToLoad = shadowElevator.getNextPeopleToLoad(nextFloors, currentFloor);
            if (!nextPeopleToLoad.isEmpty()) {

                return new LoadingFirstPerson(nextPeopleToLoad);

            } else {

                Duration duration = computeDuration(currentFloor, nextFloor);
                PRINTER.printf("[%s] Going %s to floor %d, arrival in %ds\n", time, direction, nextFloor, duration.getSeconds());

                return new ArriveAtFloor(duration, nextFloor);
            }
        }
    }

    public static class ArriveAtFloor extends Event {

        public ArriveAtFloor(Duration duration, int nextFloor) {
            super(Event.ARRIVES_AT_FLOOR, duration, nextFloor);
        }

    }

    public static class DoorOpening extends Event {

        public DoorOpening() {
            super(Event.DOOR_OPENING, Duration.ofSeconds(3));
        }

    }

    public static class LoadingFirstPerson extends Event {

        public LoadingFirstPerson(List<Person> people) {
            super(Event.LOADING_FIRST_PERSON, Duration.ofSeconds(9), people);
        }

    }

    public static class UnloadingFirstPerson extends Event {

        public UnloadingFirstPerson(List<Person> people) {
            super(UNLOADING_FIRST_PERSON, Duration.ofSeconds(9), people);
        }
    }

    public static class DoorClosing extends Event {
        public DoorClosing(int currentFloor, int nextFloor) {
            super(DOOR_CLOSING, Duration.ofSeconds(3), currentFloor, nextFloor);
        }
    }

    public static class LoadingNextPerson extends Event {
        public LoadingNextPerson(List<Person> people) {
            super(LOADING_NEXT_PERSON, Duration.ofSeconds(6), people);
        }

    }

    public static class UnloadingNextPerson extends Event {
        public UnloadingNextPerson(List<Person> people) {
            super(UNLOADING_NEXT_PERSON, Duration.ofSeconds(6), people);
        }
    }

    public static class StandByAtFloor extends Event {

        public StandByAtFloor() {
            super(STAND_BY_AT_FLOOR, Duration.ofSeconds(3));
        }
    }

    private static class StoppingAtFloor extends Event {
        public StoppingAtFloor(int currentFloor) {
            super(STOPPING_AT_FLOOR, Duration.ofSeconds(3), currentFloor);
        }
    }
}
