
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

enum CarType {
    HATCHBACK,
    SEDAN,
    SUV
}

enum BookingStatus {
    CONFIRMED,
    CANCELLED
}

class Car {
    private final String carId;
    private final String model;
    private final CarType carType;
    private final double dailyPrice;

    public Car(String carId, String model, CarType carType, double dailyPrice) {
        if (dailyPrice <= 0) {
            throw new IllegalArgumentException("Invalid daily price");
        }
        this.carId = carId;
        this.model = model;
        this.carType = carType;
        this.dailyPrice = dailyPrice;
    }

    public String getCarId() {
        return carId;
    }

    public String getModel() {
        return model;
    }

    public CarType getCarType() {
        return carType;
    }

    public double getDailyPrice() {
        return dailyPrice;
    }
}

class Booking {
    private final String bookingId;
    private final String customerId;
    private final Car car;
    private final LocalDate pickupDate;
    private final LocalDate returnDate;
    private final double totalPrice;
    private BookingStatus status;

    public Booking(String bookingId, String customerId, Car car,
                   LocalDate pickupDate, LocalDate returnDate) {
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.car = car;
        this.pickupDate = pickupDate;
        this.returnDate = returnDate;
        this.totalPrice = ChronoUnit.DAYS.between(pickupDate, returnDate)
                * car.getDailyPrice();
        this.status = BookingStatus.CONFIRMED;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Car getCar() {
        return car;
    }

    public LocalDate getPickupDate() {
        return pickupDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
    }
}

class RentalService {
    private final Map<String, Car> cars = new HashMap<>();

    public void addCar(Car car) {
        if (cars.containsKey(car.getCarId())) {
            throw new IllegalArgumentException("Car already exists");
        }
        cars.put(car.getCarId(), car);
    }

    public Car getCar(String carId) {
        Car car = cars.get(carId);

        if (car == null) {
            throw new IllegalArgumentException("Car not found");
        }

        return car;
    }

    public List<Car> getAvailableCars(LocalDate pickupDate,
                                      LocalDate returnDate,
                                      List<Booking> bookings) {
        validateDates(pickupDate, returnDate);

        List<Car> availableCars = new ArrayList<>();

        for (Car car : cars.values()) {
            boolean available = true;

            for (Booking booking : bookings) {
                if (booking.getStatus() == BookingStatus.CANCELLED) {
                    continue;
                }

                if (booking.getCar().getCarId().equals(car.getCarId())
                        && pickupDate.isBefore(booking.getReturnDate())
                        && returnDate.isAfter(booking.getPickupDate())) {
                    available = false;
                    break;
                }
            }

            if (available) {
                availableCars.add(car);
            }
        }

        return availableCars;
    }

    private void validateDates(LocalDate pickupDate, LocalDate returnDate) {
        if (pickupDate == null || returnDate == null) {
            throw new IllegalArgumentException("Dates cannot be null");
        }

        if (!pickupDate.isBefore(returnDate)) {
            throw new IllegalArgumentException(
                    "Pickup date must be before return date");
        }
    }
}

class BookingService {
    private final RentalService rentalService;
    private final Map<String, Booking> bookings = new HashMap<>();

    public BookingService(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    public synchronized Booking createBooking(String customerId,
                                              String carId,
                                              LocalDate pickupDate,
                                              LocalDate returnDate) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid customer ID");
        }

        if (pickupDate == null || returnDate == null
                || !pickupDate.isBefore(returnDate)) {
            throw new IllegalArgumentException("Invalid booking dates");
        }

        Car car = rentalService.getCar(carId);

        List<Car> availableCars = rentalService.getAvailableCars(
                pickupDate, returnDate,
                new ArrayList<>(bookings.values()));

        boolean isAvailable = availableCars.stream()
                .anyMatch(c -> c.getCarId().equals(carId));

        if (!isAvailable) {
            throw new IllegalStateException("Car is not available");
        }

        String bookingId = UUID.randomUUID().toString();

        Booking booking = new Booking(
                bookingId,
                customerId,
                car,
                pickupDate,
                returnDate
        );

        bookings.put(bookingId, booking);

        return booking;
    }

    public synchronized void cancelBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);

        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking already cancelled");
        }

        booking.cancel();
    }

    public Booking getBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);

        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }

        return booking;
    }

    public List<Booking> getCustomerBookings(String customerId) {
        List<Booking> result = new ArrayList<>();

        for (Booking booking : bookings.values()) {
            if (booking.getCustomerId().equals(customerId)) {
                result.add(booking);
            }
        }

        return result;
    }
}

public class Main {
    public static void main(String[] args) {

        RentalService rentalService = new RentalService();

        rentalService.addCar(
                new Car("C101", "Hyundai i20", CarType.HATCHBACK, 1500)
        );

        rentalService.addCar(
                new Car("C102", "Honda City", CarType.SEDAN, 2500)
        );

        rentalService.addCar(
                new Car("C103", "Toyota Fortuner", CarType.SUV, 5000)
        );

        BookingService bookingService = new BookingService(rentalService);

        LocalDate pickupDate = LocalDate.of(2026, 12, 1);
        LocalDate returnDate = LocalDate.of(2026, 12, 4);

        List<Car> availableCars = rentalService.getAvailableCars(
                pickupDate,
                returnDate,
                Collections.emptyList()
        );

        System.out.println("Available Cars:");

        for (Car car : availableCars) {
            System.out.println(
                    car.getCarId() + " | " +
                            car.getModel() + " | " +
                            car.getDailyPrice()
            );
        }

        Booking booking = bookingService.createBooking(
                "CUSTOMER101",
                "C101",
                pickupDate,
                returnDate
        );

        System.out.println("\nBooking Confirmed");
        System.out.println("Booking ID: " + booking.getBookingId());
        System.out.println("Customer ID: " + booking.getCustomerId());
        System.out.println("Car: " + booking.getCar().getModel());
        System.out.println("Pickup Date: " + booking.getPickupDate());
        System.out.println("Return Date: " + booking.getReturnDate());
        System.out.println("Total Price: " + booking.getTotalPrice());

        bookingService.cancelBooking(booking.getBookingId());

        System.out.println("Booking Status: " +
                bookingService.getBooking(booking.getBookingId()).getStatus());
    }
}