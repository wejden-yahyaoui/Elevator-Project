package org.paumard.elevator.model;

import org.paumard.elevator.Building;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.paumard.elevator.Building.MAX_FLOOR;

public class WaitingList {

    private static List<String> names = new ArrayList<>();

    static {
        Path path = Path.of("files/first-name.txt");
        try (Stream<String> lines = Files.lines(path)) {
            names = lines.collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
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
                    Person p = new Person(name, destinationFloor);
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
            System.out.println("People waiting on floor " + floor);
            if (peopleWaitingPerFloor.get(index).isEmpty()) {
                System.out.println("\tNo one");
            } else {
                peopleWaitingPerFloor.get(index).forEach(p -> System.out.println("\t" + p));
            }
        }
    }

    private List<Person> createWaitingPeople(int floor) {
        int numberOfPeopleInLine = Building.random.nextInt(Building.MAX_NUMBER_OF_PEOPLE_IN_LINE);
        List<Person> people = new ArrayList<>();
        for (int i = 0; i < numberOfPeopleInLine; i++) {
            int destinationFloor = Building.random.nextInt(Building.MAX_FLOOR);
            while (destinationFloor == floor) {
                destinationFloor = Building.random.nextInt(Building.MAX_FLOOR);
            }
            String name = names.get(Building.random.nextInt(names.size()));
            Person p = new Person(name, destinationFloor + 1);
            people.add(p);
        }
        return people;
    }

    public Optional<Map.Entry<Integer, Person>> addNewPeopleToLists() {

        if (Building.random.nextFloat() < Building.PROBABILITY_TO_SEE_A_NEW_PERSON_IN_LINE) {
            int floor = Building.random.nextInt(Building.MAX_FLOOR);
            int destinationFloor = Building.random.nextInt(Building.MAX_FLOOR);
            while (destinationFloor == floor) {
                destinationFloor = Building.random.nextInt(Building.MAX_FLOOR);
            }
            String name = names.get(Building.random.nextInt(names.size()));
            Person person = new Person(name, destinationFloor + 1);

            this.peopleWaitingPerFloor.get(floor).add(person);

            return Optional.of(Map.entry(floor + 1, person));

        } else {

            return Optional.empty();

        }
    }

    public int countPeople() {
        return peopleWaitingPerFloor.stream()
                .mapToInt(List::size)
                .sum();
    }

    public int countPeopleAtOtherFloor(int floor) {
        int index = floor - 1;
        return IntStream.range(0, peopleWaitingPerFloor.size())
                .filter(i -> i != index)
                .map(i -> peopleWaitingPerFloor.get(i).size())
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

    public Optional<Person> getNextPersonToLoad(int fromFloor) {
        int index = fromFloor - 1;
        Optional<Person> firstPerson = this.peopleWaitingPerFloor.get(index).stream().findFirst();
        firstPerson.ifPresent(this.peopleWaitingPerFloor.get(index)::remove);
        return firstPerson;
    }

    public Optional<Person> getNextPersonToLoad(int nextFloor, int fromFloor) {
        int index = fromFloor - 1;
        if (nextFloor > fromFloor) {
            Optional<Person> firstPerson = this.peopleWaitingPerFloor.get(index)
                    .stream().filter(p -> p.getDestinationFloor() >= nextFloor)
                    .findFirst();
            firstPerson.ifPresent(this.peopleWaitingPerFloor.get(index)::remove);
            return firstPerson;
        } else {
            Optional<Person> firstPerson = this.peopleWaitingPerFloor.get(index)
                    .stream().filter(p -> p.getDestinationFloor() <= nextFloor)
                    .findFirst();
            firstPerson.ifPresent(this.peopleWaitingPerFloor.get(index)::remove);
            return firstPerson;
        }
    }
}
