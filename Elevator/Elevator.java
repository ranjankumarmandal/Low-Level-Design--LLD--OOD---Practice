
import java.util.*;

enum Direction {
    UP,
    DOWN,
    IDLE
}

enum DoorState {
    OPEN,
    CLOSED
}

class Elevator {
    private final int id;
    private int currentFloor;
    private Direction direction;
    private DoorState doorState;

    private final TreeSet<Integer> upRequests;
    private final TreeSet<Integer> downRequests;

    public Elevator(int id) {
        this.id = id;
        this.currentFloor = 0;
        this.direction = Direction.IDLE;
        this.doorState = DoorState.CLOSED;
        this.upRequests = new TreeSet<>();
        this.downRequests = new TreeSet<>(Comparator.reverseOrder());
    }

    public int getId() {
        return id;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public DoorState getDoorState() {
        return doorState;
    }

    public boolean isIdle() {
        return direction == Direction.IDLE
                && upRequests.isEmpty()
                && downRequests.isEmpty()
                && doorState == DoorState.CLOSED;
    }

    public void addRequest(int floor) {
        if (floor > currentFloor) {
            upRequests.add(floor);
        } else if (floor < currentFloor) {
            downRequests.add(floor);
        } else {
            doorState = DoorState.OPEN;
        }

        if (direction == Direction.IDLE && doorState == DoorState.CLOSED) {
            updateDirection();
        }
    }

    public void step() {
        if (doorState == DoorState.OPEN) {
            doorState = DoorState.CLOSED;
            updateDirection();
            return;
        }

        if (direction == Direction.IDLE) {
            updateDirection();

            if (direction == Direction.IDLE) {
                return;
            }
        }

        if (direction == Direction.UP) {
            currentFloor++;
        } else if (direction == Direction.DOWN) {
            currentFloor--;
        }

        if (upRequests.remove(currentFloor)
                || downRequests.remove(currentFloor)) {
            doorState = DoorState.OPEN;
            updateDirection();
            return;
        }

        updateDirection();
    }

    private void updateDirection() {
        if (direction == Direction.UP) {
            if (!upRequests.isEmpty()) {
                return;
            }

            if (!downRequests.isEmpty()) {
                direction = Direction.DOWN;
                return;
            }
        } else if (direction == Direction.DOWN) {
            if (!downRequests.isEmpty()) {
                return;
            }

            if (!upRequests.isEmpty()) {
                direction = Direction.UP;
                return;
            }
        }

        if (!upRequests.isEmpty()) {
            direction = Direction.UP;
        } else if (!downRequests.isEmpty()) {
            direction = Direction.DOWN;
        } else {
            direction = Direction.IDLE;
        }
    }

    public int getDistance(int floor) {
        return Math.abs(currentFloor - floor);
    }

    public void displayStatus() {
        System.out.println(
                "Elevator " + id +
                        " | Floor: " + currentFloor +
                        " | Direction: " + direction +
                        " | Door: " + doorState +
                        " | Up Requests: " + upRequests +
                        " | Down Requests: " + downRequests
        );
    }
}

class ElevatorController {
    private final List<Elevator> elevators;

    public ElevatorController(int numberOfElevators) {
        elevators = new ArrayList<>();

        for (int i = 1; i <= numberOfElevators; i++) {
            elevators.add(new Elevator(i));
        }
    }

    public void requestElevator(int floor, Direction requestedDirection) {
        Elevator selectedElevator = null;
        int minimumDistance = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            if (canServe(elevator, floor, requestedDirection)) {
                int distance = elevator.getDistance(floor);

                if (distance < minimumDistance) {
                    minimumDistance = distance;
                    selectedElevator = elevator;
                }
            }
        }

        if (selectedElevator == null) {
            for (Elevator elevator : elevators) {
                int distance = elevator.getDistance(floor);

                if (distance < minimumDistance) {
                    minimumDistance = distance;
                    selectedElevator = elevator;
                }
            }
        }

        if (selectedElevator != null) {
            selectedElevator.addRequest(floor);

            System.out.println(
                    "Elevator " + selectedElevator.getId()
                            + " assigned to floor " + floor
                            + " for direction " + requestedDirection
            );
        }
    }

    private boolean canServe(
            Elevator elevator,
            int floor,
            Direction requestedDirection) {

        if (elevator.isIdle()) {
            return true;
        }

        if (elevator.getDirection() != requestedDirection) {
            return false;
        }

        if (requestedDirection == Direction.UP) {
            return elevator.getCurrentFloor() <= floor;
        }

        if (requestedDirection == Direction.DOWN) {
            return elevator.getCurrentFloor() >= floor;
        }

        return false;
    }

    public void selectFloor(int elevatorId, int floor) {
        Elevator elevator = getElevator(elevatorId);

        if (elevator == null) {
            System.out.println("Invalid elevator ID");
            return;
        }

        elevator.addRequest(floor);

        System.out.println(
                "Floor " + floor +
                        " selected in Elevator " + elevatorId
        );
    }

    public void step() {
        for (Elevator elevator : elevators) {
            elevator.step();
        }
    }

    public void displayStatus() {
        for (Elevator elevator : elevators) {
            elevator.displayStatus();
        }
    }

    private Elevator getElevator(int elevatorId) {
        for (Elevator elevator : elevators) {
            if (elevator.getId() == elevatorId) {
                return elevator;
            }
        }

        return null;
    }
}

public class Main {
    public static void main(String[] args) {

        ElevatorController controller = new ElevatorController(3);

        controller.requestElevator(5, Direction.UP);
        controller.requestElevator(8, Direction.UP);
        controller.requestElevator(3, Direction.DOWN);

        controller.selectFloor(1, 10);
        controller.selectFloor(2, 12);

        for (int i = 1; i <= 15; i++) {
            System.out.println("\n========== STEP " + i + " ==========");

            controller.step();
            controller.displayStatus();
        }
    }
}