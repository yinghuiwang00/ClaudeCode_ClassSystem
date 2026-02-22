Feature: User Authentication

  Background:
    Given the authentication service is available
    And the user repository is empty

  Scenario: User registration with valid data
    When the user registers with the following details:
      | username | email              | password | firstName | lastName |
      | john_doe | john@example.com   | pass123  | John      | Doe      |
    Then the registration should be successful
    And the user should receive a JWT token
    And the user should have role "ROLE_USER"
    And the user should be able to login

  Scenario: User registration with existing email
    Given a user already exists with email "existing@example.com"
    When the user tries to register with email "existing@example.com"
    Then the registration should fail with message "Email already exists"

  Scenario: User registration with existing username
    Given a user already exists with username "john_doe"
    When the user tries to register with username "john_doe"
    Then the registration should fail with message "Username already exists"

  Scenario: User registration with invalid email format
    When the user tries to register with the following details:
      | username | email           | password | firstName | lastName |
      | john_doe | invalid-email   | pass123  | John      | Doe      |
    Then the registration should fail with validation error
    And the error should be related to "email"

  Scenario: User registration with missing required fields
    When the user tries to register with the following details:
      | username | email              | password | firstName | lastName |
      |          | john@example.com   |          | John      |          |
    Then the registration should fail with validation error
    And the errors should be related to "username" and "password" and "lastName"

  Scenario: User login with valid credentials
    Given a user exists with email "john@example.com" and password "pass123"
    When the user logs in with email "john@example.com" and password "pass123"
    Then the login should be successful
    And the user should receive a JWT token

  Scenario: User login with invalid credentials
    Given a user exists with email "john@example.com" and password "pass123"
    When the user tries to log in with email "john@example.com" and password "wrongpass"
    Then the login should fail

  Scenario: User login with non-existent email
    When the user tries to log in with email "nonexistent@example.com" and password "pass123"
    Then the login should fail

  Scenario: User login with missing password
    When the user tries to log in with email "john@example.com" and empty password
    Then the login should fail with validation error
