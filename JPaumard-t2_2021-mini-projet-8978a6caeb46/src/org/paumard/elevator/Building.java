package org.paumard.elevator;

import org.paumard.elevator.event.Event;
import org.paumard.elevator.model.Person;
import org.paumard.elevator.model.WaitingList;
import org.paumard.elevator.student.LessDumbElevator;
import org.paumard.elevator.system.ShadowElevator;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class Building {

    public static PrintStream PRINTER;
    public static final int ELEVATOR_CAPACITY = 15;
    public static final int ELEVATOR_LOADING_CAPACITY = 3;
    public static final int MAX_DISPLAYED_FLOORS = 10;
    public static final int MAX_FLOOR = 10;
    public static final LocalTime START_TIME = LocalTime.of(6, 0, 0);
    public static final LocalTime END_TIME = LocalTime.of(22, 30, 0);
    public static final LocalTime END_OF_DAY = END_TIME.plusHours(1);
    public static Random random = new Random(10L); // 10L
    private static LocalTime time = START_TIME;

    public static void main(String[] args) throws FileNotFoundException {

        PRINTER = System.out;
        //PRINTER = new PrintStream("logs/debug.log");

        Set<PrintStream> printers = new HashSet<>(List.of(PRINTER, System.out));

        NavigableMap<LocalTime, Event> events = new TreeMap<>();


        Event startEvent = new Event(Event.ELEVATOR_STARTS);
        events.put(time, startEvent);

        WaitingList peopleWaitingPerFloor = new WaitingList();
        Elevator elevator = new LessDumbElevator(ELEVATOR_CAPACITY);


        int totalNumberOfPeople = peopleWaitingPerFloor.countPeople();
        elevator.peopleWaiting(peopleWaitingPerFloor.getLists());
        ShadowElevator shadowElevator = new ShadowElevator(ELEVATOR_CAPACITY, peopleWaitingPerFloor);

        peopleWaitingPerFloor.print();

        printers.forEach(printer -> {
            printer.println("Start time = " + START_TIME);
            printer.println("End time = " + END_TIME);
            printer.println("End of day = " + END_OF_DAY);
        });

        while (!shadowElevator.isStopped() && time.isBefore(END_OF_DAY)) {

            elevator.timeIs(time);

            if (time.equals(END_TIME)) {
                PRINTER.printf("\n[%s]No more people are coming.\n", time.toString());
                shadowElevator.lastPersonArrived();
                elevator.lastPersonArrived();
            }

            if (!events.containsKey(time)) {
                if (time.isBefore(END_TIME)) {
                    totalNumberOfPeople += addNewPersonToWaitingLists(time, peopleWaitingPerFloor, elevator);
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

            if (time.isBefore(END_TIME)) {
                totalNumberOfPeople += addNewPersonToWaitingLists(time, peopleWaitingPerFloor, elevator);
            }
            time = time.plusSeconds(3);
        }
        peopleWaitingPerFloor.print();
        shadowElevator.printPeople();
        printers.forEach(printer -> {
            printer.printf("[%s] Times up\n", time);
            printer.println("People loaded: " + shadowElevator.getCount());
            printer.println("Max people loaded: " + shadowElevator.getMaxLoad());
        });
        printDurationHistogram();

        long numberOfPeople =
                Event.durations.values().stream().mapToLong(l -> l).sum();
        Duration maxDuration =
                Event.durations.keySet().stream().max(Comparator.naturalOrder()).orElseThrow();
        LongSummaryStatistics stats = Event.durations.entrySet().stream()
                .collect(Collectors.summarizingLong(entry -> entry.getKey().getSeconds() * entry.getValue()));
        Duration averageDuration = Duration.ofSeconds((long) stats.getAverage());

        printers.forEach(printer -> {
            printer.println("Number of people taken = " + numberOfPeople);
            printer.printf("Average waiting time = %dmn %ds\n",
                    averageDuration.toMinutesPart(), averageDuration.toSecondsPart());
            printer.printf("Max waiting time = %dh %dmn %ds\n",
                    maxDuration.toHoursPart(), maxDuration.toMinutesPart(), maxDuration.toSecondsPart());
            printer.println("People left in elevator = " + shadowElevator.numberOfPeopleInElevator());
            printer.println("People left in floors = " + peopleWaitingPerFloor.countPeople());
        });

        System.out.println("Day is finished");
    }

    private static int addNewPersonToWaitingLists(LocalTime time, WaitingList peopleWaitingPerFloor, Elevator elevator) {
        Optional<Map.Entry<Integer, Person>> newPersonWaiting = peopleWaitingPerFloor.addNewPeopleToLists(time);
        if (newPersonWaiting.isPresent()) {
            int floor = newPersonWaiting.orElseThrow().getKey();
            Person person = newPersonWaiting.orElseThrow().getValue();
            elevator.newPersonWaitingAtFloor(floor, person);
            PRINTER.printf("\n[%s] %s calls the elevator from floor %d to go to floor %d\n", time, person.getName(), floor, person.getDestinationFloor());
            PRINTER.printf("Waiting list is now:\n");
            peopleWaitingPerFloor.print();
            return 1;
        } else {
            return 0;
        }
    }

    private static void printDurationHistogram() {
        NavigableMap<Duration, Long> durations = new TreeMap<>();

        durations.put(Duration.ofSeconds(30), 0L);
        durations.put(Duration.ofSeconds(180), 0L);
        durations.put(Duration.ofSeconds(360), 0L);
        durations.put(Duration.ofMinutes(15), 0L);
        durations.put(Duration.ofMinutes(30), 0L);
        durations.put(Duration.ofHours(1), 0L);

        Event.durations.forEach(
                (duration, count) -> {
                    for (Duration keyDuration : durations.descendingKeySet()) {
                        if (duration.compareTo(keyDuration) > 0) {
                            durations.merge(keyDuration, 1L, Long::sum);
                            continue;
                        }
                    }
                }
        );

        durations.forEach(
                (duration, count) ->
                        System.out.printf("%2dh %2dmn %2ds -> %d\n", duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart(), count)
        );

        if (PRINTER != System.out) {
            durations.forEach(
                    (duration, count) ->
                            PRINTER.printf("%2dh %2dmn %2ds -> %d\n", duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart(), count)
            );
        }
    }
}
