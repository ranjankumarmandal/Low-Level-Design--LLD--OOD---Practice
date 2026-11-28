
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

enum RoomType {
    SINGLE,
    DOUBLE,
    DELUXE,
    SUITE
}

enum BookingStatus {
    CONFIRMED,
    CANCELLED
}

class Hotel {
    private final String hotelId;
    private final String name;
    private final String city;
    private final List<Room> rooms;

    public Hotel(String hotelId, String name, String city) {
        this.hotelId = hotelId;
        this.name = name;
        this.city = city;
        this.rooms = new ArrayList<>();
    }

    public void addRoom(Room room) {
        rooms.add(room);
    }

    public List<Room> getRooms() {
        return Collections.unmodifiableList(rooms);
    }

    public String getHotelId() {
        return hotelId;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }
}

class Room {
    private final String roomId;
    private final RoomType roomType;
    private final double pricePerNight;
    private final List<Booking> bookings;

    public Room(String roomId, RoomType roomType, double pricePerNight) {
        this.roomId = roomId;
        this.roomType = roomType;
        this.pricePerNight = pricePerNight;
        this.bookings = new ArrayList<>();
    }

    public synchronized boolean isAvailable(
            LocalDate checkIn,
            LocalDate checkOut) {

        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                boolean overlaps =
                        checkIn.isBefore(booking.getCheckOut()) &&
                                booking.getCheckIn().isBefore(checkOut);

                if (overlaps) {
                    return false;
                }
            }
        }

        return true;
    }

    public synchronized void addBooking(Booking booking) {
        if (!isAvailable(booking.getCheckIn(), booking.getCheckOut())) {
            throw new IllegalStateException("Room is not available");
        }

        bookings.add(booking);
    }

    public synchronized void removeBooking(Booking booking) {
        bookings.remove(booking);
    }

    public String getRoomId() {
        return roomId;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public double getPricePerNight() {
        return pricePerNight;
    }
}

class Booking {
    private final String bookingId;
    private final String userId;
    private final Hotel hotel;
    private final Room room;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final double totalPrice;
    private BookingStatus status;

    public Booking(
            String bookingId,
            String userId,
            Hotel hotel,
            Room room,
            LocalDate checkIn,
            LocalDate checkOut) {

        this.bookingId = bookingId;
        this.userId = userId;
        this.hotel = hotel;
        this.room = room;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.totalPrice = room.getPricePerNight()
                * (checkOut.toEpochDay() - checkIn.toEpochDay());
        this.status = BookingStatus.CONFIRMED;
    }

    public synchronized void cancel() {
        if (status == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking already cancelled");
        }

        status = BookingStatus.CANCELLED;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getUserId() {
        return userId;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public Room getRoom() {
        return room;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public BookingStatus getStatus() {
        return status;
    }
}

class BookingSystem {
    private final Map<String, Hotel> hotels;
    private final Map<String, Booking> bookings;
    private final AtomicLong bookingCounter;

    public BookingSystem() {
        this.hotels = new HashMap<>();
        this.bookings = new HashMap<>();
        this.bookingCounter = new AtomicLong(1);
    }

    public synchronized void addHotel(Hotel hotel) {
        if (hotels.containsKey(hotel.getHotelId())) {
            throw new IllegalArgumentException("Hotel already exists");
        }

        hotels.put(hotel.getHotelId(), hotel);
    }

    public List<Room> searchAvailableRooms(
            String hotelId,
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut) {

        validateDates(checkIn, checkOut);

        Hotel hotel = hotels.get(hotelId);

        if (hotel == null) {
            throw new IllegalArgumentException("Hotel not found");
        }

        List<Room> availableRooms = new ArrayList<>();

        for (Room room : hotel.getRooms()) {
            if (room.getRoomType() == roomType &&
                    room.isAvailable(checkIn, checkOut)) {
                availableRooms.add(room);
            }
        }

        return availableRooms;
    }

    public synchronized Booking bookRoom(
            String userId,
            String hotelId,
            String roomId,
            LocalDate checkIn,
            LocalDate checkOut) {

        validateDates(checkIn, checkOut);

        Hotel hotel = hotels.get(hotelId);

        if (hotel == null) {
            throw new IllegalArgumentException("Hotel not found");
        }

        Room selectedRoom = null;

        for (Room room : hotel.getRooms()) {
            if (room.getRoomId().equals(roomId)) {
                selectedRoom = room;
                break;
            }
        }

        if (selectedRoom == null) {
            throw new IllegalArgumentException("Room not found");
        }

        if (!selectedRoom.isAvailable(checkIn, checkOut)) {
            throw new IllegalStateException("Room is not available");
        }

        String bookingId = "BKG-" + bookingCounter.getAndIncrement();

        Booking booking = new Booking(
                bookingId,
                userId,
                hotel,
                selectedRoom,
                checkIn,
                checkOut
        );

        selectedRoom.addBooking(booking);
        bookings.put(bookingId, booking);

        return booking;
    }

    public synchronized void cancelBooking(String bookingId, String userId) {
        Booking booking = bookings.get(bookingId);

        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }

        if (!booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized cancellation");
        }

        booking.cancel();
        booking.getRoom().removeBooking(booking);
    }

    public Booking getBooking(String bookingId) {
        return bookings.get(bookingId);
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null ||
                !checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("Invalid booking dates");
        }
    }
}

public class Main {
    public static void main(String[] args) {

        BookingSystem bookingSystem = new BookingSystem();

        Hotel hotel1 = new Hotel("H1", "Taj Hotel", "Bangalore");
        Hotel hotel2 = new Hotel("H2", "ITC Hotel", "Mumbai");

        Room room1 = new Room("R101", RoomType.SINGLE, 2000);
        Room room2 = new Room("R102", RoomType.DOUBLE, 3500);
        Room room3 = new Room("R201", RoomType.DELUXE, 5000);

        hotel1.addRoom(room1);
        hotel1.addRoom(room2);
        hotel2.addRoom(room3);

        bookingSystem.addHotel(hotel1);
        bookingSystem.addHotel(hotel2);

        LocalDate checkIn = LocalDate.of(2026, 12, 10);
        LocalDate checkOut = LocalDate.of(2026, 12, 13);

        List<Room> availableRooms =
                bookingSystem.searchAvailableRooms(
                        "H1",
                        RoomType.SINGLE,
                        checkIn,
                        checkOut
                );

        System.out.println("Available rooms: " + availableRooms.size());

        Booking booking = bookingSystem.bookRoom(
                "U101",
                "H1",
                "R101",
                checkIn,
                checkOut
        );

        System.out.println("Booking ID: " + booking.getBookingId());
        System.out.println("Total Price: " + booking.getTotalPrice());
        System.out.println("Status: " + booking.getStatus());

        bookingSystem.cancelBooking(
                booking.getBookingId(),
                "U101"
        );

        System.out.println("Status after cancellation: "
                + booking.getStatus());

        List<Room> availableAfterCancellation =
                bookingSystem.searchAvailableRooms(
                        "H1",
                        RoomType.SINGLE,
                        checkIn,
                        checkOut
                );

        System.out.println(
                "Available after cancellation: "
                        + availableAfterCancellation.size()
        );
    }
}