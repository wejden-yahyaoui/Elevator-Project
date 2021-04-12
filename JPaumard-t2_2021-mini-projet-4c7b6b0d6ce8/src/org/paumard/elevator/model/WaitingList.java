package org.paumard.elevator.model;

import org.paumard.elevator.Building;
import org.paumard.elevator.event.Event;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.paumard.elevator.Building.*;

public class WaitingList {

    public static final double PROBABLITY_TO_CALL_FROM_1 = 0.8d;
    private static final double PROBABLITY_NOT_TO_CALL_FROM_1 = 0.9d;
    private static final double ADJUSTMENT_COEFFICIENT = 10d;
    private static List<String> names = new ArrayList<>();
    private static NavigableMap<LocalTime, Double> affluence;
    private static BinaryOperator<Double> unused = (d1, d2) -> d1;
    public static long countPeopleGenerated = 0L;
    public static long countPeopleRemoved = 0L;

    static {
        Path nameFile = Path.of("files/first-name.txt");
        try (Stream<String> lines = Files.lines(nameFile)) {
            names = lines.collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Path affluenceFile = Path.of("files/affluence.txt");
        try (Stream<String> lines = Files.lines(affluenceFile)) {
            affluence = lines
                    .filter(line -> !line.isBlank())
                    .filter(line -> !line.startsWith("#"))
                    .map(Affluence::of)
                    .collect(Collectors.toMap(Affluence::getTime, Affluence::getAffluence, unused, TreeMap::new));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Event chooseEventFrom(List<Event> possibleEvents) {
        int index = random.nextInt(possibleEvents.size());
        return possibleEvents.get(index);
    }

    private static class Affluence {

        private LocalTime time;
        private double affluence;

        private Affluence(LocalTime time, double affluence) {
            this.time = time;
            this.affluence = affluence;
        }

        static Affluence of(String line) {
            String timeAsString = line.substring(0, line.indexOf(';'));
            LocalTime time = LocalTime.parse(timeAsString);
            double affluence = Double.parseDouble(line.substring(line.lastIndexOf(';') + 1));
            return new Affluence(time, affluence);
        }

        public LocalTime getTime() {
            return time;
        }

        public double getAffluence() {
            return affluence;
        }
    }

    public static List<List<Person>> peopleWaitingPerFloor;

    public static WaitingList getInstance() {
        WaitingList waitingList = new WaitingList();
        peopleWaitingPerFloor = new ArrayList<>();
        for (int floorIndex = 0; floorIndex < MAX_FLOOR; floorIndex++) {
            peopleWaitingPerFloor.add(new ArrayList<>());
        }
        return waitingList;
    }

    public static WaitingList getInstanceWith(List<List<Person>> waitingLists) {
        WaitingList waitingList = new WaitingList();
        peopleWaitingPerFloor = new ArrayList<>();
        for (int floorIndex = 0; floorIndex < MAX_FLOOR; floorIndex++) {
            List<Person> people = new ArrayList<>(waitingLists.get(floorIndex));
            peopleWaitingPerFloor.add(people);
        }
        return waitingList;
    }

    private WaitingList() {

    }

    public void print() {
        for (int index = 0; index < peopleWaitingPerFloor.size(); index++) {
            int floor = index + 1;
            PRINTER.println("People waiting on floor " + floor);
            if (peopleWaitingPerFloor.get(index).isEmpty()) {
                PRINTER.println("\tNo one");
            } else {
                peopleWaitingPerFloor.get(index).forEach(p -> PRINTER.println("\t" + p));
            }
        }
    }

    public Optional<Map.Entry<Integer, Person>> addNewPeopleToLists(LocalTime time) {

        LocalTime key = affluence.floorKey(time);
        double probabilityToAddAPerson = affluence.get(key) / ADJUSTMENT_COEFFICIENT;
        if (random.nextFloat() < probabilityToAddAPerson) {
            int indexFloor = -1;
            if (time.isBefore(LocalTime.of(10, 30, 0))) {
                if (random.nextFloat() < PROBABLITY_TO_CALL_FROM_1) {
                    indexFloor = 0;
                } else {
                    indexFloor = random.nextInt(Building.MAX_FLOOR);
                }
            } else if (time.isAfter(LocalTime.of(16, 30, 0))) {
                if (random.nextFloat() < PROBABLITY_NOT_TO_CALL_FROM_1) {
                    indexFloor = random.nextInt(Building.MAX_FLOOR - 1) + 1;
                } else {
                    indexFloor = random.nextInt(Building.MAX_FLOOR);
                }
            } else {
                indexFloor = random.nextInt(Building.MAX_FLOOR);
            }

            int indexDestinationFloor = -1;
            if (time.isAfter(LocalTime.of(16, 30, 0)) && indexFloor != 1) {
                if (random.nextFloat() < PROBABLITY_NOT_TO_CALL_FROM_1) {
                    indexDestinationFloor = 0;
                } else {
                    indexDestinationFloor = generateDestinationFloorDifferentFrom(indexFloor);
                }
            } else {
                indexDestinationFloor = generateDestinationFloorDifferentFrom(indexFloor);
            }
            String name = names.get(random.nextInt(names.size()));
            Person person = new Person(time, name, indexDestinationFloor + 1);

            countPeopleGenerated++;
            peopleWaitingPerFloor.get(indexFloor).add(person);

            return Optional.of(Map.entry(indexFloor + 1, person));

        } else {

            return Optional.empty();
        }
    }

    private int generateDestinationFloorDifferentFrom(int floor) {
        int destinationFloor = random.nextInt(Building.MAX_FLOOR);
        while (destinationFloor == floor) {
            destinationFloor = random.nextInt(Building.MAX_FLOOR);
        }
        return destinationFloor;
    }

    public int countPeople() {
        return peopleWaitingPerFloor.stream()
                .mapToInt(List::size)
                .sum();
    }

    public List<Person> getListFor(int floor) {
        int index = floor - 1;
        return new ArrayList<>(this.peopleWaitingPerFloor.get(index));
    }

    public List<List<Person>> getLists() {
        List<List<Person>> defensiveCopy =
                peopleWaitingPerFloor.stream().map(ArrayList::new).collect(Collectors.toList());
        return defensiveCopy;
    }

    public List<Person> getNextLoadablePeople(List<Integer> nextFloors, int fromFloor) {

        int index = fromFloor - 1;
        List<Person> peopleToLoad = peopleWaitingPerFloor.get(index).stream()
                .filter(person -> nextFloors.contains(person.getDestinationFloor()))
                .collect(Collectors.toList());

        return peopleToLoad;
    }

    public boolean hasLoadablePeople(List<Integer> nextFloors, int fromFloor) {

        int index = fromFloor - 1;
        boolean hasLoadablePeople = peopleWaitingPerFloor.get(index).stream()
                .anyMatch(person -> nextFloors.contains(person.getDestinationFloor()));

        return hasLoadablePeople;
    }

    public void removePersonFromFloor(int currentFloor, List<Person> loadedPeople) {
        int index = currentFloor - 1;
        countPeopleRemoved += loadedPeople.size();
        this.peopleWaitingPerFloor.get(index).removeAll(loadedPeople);
    }

    public void removePeopleFromFloor(int currentFloor, Person person) {
        int index = currentFloor - 1;
        countPeopleRemoved++;
        this.peopleWaitingPerFloor.get(index).remove(person);
    }
}