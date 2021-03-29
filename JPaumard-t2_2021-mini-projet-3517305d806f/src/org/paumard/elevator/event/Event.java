package org.paumard.elevator.event;

import org.paumard.elevator.Building;
import org.paumard.elevator.Elevator;
import org.paumard.elevator.system.ShadowElevator;
import org.paumard.elevator.model.Direction;
import org.paumard.elevator.model.Person;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;

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

    private String name;
    private Person person;
    private Duration duration;

    public Event(String name, Duration duration) {
        this.name = name;
        this.duration = duration;
    }

    public Event(String name, Duration duration, Person person) {
        this.name = name;
        this.duration = duration;
        this.person = person;
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

    public Person getPerson() {
        return person;
    }

    public Duration getDuration() {
        return duration;
    }

    public static Duration computeDuration(int currentFloor, int nextFloor) {

        if (currentFloor == nextFloor) {
            return Duration.ofSeconds(0);
        } else if (currentFloor == nextFloor + 1 || currentFloor == nextFloor - 1) {
            return Duration.ofSeconds(18);
        } else {
            return Duration.ofSeconds(24 + Math.abs(currentFloor - nextFloor - 1) * 6L);
        }
    }

    private static Direction computeDirection(int currentFloor, int nextFloor) {
        Direction direction;
        if (nextFloor > currentFloor) {
            direction = Direction.UP;
        } else if (nextFloor < currentFloor) {
            direction = Direction.DOWN;
        } else {
            direction = Direction.STOP;
        }
        return direction;
    }

    public static Event fromElevatorStart(LocalTime time, Elevator elevator, ShadowElevator shadowElevator) {
        Event event;
        int currentFloor = 1;
        System.out.printf("\n[%s] Starting at floor %d\n", time, currentFloor);

        elevator.startsAtFloor(time, currentFloor);
        shadowElevator.startsAtFloor(currentFloor);

        int nextFloor = readNextFloorFrom(elevator);
        printElevatorGoingTo(time, currentFloor, nextFloor);

        Direction direction = computeDirection(currentFloor, nextFloor);
        shadowElevator.setNextFloor(nextFloor);

        if (direction == Direction.STOP) {
            if (shadowElevator.hasLastPersonArrived()) {
                shadowElevator.stopping();
                System.out.printf("\n[%s] Stopping at floor %d\n", time, currentFloor);
                return new StoppingAtFloor();
            } else {
                System.out.printf("\n[%s] Standby at floor %d\n", time, currentFloor);
                return new StandByAtFloor();
            }
        }

        System.out.printf("[%s] Going %s to floor %d from floor %d\n", time, direction, nextFloor, currentFloor);

        Optional<Person> nextPersonToLoad = shadowElevator.getNextPersonToLoad(nextFloor, currentFloor);
        if (nextPersonToLoad.isPresent()) {

            Person person = nextPersonToLoad.orElseThrow();
            event = new LoadingFirstPerson(person);

        } else {

            Duration duration = computeDuration(currentFloor, nextFloor);
            System.out.printf("\n[%s] Going %s to floor %d, arrival in %ds\n", time, direction, nextFloor, duration.getSeconds());
            event = new ArriveAtFloor(duration);
        }
        return event;
    }

    private static int readNextFloorFrom(Elevator elevator) {
        int nextFloor = elevator.chooseNextFloor();
        if (nextFloor < 1 || nextFloor > Building.MAX_FLOOR) {
            throw new IllegalStateException("Elevator returned floor " + nextFloor);
        }
        return nextFloor;
    }

    public static Event fromArrivesAtFloor(LocalTime time, Elevator elevator, ShadowElevator shadowElevator) {
        int currentFloor = shadowElevator.getNextFloor();
        elevator.arriveAtFloor(currentFloor);
        shadowElevator.moveTo(currentFloor);

        System.out.printf("\n[%s] Arrived at floor %d\n", time, currentFloor);

        return new DoorOpening();
    }

    public static Event fromDoorOpening(LocalTime time, Elevator elevator, ShadowElevator shadowElevator) {

        int currentFloor = shadowElevator.getCurrentFloor();

        System.out.printf("[%s] Door opened at floor %d\n", time, currentFloor);

        Optional<Person> nextPersonToUnload = shadowElevator.getNextPersonToUnload(currentFloor);
        if (nextPersonToUnload.isPresent()) {

            Person personToUnload = nextPersonToUnload.orElseThrow();
            return new UnloadingFirstPerson(personToUnload);

        } else {

            int nextFloor = readNextFloorFrom(elevator);
            printElevatorGoingTo(time, currentFloor, nextFloor);

            Direction direction = computeDirection(currentFloor, nextFloor);
            shadowElevator.setNextFloor(nextFloor);

            if (direction == Direction.STOP) {
                if (shadowElevator.hasLastPersonArrived()) {
                    shadowElevator.stopping();
                    System.out.printf("\n[%s] Stopping at floor %d\n", time, currentFloor);
                    return new StoppingAtFloor();
                } else {
                    System.out.printf("\n[%s] Standby at floor %d\n", time, currentFloor);
                    return new StandByAtFloor();
                }
            }

            System.out.printf("[%s] Going %s to floor %d from floor %d\n", time, direction, nextFloor, currentFloor);

            Optional<Person> nextPersonToLoad = shadowElevator.getNextPersonToLoad(nextFloor, currentFloor);
            if (nextPersonToLoad.isPresent()) {

                Person personToLoad = nextPersonToLoad.orElseThrow();
                return new LoadingFirstPerson(personToLoad);


            } else {

                Duration duration = computeDuration(currentFloor, nextFloor);
                System.out.printf("\n[%s] Going %s to floor %d, arrival in %ds\n", time, direction, nextFloor, duration.getSeconds());

                return new ArriveAtFloor(duration);
            }
        }
    }

    public static Event fromDoorClosing(LocalTime time, ShadowElevator shadowElevator) {
        int currentFloor = shadowElevator.getCurrentFloor();
        int nextFloor = shadowElevator.getNextFloor();

        System.out.printf("[%s] Door closed at floor %d, going to floor %d\n", time, currentFloor, nextFloor);

        Duration duration = computeDuration(currentFloor, nextFloor);
        return new ArriveAtFloor(duration);
    }

    public static Event fromStandByAtFloor(LocalTime time, Elevator elevator, ShadowElevator shadowElevator) {

        int currentFloor = shadowElevator.getCurrentFloor();
        if (shadowElevator.isAnyoneWaitingAtCurrentFloor()) {
            Person nextPersonToLoad = shadowElevator.getNextPersonToLoadFromCurrentFloor().orElseThrow();
            int nextFloor = nextPersonToLoad.getDestinationFloor();
            shadowElevator.setNextFloor(nextFloor);
            return new LoadingFirstPerson(nextPersonToLoad);

        } else if (shadowElevator.isAnyoneWaitingAtOtherFloor()) {
            int nextFloor = readNextFloorFrom(elevator);
            printElevatorGoingTo(time, currentFloor, nextFloor);
            shadowElevator.setNextFloor(nextFloor);
            if (nextFloor == shadowElevator.getCurrentFloor()) {
                Optional<Person> nextPersonToLoad = shadowElevator.getNextPersonToLoad(nextFloor, currentFloor);
                if (nextPersonToLoad.isPresent()) {
                    Person personToLoad = nextPersonToLoad.orElseThrow();
                    return new LoadingFirstPerson(personToLoad);
                }
            } else {
                return new DoorClosing();
            }
        }
        if (shadowElevator.hasLastPersonArrived()) {
            System.out.printf("\n[%s] Stopping at floor %d\n", time, currentFloor);
            return new StoppingAtFloor();
        } else {
            return new StandByAtFloor();
        }
    }

    private static void printElevatorGoingTo(LocalTime time, int currentFloor, int nextFloor) {
        if (nextFloor == currentFloor) {
            System.out.printf("[%s] Elevator decides to stay at floor %d\n", time, currentFloor);
        } else {
            System.out.printf("[%s] Elevator decides to go to floor %d\n", time, nextFloor);
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
        Person person = nextEvent.getPerson();
        shadowElevator.loadPerson(person);
        elevator.loadPerson(person);
        int nextFloor = shadowElevator.getNextFloor();

        System.out.printf("[%s] Person loaded [%s] at floor %d\n", time, person.toString(), currentFloor);

        Optional<Person> nextPersonToLoad = shadowElevator.getNextPersonToLoad(nextFloor, currentFloor);
        if (nextPersonToLoad.isPresent()) {

            Person personToLoad = nextPersonToLoad.orElseThrow();
            return new LoadingNextPerson(personToLoad);

        } else {

            return new DoorClosing();
        }
    }

    private static Event fromUnloadingPerson(LocalTime time, Elevator elevator, ShadowElevator shadowElevator, Event nextEvent) {
        int currentFloor = shadowElevator.getCurrentFloor();
        Person person = nextEvent.getPerson();
        shadowElevator.unload(person);
        elevator.unloadPerson(person);

        System.out.printf("[%s] Person unloaded [%s] at floor %d\n", time, person.toString(), currentFloor);

        Optional<Person> nextPersonToUnload = shadowElevator.getNextPersonToUnload(currentFloor);
        if (nextPersonToUnload.isPresent()) {

            Person personToUnload = nextPersonToUnload.orElseThrow();
            return new UnloadingNextPerson(personToUnload);

        } else {

            int nextFloor = readNextFloorFrom(elevator);
            printElevatorGoingTo(time, currentFloor, nextFloor);

            Direction direction = computeDirection(currentFloor, nextFloor);
            shadowElevator.setNextFloor(nextFloor);

            if (direction == Direction.STOP) {
                if (shadowElevator.hasLastPersonArrived()) {
                    shadowElevator.stopping();
                    System.out.printf("\n[%s] Stopping at floor %d\n", time, currentFloor);
                    return new StoppingAtFloor();
                } else {
                    System.out.printf("\n[%s] Standing by at floor %d\n", time, currentFloor);
                    return new StandByAtFloor();
                }
            }

            Optional<Person> nextPersonToLoad = shadowElevator.getNextPersonToLoad(nextFloor, currentFloor);
            if (nextPersonToLoad.isPresent()) {

                Person personToLoad = nextPersonToLoad.orElseThrow();
                return new LoadingFirstPerson(personToLoad);

            } else {

                Duration duration = computeDuration(currentFloor, nextFloor);
                System.out.printf("[%s] Going %s to floor %d, arrival in %ds\n", time, direction, nextFloor, duration.getSeconds());

                return new ArriveAtFloor(duration);
            }
        }
    }

    public static class ArriveAtFloor extends Event {

        public ArriveAtFloor(Duration duration) {
            super(Event.ARRIVES_AT_FLOOR, duration);
        }

    }

    public static class DoorOpening extends Event {

        public DoorOpening() {
            super(Event.DOOR_OPENING, Duration.ofSeconds(3));
        }

    }

    public static class LoadingFirstPerson extends Event {

        public LoadingFirstPerson(Person person) {
            super(Event.LOADING_FIRST_PERSON, Duration.ofSeconds(9), person);
        }

    }

    public static class UnloadingFirstPerson extends Event {

        public UnloadingFirstPerson(Person person) {
            super(UNLOADING_FIRST_PERSON, Duration.ofSeconds(9), person);
        }
    }

    public static class DoorClosing extends Event {
        public DoorClosing() {
            super(DOOR_CLOSING, Duration.ofSeconds(3));
        }
    }

    public static class LoadingNextPerson extends Event {
        public LoadingNextPerson(Person person) {
            super(LOADING_NEXT_PERSON, Duration.ofSeconds(6), person);
        }

    }

    public static class UnloadingNextPerson extends Event {
        public UnloadingNextPerson(Person person) {
            super(UNLOADING_NEXT_PERSON, Duration.ofSeconds(6), person);
        }
    }

    public static class StandByAtFloor extends Event {

        public StandByAtFloor() {
            super(STAND_BY_AT_FLOOR, Duration.ofSeconds(3));
        }
    }

    private static class StoppingAtFloor extends Event {
        public StoppingAtFloor() {
            super(STOPPING_AT_FLOOR, Duration.ofSeconds(3));
        }
    }
}
