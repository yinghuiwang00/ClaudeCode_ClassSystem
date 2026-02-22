package com.booking.system.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException")
    void shouldHandleResourceNotFoundException() {
        // Given
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found with id: 1");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/v1/users/1");

        // When
        var response = globalExceptionHandler.handleResourceNotFoundException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).contains("User not found");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/users/1");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle BookingException")
    void shouldHandleBookingException() {
        // Given
        BookingException ex = new BookingException("Booking error");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/v1/bookings");

        // When
        var response = globalExceptionHandler.handleBookingException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Booking Error");
        assertThat(response.getBody().getMessage()).contains("Booking error");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/bookings");
    }

    @Test
    @DisplayName("Should handle AuthenticationException")
    void shouldHandleAuthenticationException() {
        // Given
        AuthenticationException ex = new AuthenticationException("Auth error");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/v1/auth/login");

        // When
        var response = globalExceptionHandler.handleAuthenticationException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Authentication Error");
        assertThat(response.getBody().getMessage()).contains("Auth error");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/auth/login");
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with single field error")
    void shouldHandleMethodArgumentNotValidExceptionWithSingleFieldError() {
        // Given
        FieldError fieldError = new FieldError("objectName", "email", "must be a valid email");
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(Collections.singletonList(fieldError));

        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/v1/auth/register");

        // When
        var response = globalExceptionHandler.handleValidationExceptions(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("errors")).isNotNull();
        assertThat(body.get("status")).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(body.get("error")).isEqualTo("Validation Failed");
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with multiple field errors")
    void shouldHandleMethodArgumentNotValidExceptionWithMultipleFieldErrors() {
        // Given
        FieldError fieldError1 = new FieldError("objectName", "email", "must be a valid email");
        FieldError fieldError2 = new FieldError("objectName", "password", "must not be blank");
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(java.util.List.of(fieldError1, fieldError2));

        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/v1/auth/register");

        // When
        var response = globalExceptionHandler.handleValidationExceptions(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("errors")).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertThat(errors).hasSize(2);
    }

    @Test
    @DisplayName("Should handle generic Exception")
    void shouldHandleGenericException() {
        // Given
        Exception ex = new RuntimeException("Unexpected error");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/v1/test");

        // When
        var response = globalExceptionHandler.handleGlobalException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).contains("Unexpected error");
    }

    @Test
    @DisplayName("Should include timestamp in error response")
    void shouldIncludeTimestampInErrorResponse() {
        // Given
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/v1/test");

        // When
        var response = globalExceptionHandler.handleResourceNotFoundException(ex, request);

        // Then
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should extract correct path from WebRequest description")
    void shouldExtractCorrectPathFromWebRequestDescription() {
        // Given
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/v1/users/123?param=value");

        // When
        var response = globalExceptionHandler.handleResourceNotFoundException(ex, request);

        // Then
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/users/123?param=value");
    }

    @Test
    @DisplayName("Should return 404 status code for ResourceNotFoundException")
    void shouldReturn404StatusCodeForResourceNotFoundException() {
        // Given
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/v1/users/1");

        // When
        var response = globalExceptionHandler.handleResourceNotFoundException(ex, request);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("Should return 400 status code for BookingException")
    void shouldReturn400StatusCodeForBookingException() {
        // Given
        BookingException ex = new BookingException("Booking error");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/v1/bookings");

        // When
        var response = globalExceptionHandler.handleBookingException(ex, request);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should return 401 status code for AuthenticationException")
    void shouldReturn401StatusCodeForAuthenticationException() {
        // Given
        AuthenticationException ex = new AuthenticationException("Auth error");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/v1/auth/login");

        // When
        var response = globalExceptionHandler.handleAuthenticationException(ex, request);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("Should return 500 status code for generic Exception")
    void shouldReturn500StatusCodeForGenericException() {
        // Given
        Exception ex = new RuntimeException("Internal error");
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/v1/test");

        // When
        var response = globalExceptionHandler.handleGlobalException(ex, request);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    @Test
    @DisplayName("Should include timestamp in validation error response")
    void shouldIncludeTimestampInValidationErrorResponse() {
        // Given
        FieldError fieldError = new FieldError("objectName", "email", "must be a valid email");
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(Collections.singletonList(fieldError));

        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/v1/auth/register");

        // When
        var response = globalExceptionHandler.handleValidationExceptions(ex, request);

        // Then
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("timestamp")).isNotNull();
    }
}
