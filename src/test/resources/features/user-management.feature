Feature: User Management

  Background:
    Given the user service is available
    And the following users exist in the system:
      | username | email              | role       | isActive |
      | john_doe | john@example.com   | ROLE_USER  | true     |
      | admin_j  | admin@example.com  | ROLE_ADMIN | true     |
      | instr_m  | instructor@example.com | ROLE_INSTRUCTOR | true |

  Scenario: User gets their own profile
    Given a user exists with email "john@example.com"
    And the user is authenticated with email "john@example.com"
    When the user requests their profile
    Then the request should be successful
    And the response should contain user details for "john@example.com"
    And the user role should be "ROLE_USER"

  Scenario: Admin gets all users
    Given an admin user exists with email "admin@example.com"
    And the admin is authenticated with email "admin@example.com"
    When the admin requests all users
    Then the request should be successful
    And the response should contain 3 users
    And the users should include "john_doe", "admin_j", and "instr_m"

  Scenario: Admin gets specific user by ID
    Given an admin user exists with email "admin@example.com"
    And the admin is authenticated with email "admin@example.com"
    And a user exists with id "1" and username "john_doe"
    When the admin requests user with id "1"
    Then the request should be successful
    And the response should contain user with username "john_doe"

  Scenario: Admin gets non-existent user
    Given an admin user exists with email "admin@example.com"
    And the admin is authenticated with email "admin@example.com"
    When the admin requests user with id "999"
    Then the request should fail with status 404

  Scenario: User gets profile with non-existent authenticated email
    Given a user is authenticated with email "nonexistent@example.com"
    When the user requests their profile
    Then the request should fail with status 404