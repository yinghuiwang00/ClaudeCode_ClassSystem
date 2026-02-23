Feature: Class Booking

  Background:
    Given the booking service is available
    And the class schedule repository contains the following classes:
      | id | name         | capacity | status   |
      | 1  | Yoga Class   | 10       | SCHEDULED |
      | 2  | Pilates     | 5        | SCHEDULED |

  Scenario: User books an available class
    Given a user exists with email "user@example.com"
    And class "Yoga Class" is available for booking
    When the user books class with id "1"
    Then the booking should be successful
    And the booking status should be "CONFIRMED"
    And the class current bookings should be increased

  Scenario: User books a full class
    Given a user exists with email "user@example.com"
    And class "Pilates" has 5 current bookings out of capacity 5
    When the user tries to book class with id "2"
    Then the booking should fail with message "Class is full"

  Scenario: User books a cancelled class
    Given a user exists with email "user@example.com"
    And class "Yoga Class" has status "CANCELLED"
    When the user tries to book class with id "1"
    Then the booking should fail with message "Class is not available for booking"

  Scenario: User books a class that has already started
    Given a user exists with email "user@example.com"
    And class "Yoga Class" start time is in the past
    When the user tries to book class with id "1"
    Then the booking should fail with message "Cannot book a class that has already started or passed"

  Scenario: User tries to book the same class twice
    Given a user exists with email "user@example.com"
    And the user has already booked class with id "1"
    When the user tries to book class with id "1" again
    Then the booking should fail with message "You have already booked this class"

  Scenario: User cancels their own booking
    Given a user exists with email "user@example.com"
    And the user has a confirmed booking with id "1" for class "Yoga Class"
    When the user cancels booking with id "1"
    Then the cancellation should be successful
    And the booking status should be "CANCELLED"
    And the class current bookings should be decreased

  Scenario: User tries to cancel another user's booking
    Given a user exists with email "user@example.com"
    And another user has a confirmed booking with id "1"
    When the user tries to cancel booking with id "1"
    Then the cancellation should fail with message "You can only cancel your own bookings"

  Scenario: User tries to cancel already cancelled booking
    Given a user exists with email "user@example.com"
    And the user has a cancelled booking with id "1"
    When the user tries to cancel booking with id "1" again
    Then the cancellation should fail with message "Booking is already cancelled"

  Scenario: User views their bookings
    Given a user exists with email "user@example.com"
    And the user has the following bookings:
      | className   | status    |
      | Yoga Class  | CONFIRMED |
      | Pilates     | CANCELLED |
    When the user requests their bookings
    Then the user should receive 2 bookings
    And the bookings should include "Yoga Class" and "Pilates"

  Scenario: User views only active bookings
    Given a user exists with email "user@example.com"
    And the user has the following bookings:
      | className   | status    |
      | Yoga Class  | CONFIRMED |
      | Pilates     | CANCELLED |
    When the user requests their active bookings
    Then the user should receive 1 booking
    And the booking should be for "Yoga Class"
    And the booking status should be "CONFIRMED"

  Scenario: Admin views all bookings
    Given an admin user exists with email "admin@example.com"
    And there are 3 bookings in the system
    When the admin requests all bookings
    Then the admin should receive 3 bookings

  Scenario: Instructor views class bookings
    Given an instructor user exists with email "instructor@example.com"
    And class "Yoga Class" has 2 confirmed bookings
    When the instructor requests bookings for class with id "1"
    Then the instructor should receive 2 bookings
    And all bookings should be for class "Yoga Class"

  @new
  Scenario: Concurrent booking attempts for last spot
    Given a class exists with id "3" and name "Popular Class" with capacity 1 and 0 current bookings
    And user "user1@example.com" exists and is ready to book
    And user "user2@example.com" exists and is ready to book
    When both users attempt to book class with id "3" concurrently
    Then only one booking should succeed
    And the class current bookings should be 1
    And the other user should receive "Class is full" error

  @new
  Scenario: Concurrent booking and cancellation
    Given a class exists with id "3" and name "Dynamic Class" with capacity 2 and 1 current bookings
    And user "userA@example.com" has a confirmed booking for class with id "3"
    And user "userB@example.com" exists and is ready to book
    When user "userA@example.com" cancels their booking
    And user "userB@example.com" attempts to book class with id "3" concurrently
    Then user "userB@example.com" booking should succeed
    And the class current bookings should remain 1
    And user "userA@example.com" cancellation should succeed
