package org.paumard.elevator.model;

import org.paumard.elevator.Building;

import java.io.BufferedReader;
import java.io.FileReader;
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
    private static List<String> names = new ArrayList<>();
    private static NavigableMap<LocalTime, Double> affluence;
    private static BinaryOperator<Double> unused = (d1, d2) -> d1;

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

    private List<List<Person>> peopleWaitingPerFloor;

    public WaitingList() {
        this.peopleWaitingPerFloor = new ArrayList<>();
        for (int floor = 0; floor < MAX_FLOOR; floor++) {
            peopleWaitingPerFloor.add(createWaitingPeople(floor));
        }
    }

    public WaitingList(String pathName) {

        this.peopleWaitingPerFloor = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(pathName))) {
            String line = reader.readLine();
            while (line != null) {
                if (line.isBlank() || line.startsWith("#")) {

                    // the line is a comment, do nothing

                } else if (line.startsWith("People waiting on floor")) {

                    List<Person> peopleAtFloor = new ArrayList<>();
                    peopleWaitingPerFloor.add(peopleAtFloor);

                } else if (line.contains("No one")) {

                    // Nothing to do

                } else if (line.contains("going to")) {

                    String name = line.trim().substring(0, line.indexOf(' ')).trim();
                    String destinationFloorAsString = line.substring(line.lastIndexOf(' ') + 1);
                    int destinationFloor = Integer.parseInt(destinationFloorAsString);
                    Person p = new Person(START_TIME, name, destinationFloor);
                    peopleWaitingPerFloor.get(peopleWaitingPerFloor.size() - 1).add(p);
                }

                line = reader.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private List<Person> createWaitingPeople(int floor) {

        return new ArrayList<>();
    }

    public Optional<Map.Entry<Integer, Person>> addNewPeopleToLists(LocalTime time) {

        LocalTime key = affluence.floorKey(time);
        double probabilityToAddAPerson = affluence.get(key) / 3d;
        if (random.nextFloat() < probabilityToAddAPerson) {
            int indexFloor = -1;
            if (time.isBefore(LocalTime.of(10, 30, 0))) {
                if (random.nextFloat() < PROBABLITY_TO_CALL_FROM_1) {
                    indexFloor = 0;
                } else {
                    indexFloor = random.nextInt(Building.MAX_FLOOR - 1) + 1;
                }
            } else if (time.isAfter(LocalTime.of(16, 30, 0))) {
                if (random.nextFloat() < PROBABLITY_NOT_TO_CALL_FROM_1) {
                    indexFloor = random.nextInt(Building.MAX_FLOOR - 1) + 1;
                } else {
                    indexFloor = 0;
                }
            } else {
                indexFloor = random.nextInt(Building.MAX_FLOOR);
            }

            int indexDestinationFloor = -1;
            if (time.isAfter(LocalTime.of(16, 30, 0)) && indexFloor != 0) {
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

            this.peopleWaitingPerFloor.get(indexFloor).add(person);

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
        return this.peopleWaitingPerFloor.get(index);
    }

    public List<List<Person>> getLists() {
        List<List<Person>> defensiveCopy =
                peopleWaitingPerFloor.stream().map(ArrayList::new).collect(Collectors.toList());
        return defensiveCopy;
    }

    public List<Person> getNextLoadablePeople(List<Integer> nextFloors, int fromFloor) {

        int index = fromFloor - 1;
        List<Person> peopleToLoad = this.peopleWaitingPerFloor.get(index).stream()
                .filter(person -> nextFloors.contains(person.getDestinationFloor()))
                .collect(Collectors.toList());

//
        return peopleToLoad;
    }

    public void removePeopleFromFloor(int currentFloor, List<Person> loadedPeople) {
        int index = currentFloor - 1;
        this.peopleWaitingPerFloor.get(index).removeAll(loadedPeople);
    }

}