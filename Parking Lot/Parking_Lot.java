
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

enum VehicleType {
    MOTORCYCLE, CAR, TRUCK
}

enum SpotType {
    MOTORCYCLE, COMPACT, LARGE
}

class Vehicle {
    private final String licensePlate;
    private final VehicleType type;

    public Vehicle(String licensePlate, VehicleType type) {
        if (licensePlate == null || licensePlate.isBlank() || type == null) {
            throw new IllegalArgumentException("Invalid vehicle");
        }
        this.licensePlate = licensePlate;
        this.type = type;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public VehicleType getType() {
        return type;
    }
}

class ParkingSpot {
    private final String id;
    private final SpotType type;
    private final int floorNumber;
    private Vehicle vehicle;

    public ParkingSpot(String id, SpotType type, int floorNumber) {
        this.id = id;
        this.type = type;
        this.floorNumber = floorNumber;
    }

    public boolean isAvailable() {
        return vehicle == null;
    }

    public boolean canFit(Vehicle vehicle) {
        return switch (vehicle.getType()) {
            case MOTORCYCLE -> true;
            case CAR -> type == SpotType.COMPACT || type == SpotType.LARGE;
            case TRUCK -> type == SpotType.LARGE;
        };
    }

    public void parkVehicle(Vehicle vehicle) {
        if (!isAvailable() || !canFit(vehicle)) {
            throw new IllegalStateException("Spot unavailable or incompatible");
        }
        this.vehicle = vehicle;
    }

    public void removeVehicle() {
        if (vehicle == null) {
            throw new IllegalStateException("Spot is already empty");
        }
        this.vehicle = null;
    }

    public String getId() {
        return id;
    }

    public SpotType getType() {
        return type;
    }

    public int getFloorNumber() {
        return floorNumber;
    }
}

class ParkingFloor {
    private final int floorNumber;
    private final List<ParkingSpot> spots;

    public ParkingFloor(int floorNumber, List<ParkingSpot> spots) {
        this.floorNumber = floorNumber;
        this.spots = new ArrayList<>(spots);
    }

    public List<ParkingSpot> getSpots() {
        return Collections.unmodifiableList(spots);
    }

    public int getFloorNumber() {
        return floorNumber;
    }
}

class ParkingTicket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private boolean active;

    public ParkingTicket(Vehicle vehicle, ParkingSpot spot) {
        this.ticketId = UUID.randomUUID().toString();
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTime = LocalDateTime.now();
        this.active = true;
    }

    public void close(LocalDateTime exitTime) {
        if (!active) {
            throw new IllegalStateException("Ticket already closed");
        }
        if (exitTime.isBefore(entryTime)) {
            throw new IllegalArgumentException("Invalid exit time");
        }
        this.exitTime = exitTime;
        this.active = false;
    }

    public String getTicketId() {
        return ticketId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingSpot getSpot() {
        return spot;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public boolean isActive() {
        return active;
    }
}

class ParkingReceipt {
    private final String receiptId;
    private final String ticketId;
    private final BigDecimal amount;
    private final long chargedHours;
    private final LocalDateTime exitTime;

    public ParkingReceipt(String ticketId, BigDecimal amount,
                          long chargedHours, LocalDateTime exitTime) {
        this.receiptId = UUID.randomUUID().toString();
        this.ticketId = ticketId;
        this.amount = amount;
        this.chargedHours = chargedHours;
        this.exitTime = exitTime;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public long getChargedHours() {
        return chargedHours;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }
}

interface SpotAssignmentStrategy {
    ParkingSpot findSpot(Vehicle vehicle, List<ParkingFloor> floors);
}

class FirstAvailableSpotStrategy implements SpotAssignmentStrategy {

    @Override
    public ParkingSpot findSpot(Vehicle vehicle, List<ParkingFloor> floors) {
        for (ParkingFloor floor : floors) {
            for (ParkingSpot spot : floor.getSpots()) {
                if (spot.isAvailable() && spot.canFit(vehicle)) {
                    return spot;
                }
            }
        }
        throw new IllegalStateException("No parking spot available");
    }
}

interface PricingStrategy {
    BigDecimal calculateFee(ParkingTicket ticket);
}

class HourlyPricingStrategy implements PricingStrategy {
    private final BigDecimal hourlyRate;

    public HourlyPricingStrategy(BigDecimal hourlyRate) {
        if (hourlyRate == null || hourlyRate.signum() < 0) {
            throw new IllegalArgumentException("Invalid hourly rate");
        }
        this.hourlyRate = hourlyRate;
    }

    @Override
    public BigDecimal calculateFee(ParkingTicket ticket) {
        if (ticket.getExitTime() == null) {
            throw new IllegalStateException("Ticket is not closed");
        }

        long minutes = Duration.between(
                ticket.getEntryTime(),
                ticket.getExitTime()
        ).toMinutes();

        long hours = Math.max(1, (minutes + 59) / 60);

        return hourlyRate.multiply(BigDecimal.valueOf(hours));
    }
}

class ParkingLot {
    private final String name;
    private final List<ParkingFloor> floors;
    private final SpotAssignmentStrategy assignmentStrategy;
    private final PricingStrategy pricingStrategy;

    private final Map<String, ParkingTicket> activeTickets;
    private final Map<String, ParkingTicket> allTickets;
    private final Map<String, ParkingReceipt> receipts;
    private final Map<String, String> vehicleActiveTickets;

    public ParkingLot(
            String name,
            List<ParkingFloor> floors,
            SpotAssignmentStrategy assignmentStrategy,
            PricingStrategy pricingStrategy) {

        this.name = name;
        this.floors = new ArrayList<>(floors);
        this.assignmentStrategy = assignmentStrategy;
        this.pricingStrategy = pricingStrategy;

        this.activeTickets = new HashMap<>();
        this.allTickets = new HashMap<>();
        this.receipts = new HashMap<>();
        this.vehicleActiveTickets = new HashMap<>();
    }

    public synchronized ParkingTicket parkVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle cannot be null");
        }

        String licensePlate = vehicle.getLicensePlate();

        if (vehicleActiveTickets.containsKey(licensePlate)) {
            throw new IllegalStateException(
                    "Vehicle already has an active ticket");
        }

        ParkingSpot spot = assignmentStrategy.findSpot(vehicle, floors);

        spot.parkVehicle(vehicle);

        ParkingTicket ticket = new ParkingTicket(vehicle, spot);

        activeTickets.put(ticket.getTicketId(), ticket);
        allTickets.put(ticket.getTicketId(), ticket);
        vehicleActiveTickets.put(licensePlate, ticket.getTicketId());

        return ticket;
    }

    public synchronized ParkingReceipt unparkVehicle(String ticketId) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("Invalid ticket ID");
        }

