Feature: Class Schedule Management

  Background:
    Given the class schedule service is available

  Scenario: Admin creates a new class
    Given an admin user exists with email "admin@example.com"
    When the admin creates a class with the following details:
      | name        | Description      | capacity | location  |
      | Yoga        | Beginner yoga    | 15       | Studio A  |
    Then the class should be created successfully
    And the class status should be "SCHEDULED"
    And the class should have 0 current bookings

  Scenario: Instructor creates a new class
    Given an instructor user exists with email "instructor@example.com"
    When the instructor creates a class with the following details:
      | name        | Description       | capacity | location  |
      | Pilates     | Advanced pilates | 10       | Studio B  |
    Then the class should be created successfully

  Scenario: Regular user tries to create a class
    Given a regular user exists with email "user@example.com"
    When the user tries to create a class with name "Test Class"
    Then the class creation should fail with unauthorized status

  Scenario: Create class with invalid time range
    Given an admin user exists with email "admin@example.com"
    When the admin tries to create a class with end time before start time
    Then the class creation should fail with message "End time must be after start time"

  Scenario: Create class with non-existent instructor
    Given an admin user exists with email "admin@example.com"
    When the admin tries to create a class with instructor id "999"
    Then the class creation should fail with message "Instructor not found"

  Scenario: Update class details
    Given an admin user exists with email "admin@example.com"
    And a class exists with id "1" and name "Yoga Class"
    When the admin updates the class with:
      | name    | capacity |
      | Advanced Yoga | 20  |
    Then the update should be successful
    And the class name should be "Advanced Yoga"
    And the class capacity should be 20

  Scenario: Update class capacity below current bookings
    Given an admin user exists with email "admin@example.com"
    And a class exists with id "1" with capacity 15 and 10 current bookings
    When the admin tries to update the class capacity to 5
    Then the update should fail with message "Cannot reduce capacity below current bookings"

  Scenario: Update class instructor
    Given an admin user exists with email "admin@example.com"
    And a class exists with id "1"
    And an instructor exists with id "2"
    When the admin updates the class with instructor id "2"
    Then the update should be successful
    And the class should have instructor with id "2"

  Scenario: Delete class without bookings
    Given an admin user exists with email "admin@example.com"
    And a class exists with id "1" with 0 current bookings
    When the admin deletes class with id "1"
    Then the class should be deleted successfully
    And the class should not exist in the system

  Scenario: Cancel class with bookings
    Given an admin user exists with email "admin@example.com"
    And a class exists with id "1" with 5 current bookings
    When the admin deletes class with id "1"
    Then the class should be cancelled
    And the class status should be "CANCELLED"
    And the class should still exist in the system

  Scenario: Regular user tries to update a class
    Given a regular user exists with email "user@example.com"
    And a class exists with id "1"
    When the user tries to update the class with name "Updated Class"
    Then the update should fail with forbidden status

  Scenario: Regular user tries to delete a class
    Given a regular user exists with email "user@example.com"
    And a class exists with id "1"
    When the user tries to delete class with id "1"
    Then the deletion should fail with forbidden status

  Scenario: Get all classes
    Given the following classes exist:
      | id | name        | status   |
      | 1  | Yoga        | SCHEDULED |
      | 2  | Pilates     | SCHEDULED |
      | 3  | Zumba       | CANCELLED |
    When a user requests all classes
    Then the user should receive 3 classes

  Scenario: Get available classes only
    Given the following classes exist:
      | id | name        | capacity | currentBookings | status   |
      | 1  | Yoga        | 10       | 5               | SCHEDULED |
      | 2  | Pilates     | 5        | 5               | SCHEDULED |
      | 3  | Zumba       | 15       | 3               | CANCELLED |
    When a user requests available classes
    Then the user should receive 1 class
    And the class should be "Yoga"

  Scenario: Get classes by status
    Given the following classes exist:
      | id | name        | status   |
      | 1  | Yoga        | SCHEDULED |
      | 2  | Pilates     | SCHEDULED |
      | 3  | Zumba       | CANCELLED |
    When a user requests classes with status "CANCELLED"
    Then the user should receive 1 class
    And the class should be "Zumba"

  Scenario: Get classes by instructor
    Given an instructor exists with id "1" and name "John Doe"
    And the following classes exist:
      | id | name        | instructorId |
      | 1  | Yoga        | 1            |
      | 2  | Pilates     | 1            |
      | 3  | Zumba       | 2            |
    When a user requests classes for instructor "1"
    Then the user should receive 2 classes
    And all classes should be by instructor "John Doe"

  Scenario: Get class by ID
    Given a class exists with id "1" and name "Yoga Class"
    When a user requests class with id "1"
    Then the user should receive the class details
    And the class name should be "Yoga Class"

  Scenario: Get non-existent class
    When a user requests class with id "999"
    Then the request should fail with not found status
