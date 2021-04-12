package org.paumard.elevator.system;

import org.paumard.elevator.Building;
import org.paumard.elevator.Elevator;

import java.util.Map;

public class ShadowElevators {

    private Map<String, ShadowElevator> shadowElevators;

    public ShadowElevators(Map<String, ShadowElevator> shadowElevators) {
        this.shadowElevators = shadowElevators;
    }

    public boolean areStillRunning() {
        return shadowElevators.values().stream()
                .anyMatch(ShadowElevator::isRunning);
    }

    public void lastPersonArrived() {
        shadowElevators.values().forEach(ShadowElevator::lastPersonArrived);
    }

    public ShadowElevator getShadowElevatorFor(Elevator elevator) {
        return shadowElevators.get(elevator.getId());
    }

    public void printPeople() {
        shadowElevators.values().forEach(ShadowElevator::printPeople);
    }

    public void printCounts() {
        long totalLoadedCount = 0L;
        long totalUnloadedCount = 0L;
        for (ShadowElevator shadowElevator : shadowElevators.values()) {
            long loaded = shadowElevator.getCountLoadedPeople();
            totalLoadedCount += loaded;
            long unloaded = shadowElevator.getCountUnloadedPeople();
            totalUnloadedCount += unloaded;
            Building.PRINTER.printf("\tElevator [%s] people loaded: %d\n", shadowElevator.getId(), loaded);
            Building.PRINTER.printf("\tElevator [%s] people unloaded: %d\n", shadowElevator.getId(), unloaded);
        }
        Building.PRINTER.printf("Total people loaded: %d\n", totalLoadedCount);
        Building.PRINTER.printf("Total people unloaded: %d\n", totalUnloadedCount);
    }

    public void printMaxes() {
        for (ShadowElevator shadowElevator : shadowElevators.values()) {
            Building.PRINTER.printf("\tElevator [%s] max people loaded: %d\n", shadowElevator.getId(), shadowElevator.getMaxLoad());
            if (Building.PRINTER != System.out) {
                System.out.printf("\tElevator [%s] max people loaded: %d\n", shadowElevator.getId(), shadowElevator.getMaxLoad());
            }
        }
    }
}