        ParkingTicket ticket = allTickets.get(ticketId);

        if (ticket == null) {
            throw new IllegalArgumentException("Invalid ticket");
        }

        if (!ticket.isActive() || !activeTickets.containsKey(ticketId)) {
            throw new IllegalStateException("Ticket already closed");
        }

        LocalDateTime exitTime = LocalDateTime.now();

        ticket.close(exitTime);

        BigDecimal fee = pricingStrategy.calculateFee(ticket);

        long minutes = Duration.between(
                ticket.getEntryTime(), exitTime).toMinutes();

        long hours = Math.max(1, (minutes + 59) / 60);

        ParkingReceipt receipt = new ParkingReceipt(
                ticketId, fee, hours, exitTime);

        ticket.getSpot().removeVehicle();

        activeTickets.remove(ticketId);
        vehicleActiveTickets.remove(
                ticket.getVehicle().getLicensePlate());

        receipts.put(receipt.getReceiptId(), receipt);

        return receipt;
    }

    public synchronized int getAvailableSpotCount() {
        return (int) floors.stream()
                .flatMap(floor -> floor.getSpots().stream())
                .filter(ParkingSpot::isAvailable)
                .count();
    }

    public synchronized Optional<ParkingTicket> getActiveTicket(
            String licensePlate) {
        String ticketId = vehicleActiveTickets.get(licensePlate);
        return Optional.ofNullable(activeTickets.get(ticketId));
    }

    public synchronized Optional<ParkingReceipt> getReceipt(
            String receiptId) {
        return Optional.ofNullable(receipts.get(receiptId));
    }

    public String getName() {
        return name;
    }
}

public class Main {
    public static void main(String[] args) {

        ParkingSpot s1 = new ParkingSpot("S1", SpotType.MOTORCYCLE, 1);
        ParkingSpot s2 = new ParkingSpot("S2", SpotType.COMPACT, 1);
        ParkingSpot s3 = new ParkingSpot("S3", SpotType.LARGE, 1);

        ParkingFloor floor1 = new ParkingFloor(
                1, List.of(s1, s2, s3));

        ParkingLot parkingLot = new ParkingLot(
                "Central Parking",
                List.of(floor1),
                new FirstAvailableSpotStrategy(),
                new HourlyPricingStrategy(new BigDecimal("50"))
        );

        Vehicle car = new Vehicle("KA01AB1234", VehicleType.CAR);

        ParkingTicket ticket = parkingLot.parkVehicle(car);

        System.out.println("Parking Lot: " + parkingLot.getName());
        System.out.println("Ticket ID: " + ticket.getTicketId());
        System.out.println("Assigned Spot: " + ticket.getSpot().getId());
        System.out.println("Available Spots: "
                + parkingLot.getAvailableSpotCount());

        ParkingReceipt receipt =
                parkingLot.unparkVehicle(ticket.getTicketId());

        System.out.println("Receipt ID: " + receipt.getReceiptId());
        System.out.println("Amount: " + receipt.getAmount());
        System.out.println("Charged Hours: " + receipt.getChargedHours());
        System.out.println("Available Spots: "
                + parkingLot.getAvailableSpotCount());
    }
}