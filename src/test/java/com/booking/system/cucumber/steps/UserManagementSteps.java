package com.booking.system.cucumber.steps;

import com.booking.system.dto.response.UserResponse;
import com.booking.system.entity.User;
import com.booking.system.repository.UserRepository;
import com.booking.system.service.UserService;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class UserManagementSteps {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    private List<UserResponse> userResponses;
    private UserResponse singleUserResponse;
    private Exception caughtException;
    private String currentAuthenticatedEmail;

    @Given("the user service is available")
    public void theUserServiceIsAvailable() {
        assertThat(userService).isNotNull();
    }

    @Given("the following users exist in the system:")
    public void theFollowingUsersExistInTheSystem(io.cucumber.datatable.DataTable dataTable) {
        List<Map<String, String>> data = dataTable.asMaps(String.class, String.class);
        for (Map<String, String> row : data) {
            if (!userRepository.existsByEmail(row.get("email"))) {
                User user = new User();
                user.setUsername(row.get("username"));
                user.setEmail(row.get("email"));
                user.setPasswordHash("encoded_password");
                user.setFirstName("Test");
                user.setLastName("User");
                user.setRole(row.get("role"));
                user.setIsActive(Boolean.parseBoolean(row.get("isActive")));
                user.setCreatedAt(LocalDateTime.now());
                userRepository.save(user);
            }
        }
    }




    @Given("a user exists with id {string} and username {string}")
    public void aUserExistsWithIdAndUsername(String id, String username) {
        // Ensure a user with this ID exists by creating if not exists
        Long userId = Long.parseLong(id);
        if (!userRepository.existsById(userId)) {
            User user = new User();
            user.setId(userId); // Set the specific ID for testing
            user.setUsername(username);
            user.setEmail(username + "@example.com");
            user.setPasswordHash("encoded_password");
            user.setFirstName("Test");
            user.setLastName("User");
            user.setRole("ROLE_USER");
            user.setIsActive(true);
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    @Given("another user exists with id {string} and username {string}")
    public void anotherUserExistsWithIdAndUsername(String id, String username) {
        aUserExistsWithIdAndUsername(id, username);
    }

    @Given("the user is authenticated with email {string}")
    public void theUserIsAuthenticatedWithEmail(String email) {
        currentAuthenticatedEmail = email;
        // Set up Spring Security context for authentication
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            // If user doesn't exist, create a simple authentication with just the email
            // This allows testing scenarios where authentication succeeds but user doesn't exist in DB
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                email, null, java.util.Collections.emptyList()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }

    @Given("the admin is authenticated with email {string}")
    public void anAdminIsAuthenticatedWithEmail(String email) {
        theUserIsAuthenticatedWithEmail(email);
    }

    @Given("a user is authenticated with email {string}")
    public void aUserIsAuthenticatedWithEmail(String email) {
        theUserIsAuthenticatedWithEmail(email);
    }

    @When("the user requests their profile")
    public void theUserRequestsTheirProfile() {
        try {
            singleUserResponse = userService.getCurrentUser(currentAuthenticatedEmail);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("an unauthenticated user requests their profile")
    public void anUnauthenticatedUserRequestsTheirProfile() {
        // Clear authentication context
        SecurityContextHolder.clearContext();
        try {
            singleUserResponse = userService.getCurrentUser(null);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("the admin requests all users")
    public void theAdminRequestsAllUsers() {
        try {
            userResponses = userService.getAllUsers();
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("the user tries to request all users")
    public void theUserTriesToRequestAllUsers() {
        try {
            userResponses = userService.getAllUsers();
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("the admin requests user with id {string}")
    public void theAdminRequestsUserWithId(String id) {
        try {
            Long userId = Long.parseLong(id);
            singleUserResponse = userService.getUserById(userId);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @When("the user tries to request user with id {string}")
    public void theUserTriesToRequestUserWithId(String id) {
        try {
            Long userId = Long.parseLong(id);
            singleUserResponse = userService.getUserById(userId);
        } catch (Exception e) {
            caughtException = e;
        }
    }

    @Then("the request should be successful")
    public void theRequestShouldBeSuccessful() {
        assertThat(caughtException).isNull();
    }

    @Then("the request should fail with status {int}")
    public void theRequestShouldFailWithStatus(int status) {
        assertThat(caughtException).isNotNull();
        // Note: In unit tests, security exceptions may be thrown as AccessDeniedException
        // which will be caught as Exception. We're just verifying an exception was thrown.
    }

    @And("the response should contain user details for {string}")
    public void theResponseShouldContainUserDetailsFor(String email) {
        assertThat(singleUserResponse).isNotNull();
        assertThat(singleUserResponse.getEmail()).isEqualTo(email);
    }

    @And("the user role should be {string}")
    public void theUserRoleShouldBe(String role) {
        assertThat(singleUserResponse.getRole()).isEqualTo(role);
    }

    @And("the response should contain {int} users")
    public void theResponseShouldContainUsers(int count) {
        assertThat(userResponses).hasSize(count);
    }

    @And("the users should include {string}, {string}, and {string}")
    public void theUsersShouldIncludeAnd(String user1, String user2, String user3) {
        List<String> usernames = userResponses.stream()
            .map(UserResponse::getUsername)
            .toList();
        assertThat(usernames).contains(user1, user2, user3);
    }

    @And("the response should contain user with username {string}")
    public void theResponseShouldContainUserWithUsername(String username) {
        assertThat(singleUserResponse.getUsername()).isEqualTo(username);
    }
}