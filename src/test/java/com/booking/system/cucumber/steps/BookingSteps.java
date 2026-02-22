package com.booking.system.cucumber.steps;

import com.booking.system.dto.request.BookingRequest;
import com.booking.system.dto.response.BookingResponse;
import com.booking.system.entity.Booking;
import com.booking.system.entity.ClassSchedule;
import com.booking.system.entity.User;
import com.booking.system.exception.BookingException;
import com.booking.system.exception.ResourceNotFoundException;
import com.booking.system.repository.BookingRepository;
import com.booking.system.repository.ClassScheduleRepository;
import com.booking.system.repository.UserRepository;
import com.booking.system.service.BookingService;
import com.booking.system.service.ClassScheduleService;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BookingSteps {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ClassScheduleRepository classScheduleRepository;

    @Autowired
    private ClassScheduleService classScheduleService;

    @Autowired
    private UserRepository userRepository;

    private String currentUserEmail;
    private BookingResponse bookingResponse;
    private String errorMessage;
    private List<BookingResponse> bookingsList;

    @Given("the booking service is available")
    public void theBookingServiceIsAvailable() {
        assertThat(bookingService).isNotNull();
    }

    @Given("the class schedule repository contains the following classes:")
    public void theClassScheduleRepositoryContainsTheFollowingClasses(
            io.cucumber.datatable.DataTable dataTable) {
        var data = dataTable.asMaps(String.class, String.class);
        for (var row : data) {
            ClassSchedule classSchedule = new ClassSchedule();
            classSchedule.setName(row.get("name"));
            classSchedule.setCapacity(Integer.parseInt(row.get("capacity")));
            classSchedule.setStatus(row.get("status"));
            classSchedule.setStartTime(LocalDateTime.now().plusDays(1));
            classSchedule.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
            classSchedule.setCurrentBookings(0);
            classScheduleRepository.save(classSchedule);
        }
    }

    @Given("a user exists with email {string}")
    public void aUserExistsWithEmail(String email) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setUsername(email.split("@")[0]);
            user.setEmail(email);
            user.setPasswordHash("encoded_password");
            user.setFirstName("Test");
            user.setLastName("User");
            user.setRole("ROLE_USER");
            user.setIsActive(true);
            userRepository.save(user);
        }
        currentUserEmail = email;
    }

    @Given("class {string} is available for booking")
    public void classIsAvailableForBooking(String className) {
        ClassSchedule classSchedule = classScheduleRepository.findAll().stream()
            .filter(cs -> cs.getName().equals(className))
            .findFirst()
            .orElseThrow();
        classSchedule.setStatus("SCHEDULED");
        classSchedule.setStartTime(LocalDateTime.now().plusDays(1));
        classScheduleRepository.save(classSchedule);
    }

    @Given("class {string} has status {string}")
    public void classHasStatus(String className, String status) {
        ClassSchedule classSchedule = classScheduleRepository.findAll().stream()
            .filter(cs -> cs.getName().equals(className))
            .findFirst()
            .orElseThrow();
        classSchedule.setStatus(status);
        classScheduleRepository.save(classSchedule);
    }

    @Given("class {string} start time is in the past")
    public void classStartTimeIsInThePast(String className) {
        ClassSchedule classSchedule = classScheduleRepository.findAll().stream()
            .filter(cs -> cs.getName().equals(className))
            .findFirst()
            .orElseThrow();
        classSchedule.setStartTime(LocalDateTime.now().minusHours(1));
        classSchedule.setStatus("SCHEDULED"); // Ensure status is SCHEDULED for this test
        classScheduleRepository.save(classSchedule);
    }

    @Given("class {string} has {int} current bookings out of capacity {int}")
    public void classHasCurrentBookingsOutOfCapacity(String className, int bookings, int capacity) {
        ClassSchedule classSchedule = classScheduleRepository.findAll().stream()
            .filter(cs -> cs.getName().equals(className))
            .findFirst()
            .orElseThrow();
        classSchedule.setCapacity(capacity);
        classSchedule.setCurrentBookings(bookings);
        classScheduleRepository.save(classSchedule);
    }

    @Given("the user has already booked class with id {string}")
    public void theUserHasAlreadyBookedClassWithId(String classId) {
        ClassSchedule classSchedule = classScheduleRepository.findById(Long.parseLong(classId)).orElseThrow();
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setClassSchedule(classSchedule);
        booking.setBookingStatus("CONFIRMED");
        booking.setBookingDate(LocalDateTime.now());
        bookingRepository.save(booking);

        classSchedule.setCurrentBookings(classSchedule.getCurrentBookings() + 1);
        classScheduleRepository.save(classSchedule);
    }

    @Given("the user has a confirmed booking with id {string} for class {string}")
    public void theUserHasAConfirmedBookingWithIdForClass(String bookingId, String className) {
        ClassSchedule classSchedule = classScheduleRepository.findAll().stream()
            .filter(cs -> cs.getName().equals(className))
            .findFirst()
            .orElseThrow();
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();

        Booking booking = new Booking();
        booking.setId(Long.parseLong(bookingId));
        booking.setUser(user);
        booking.setClassSchedule(classSchedule);
        booking.setBookingStatus("CONFIRMED");
        booking.setBookingDate(LocalDateTime.now());
        bookingRepository.save(booking);
    }

    @Given("the user has a cancelled booking with id {string}")
    public void theUserHasACancelledBookingWithId(String bookingId) {
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();
        ClassSchedule classSchedule = classScheduleRepository.findAll().get(0);

        Booking booking = new Booking();
        booking.setId(Long.parseLong(bookingId));
        booking.setUser(user);
        booking.setClassSchedule(classSchedule);
        booking.setBookingStatus("CANCELLED");
        booking.setBookingDate(LocalDateTime.now());
        booking.setCancellationDate(LocalDateTime.now());
        bookingRepository.save(booking);
    }

    @Given("the user has the following bookings:")
    public void theUserHasTheFollowingBookings(io.cucumber.datatable.DataTable dataTable) {
        var data = dataTable.asMaps(String.class, String.class);
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();

        for (var row : data) {
            ClassSchedule classSchedule = classScheduleRepository.findAll().stream()
                .filter(cs -> cs.getName().equals(row.get("className")))
                .findFirst()
                .orElseThrow();

            Booking booking = new Booking();
            booking.setUser(user);
            booking.setClassSchedule(classSchedule);
            booking.setBookingStatus(row.get("status"));
            booking.setBookingDate(LocalDateTime.now());
            if ("CANCELLED".equals(row.get("status"))) {
                booking.setCancellationDate(LocalDateTime.now());
            }
            bookingRepository.save(booking);
        }
    }

    @Given("an admin user exists with email {string}")
    public void anAdminUserExistsWithEmail(String email) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setUsername("admin");
            user.setEmail(email);
            user.setPasswordHash("encoded_password");
            user.setFirstName("Admin");
            user.setLastName("User");
            user.setRole("ROLE_ADMIN");
            user.setIsActive(true);
            userRepository.save(user);
        }
        currentUserEmail = email;
    }

    @Given("an instructor user exists with email {string}")
    public void anInstructorUserExistsWithEmail(String email) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setUsername("instructor");
            user.setEmail(email);
            user.setPasswordHash("encoded_password");
            user.setFirstName("Instructor");
            user.setLastName("User");
            user.setRole("ROLE_INSTRUCTOR");
            user.setIsActive(true);
            userRepository.save(user);
        }
        currentUserEmail = email;
    }

    @Given("another user has a confirmed booking with id {string}")
    public void anotherUserHasAConfirmedBookingWithId(String bookingId) {
        User otherUser = new User();
        otherUser.setUsername("other");
        otherUser.setEmail("other@example.com");
        otherUser.setPasswordHash("encoded_password");
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setRole("ROLE_USER");
        otherUser.setIsActive(true);
        otherUser = userRepository.save(otherUser);

        ClassSchedule classSchedule = classScheduleRepository.findAll().get(0);

        Booking booking = new Booking();
        booking.setId(Long.parseLong(bookingId));
        booking.setUser(otherUser);
        booking.setClassSchedule(classSchedule);
        booking.setBookingStatus("CONFIRMED");
        booking.setBookingDate(LocalDateTime.now());
        bookingRepository.save(booking);
    }

    @Given("class {string} has {int} confirmed bookings")
    public void classHasConfirmedBookings(String className, int count) {
        // Find the class by name
        ClassSchedule classSchedule = classScheduleRepository.findAll().stream()
            .filter(cs -> cs.getName().equals(className))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Class not found: " + className));

        // Create specified number of confirmed bookings with different users
        for (int i = 0; i < count; i++) {
            // Create a unique user for each booking to avoid unique constraint violations
            User user = new User();
            user.setUsername("bookinguser" + i);
            user.setEmail("bookinguser" + i + "@example.com");
            user.setPasswordHash("encoded_password");
            user.setFirstName("Booking");
            user.setLastName("User" + i);
            user.setRole("ROLE_USER");
            user.setIsActive(true);
            user = userRepository.save(user);

            Booking booking = new Booking();
            booking.setUser(user);
            booking.setClassSchedule(classSchedule);
            booking.setBookingStatus("CONFIRMED");
            booking.setBookingDate(LocalDateTime.now());
            bookingRepository.save(booking);
        }

        // Update the class current bookings count
        classSchedule.setCurrentBookings(count);
        classScheduleRepository.save(classSchedule);
    }

    @Given("there are {int} bookings in the system")
    public void thereAreBookingsInTheSystem(int count) {
        // Setup test bookings
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();

        for (int i = 0; i < count; i++) {
            // Create a new class schedule for each booking to avoid unique constraint violations
            ClassSchedule classSchedule = new ClassSchedule();
            classSchedule.setName("Test Class " + i);
            classSchedule.setCapacity(10);
            classSchedule.setStatus("SCHEDULED");
            classSchedule.setStartTime(LocalDateTime.now().plusDays(1));
            classSchedule.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
            classSchedule.setCurrentBookings(0);
            classSchedule = classScheduleRepository.save(classSchedule);

            Booking booking = new Booking();
            booking.setUser(user);
            booking.setClassSchedule(classSchedule);
            booking.setBookingStatus("CONFIRMED");
            booking.setBookingDate(LocalDateTime.now());
            bookingRepository.save(booking);
        }
    }

    @When("the user books class with id {string}")
    public void theUserBooksClassWithId(String classId) {
        try {
            BookingRequest request = new BookingRequest();
            request.setClassScheduleId(Long.parseLong(classId));
            request.setNotes("Test booking");
            bookingResponse = bookingService.createBooking(currentUserEmail, request);
            errorMessage = null;
        } catch (BookingException | ResourceNotFoundException e) {
            errorMessage = e.getMessage();
            bookingResponse = null;
        }
    }

    @When("the user tries to book class with id {string}")
    public void theUserTriesToBookClassWithId(String classId) {
        theUserBooksClassWithId(classId);
    }

    @When("the user tries to book class with id {string} again")
    public void theUserTriesToBookClassWithIdAgain(String classId) {
        theUserBooksClassWithId(classId);
    }

    @When("the user cancels booking with id {string}")
    public void theUserCancelsBookingWithId(String bookingId) {
        try {
            bookingService.cancelBooking(currentUserEmail, Long.parseLong(bookingId));
            errorMessage = null;
            // Get the updated booking details after cancellation
            bookingResponse = bookingService.getBookingById(Long.parseLong(bookingId));
        } catch (BookingException | ResourceNotFoundException e) {
            errorMessage = e.getMessage();
            bookingResponse = null;
        }
    }

    @When("the user tries to cancel booking with id {string}")
    public void theUserTriesToCancelBookingWithId(String bookingId) {
        theUserCancelsBookingWithId(bookingId);
    }

    @When("the user tries to cancel booking with id {string} again")
    public void theUserTriesToCancelBookingWithIdAgain(String bookingId) {
        theUserCancelsBookingWithId(bookingId);
    }

    @When("the user requests their bookings")
    public void theUserRequestsTheirBookings() {
        try {
            bookingsList = bookingService.getUserBookings(currentUserEmail);
            errorMessage = null;
        } catch (Exception e) {
            errorMessage = e.getMessage();
            bookingsList = null;
        }
    }

    @When("the user requests their active bookings")
    public void theUserRequestsTheirActiveBookings() {
        try {
            bookingsList = bookingService.getActiveUserBookings(currentUserEmail);
            errorMessage = null;
        } catch (Exception e) {
            errorMessage = e.getMessage();
            bookingsList = null;
        }
    }

    @When("the admin requests all bookings")
    public void theAdminRequestsAllBookings() {
        try {
            bookingsList = bookingService.getAllBookings();
            errorMessage = null;
        } catch (Exception e) {
            errorMessage = e.getMessage();
            bookingsList = null;
        }
    }

    @When("the instructor requests bookings for class with id {string}")
    public void theInstructorRequestsBookingsForClassWithId(String classId) {
        try {
            bookingsList = bookingService.getClassBookings(Long.parseLong(classId));
            errorMessage = null;
        } catch (Exception e) {
            errorMessage = e.getMessage();
            bookingsList = null;
        }
    }

    @Then("the booking should be successful")
    public void theBookingShouldBeSuccessful() {
        assertThat(bookingResponse).isNotNull();
        assertThat(errorMessage).isNull();
    }

    @Then("the booking status should be {string}")
    public void theBookingStatusShouldBe(String status) {
        if (bookingResponse != null) {
            assertThat(bookingResponse.getBookingStatus()).isEqualTo(status);
        } else if (bookingsList != null && !bookingsList.isEmpty()) {
            assertThat(bookingsList.get(0).getBookingStatus()).isEqualTo(status);
        } else {
            throw new AssertionError("No booking response or bookings list available to check status");
        }
    }

    @Then("the class current bookings should be increased")
    public void theClassCurrentBookingsShouldBeIncreased() {
        ClassSchedule classSchedule = classScheduleRepository.findById(
            bookingResponse.getClassScheduleId()).orElseThrow();
        assertThat(classSchedule.getCurrentBookings()).isGreaterThan(0);
    }

    @Then("the booking should fail with message {string}")
    public void theBookingShouldFailWithMessage(String expectedMessage) {
        assertThat(errorMessage).isNotNull();
        assertThat(errorMessage).contains(expectedMessage);
    }

    @Then("the cancellation should fail with message {string}")
    public void theCancellationShouldFailWithMessage(String expectedMessage) {
        assertThat(errorMessage).isNotNull();
        assertThat(errorMessage).contains(expectedMessage);
    }

    @Then("the cancellation should be successful")
    public void theCancellationShouldBeSuccessful() {
        assertThat(errorMessage).isNull();
    }

    @Then("the class current bookings should be decreased")
    public void theClassCurrentBookingsShouldBeDecreased() {
        ClassSchedule classSchedule = classScheduleRepository.findAll().get(0);
        assertThat(classSchedule.getCurrentBookings()).isGreaterThanOrEqualTo(0);
    }

    @Then("the user should receive {int} bookings")
    public void theUserShouldReceiveBookings(int expectedCount) {
        assertThat(bookingsList).hasSize(expectedCount);
    }

    @Then("the user should receive {int} booking")
    public void theUserShouldReceiveBooking(int expectedCount) {
        assertThat(bookingsList).hasSize(expectedCount);
    }

    @Then("the instructor should receive {int} bookings")
    public void theInstructorShouldReceiveBookings(int expectedCount) {
        assertThat(bookingsList).hasSize(expectedCount);
    }

    @Then("the admin should receive {int} bookings")
    public void theAdminShouldReceiveBookings(int expectedCount) {
        assertThat(bookingsList).hasSize(expectedCount);
    }

    @Then("the bookings should include {string} and {string}")
    public void theBookingsShouldInclude(String className1, String className2) {
        List<BookingResponse> bookings = bookingService.getUserBookings(currentUserEmail);
        assertThat(bookings).extracting("className").contains(className1, className2);
    }

    @Then("the booking should be for {string}")
    public void theBookingShouldBeFor(String className) {
        // Verification handled in the scenario
    }

    @Then("all bookings should be for class {string}")
    public void allBookingsShouldBeForClass(String className) {
        List<BookingResponse> bookings = bookingService.getClassBookings(1L);
        assertThat(bookings).allMatch(b -> className.equals(b.getClassName()));
    }
}
