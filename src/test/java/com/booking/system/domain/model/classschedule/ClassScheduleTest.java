package com.booking.system.domain.model.classschedule;

import com.booking.system.domain.model.shared.Capacity;
import com.booking.system.domain.model.shared.TimeRange;
import com.booking.system.domain.model.shared.Location;
import com.booking.system.domain.model.instructor.Instructor;
import com.booking.system.domain.model.user.User;
import com.booking.system.domain.model.shared.Email;
import com.booking.system.domain.shared.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ClassSchedule Aggregate Root Tests")
class ClassScheduleTest {

    private Instructor instructor;
    private TimeRange futureTimeRange;
    private TimeRange pastTimeRange;
    private TimeRange ongoingTimeRange;
    private Capacity capacity;
    private Location location;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        // 创建测试用户和讲师
        User user = User.create(
            "instructor_user",
            Email.of("instructor@example.com"),
            "John",
            "Doe",
            "passwordHash",
            "ROLE_INSTRUCTOR"
        );
        instructor = Instructor.create(user, "Experienced instructor", "Yoga");

        // 创建时间范围
        futureTimeRange = TimeRange.of(now.plusHours(2), now.plusHours(4));
        pastTimeRange = TimeRange.of(now.minusHours(4), now.minusHours(2));
        ongoingTimeRange = TimeRange.of(now.minusHours(1), now.plusHours(1));

        // 创建容量和位置
        capacity = Capacity.of(20);
        location = Location.of("Room 101, Building A");
    }

    @Test
    @DisplayName("Should create class schedule successfully")
    void shouldCreateClassScheduleSuccessfully() {
        // When
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Beginner yoga class",
            instructor,
            futureTimeRange,
            capacity,
            location
        );

        // Then
        assertThat(classSchedule).isNotNull();
        assertThat(classSchedule.getName()).isEqualTo("Yoga Class");
        assertThat(classSchedule.getDescription()).isEqualTo("Beginner yoga class");
        assertThat(classSchedule.getInstructor()).isEqualTo(instructor);
        assertThat(classSchedule.getTimeRange()).isEqualTo(futureTimeRange);
        assertThat(classSchedule.getCapacity()).isEqualTo(capacity);
        assertThat(classSchedule.getLocation()).isEqualTo(location);
        assertThat(classSchedule.getCurrentBookings()).isEqualTo(0);
        assertThat(classSchedule.getStatus()).isEqualTo("SCHEDULED");
        assertThat(classSchedule.getCreatedAt()).isNotNull();
        assertThat(classSchedule.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when creating with null name")
    void shouldThrowExceptionWhenCreatingWithNullName() {
        // When & Then
        assertThatThrownBy(() -> ClassSchedule.create(
            null,
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            location
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Class name cannot be empty");
    }

    @Test
    @DisplayName("Should throw exception when creating with empty name")
    void shouldThrowExceptionWhenCreatingWithEmptyName() {
        // When & Then
        assertThatThrownBy(() -> ClassSchedule.create(
            "",
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            location
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Class name cannot be empty");
    }

    @Test
    @DisplayName("Should throw exception when creating with null instructor")
    void shouldThrowExceptionWhenCreatingWithNullInstructor() {
        // When & Then
        assertThatThrownBy(() -> ClassSchedule.create(
            "Yoga Class",
            "Description",
            null,
            futureTimeRange,
            capacity,
            location
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Instructor cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when creating with null time range")
    void shouldThrowExceptionWhenCreatingWithNullTimeRange() {
        // When & Then
        assertThatThrownBy(() -> ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            null,
            capacity,
            location
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Time range cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when creating with null capacity")
    void shouldThrowExceptionWhenCreatingWithNullCapacity() {
        // When & Then
        assertThatThrownBy(() -> ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            null,
            location
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Capacity cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when creating with null location")
    void shouldThrowExceptionWhenCreatingWithNullLocation() {
        // When & Then
        assertThatThrownBy(() -> ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Location cannot be null");
    }

    @Test
    @DisplayName("Should update class info successfully")
    void shouldUpdateClassInfoSuccessfully() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Beginner yoga class",
            instructor,
            futureTimeRange,
            capacity,
            location
        );

        LocalDateTime originalUpdatedAt = classSchedule.getUpdatedAt();
        Location newLocation = Location.of("Room 202, Building B");

        // When
        classSchedule.updateInfo("Advanced Yoga", "Advanced yoga class", newLocation);

        // Then
        assertThat(classSchedule.getName()).isEqualTo("Advanced Yoga");
        assertThat(classSchedule.getDescription()).isEqualTo("Advanced yoga class");
        assertThat(classSchedule.getLocation()).isEqualTo(newLocation);
        assertThat(classSchedule.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("Should throw exception when updating with null name")
    void shouldThrowExceptionWhenUpdatingWithNullName() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            location
        );

        // When & Then
        assertThatThrownBy(() -> classSchedule.updateInfo(null, "New description", location))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Class name cannot be empty");
    }

    @Test
    @DisplayName("Should throw exception when updating with empty name")
    void shouldThrowExceptionWhenUpdatingWithEmptyName() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            location
        );

        // When & Then
        assertThatThrownBy(() -> classSchedule.updateInfo("", "New description", location))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Class name cannot be empty");
    }

    @Test
    @DisplayName("Should update time successfully for future class")
    void shouldUpdateTimeSuccessfullyForFutureClass() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            location
        );

        LocalDateTime originalUpdatedAt = classSchedule.getUpdatedAt();
        TimeRange newTimeRange = TimeRange.of(now.plusHours(5), now.plusHours(7));

        // When
        classSchedule.updateTime(newTimeRange);

        // Then
        assertThat(classSchedule.getTimeRange()).isEqualTo(newTimeRange);
        assertThat(classSchedule.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("Should throw exception when updating time for started class")
    void shouldThrowExceptionWhenUpdatingTimeForStartedClass() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            ongoingTimeRange, // 正在进行中的课程
            capacity,
            location
        );

        TimeRange newTimeRange = TimeRange.of(now.plusHours(5), now.plusHours(7));

        // When & Then
        assertThatThrownBy(() -> classSchedule.updateTime(newTimeRange))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Cannot change time for a class that has already started");
    }

    @Test
    @DisplayName("Should update capacity successfully")
    void shouldUpdateCapacitySuccessfully() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            location
        );

        LocalDateTime originalUpdatedAt = classSchedule.getUpdatedAt();
        Capacity newCapacity = Capacity.of(30);

        // When
        classSchedule.updateCapacity(newCapacity);

        // Then
        assertThat(classSchedule.getCapacity()).isEqualTo(newCapacity);
        assertThat(classSchedule.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("Should throw exception when updating capacity less than current bookings")
    void shouldThrowExceptionWhenUpdatingCapacityLessThanCurrentBookings() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            location
        );

        // 预订一些座位
        classSchedule.book();
        classSchedule.book(); // 2个预订

        Capacity newCapacity = Capacity.of(1); // 小于当前预订数

        // When & Then
        assertThatThrownBy(() -> classSchedule.updateCapacity(newCapacity))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("New capacity cannot be less than current bookings");
    }

    @Test
    @DisplayName("Should book class successfully")
    void shouldBookClassSuccessfully() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            location
        );

        LocalDateTime originalUpdatedAt = classSchedule.getUpdatedAt();

        // When
        classSchedule.book();

        // Then
        assertThat(classSchedule.getCurrentBookings()).isEqualTo(1);
        assertThat(classSchedule.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("Should throw exception when booking full class")
    void shouldThrowExceptionWhenBookingFullClass() {
        // Given
        Capacity smallCapacity = Capacity.of(2);
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            smallCapacity,
            location
        );

        // 预订满
        classSchedule.book();
        classSchedule.book(); // 现在已满

        // When & Then
        assertThatThrownBy(() -> classSchedule.book())
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Class is full");
    }

    @Test
    @DisplayName("Should throw exception when booking started class")
    void shouldThrowExceptionWhenBookingStartedClass() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            ongoingTimeRange, // 正在进行中的课程
            capacity,
            location
        );

        // When & Then
        assertThatThrownBy(() -> classSchedule.book())
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Cannot book a class that has already started");
    }

    @Test
    @DisplayName("Should throw exception when booking cancelled class")
    void shouldThrowExceptionWhenBookingCancelledClass() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            location
        );

        classSchedule.cancel();

        // When & Then
        assertThatThrownBy(() -> classSchedule.book())
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Cannot book a class that is not scheduled");
    }

    @Test
    @DisplayName("Should cancel booking successfully")
    void shouldCancelBookingSuccessfully() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            location
        );

        classSchedule.book(); // 先预订
        LocalDateTime originalUpdatedAt = classSchedule.getUpdatedAt();

        // When
        classSchedule.cancelBooking();

        // Then
        assertThat(classSchedule.getCurrentBookings()).isEqualTo(0);
        assertThat(classSchedule.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("Should throw exception when cancelling booking with no bookings")
    void shouldThrowExceptionWhenCancellingBookingWithNoBookings() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            location
        );

        // When & Then
        assertThatThrownBy(() -> classSchedule.cancelBooking())
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("No bookings to cancel");
    }

    @Test
    @DisplayName("Should throw exception when cancelling booking for ended class")
    void shouldThrowExceptionWhenCancellingBookingForEndedClass() throws NoSuchFieldException, IllegalAccessException {
        // Given
        // 创建已结束的课程
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            pastTimeRange, // 已结束的课程
            capacity,
            location
        );

        // 通过反射设置currentBookings = 1，模拟已有预订
        Field currentBookingsField = ClassSchedule.class.getDeclaredField("currentBookings");
        currentBookingsField.setAccessible(true);
        currentBookingsField.set(classSchedule, 1);

        // When & Then
        assertThatThrownBy(() -> classSchedule.cancelBooking())
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Cannot cancel booking for a class that has ended");
    }

    @Test
    @DisplayName("Should cancel class successfully")
    void shouldCancelClassSuccessfully() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            location
        );

        LocalDateTime originalUpdatedAt = classSchedule.getUpdatedAt();

        // When
        classSchedule.cancel();

        // Then
        assertThat(classSchedule.getStatus()).isEqualTo("CANCELLED");
        assertThat(classSchedule.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("Should throw exception when cancelling already started class")
    void shouldThrowExceptionWhenCancellingAlreadyStartedClass() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            ongoingTimeRange, // 正在进行中的课程
            capacity,
            location
        );

        // When & Then
        assertThatThrownBy(() -> classSchedule.cancel())
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Cannot cancel a class that has already started");
    }

    @Test
    @DisplayName("Should complete class successfully")
    void shouldCompleteClassSuccessfully() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            pastTimeRange, // 已结束的课程
            capacity,
            location
        );

        LocalDateTime originalUpdatedAt = classSchedule.getUpdatedAt();

        // When
        classSchedule.complete();

        // Then
        assertThat(classSchedule.getStatus()).isEqualTo("COMPLETED");
        assertThat(classSchedule.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("Should throw exception when completing not ended class")
    void shouldThrowExceptionWhenCompletingNotEndedClass() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange, // 未开始的课程
            capacity,
            location
        );

        // When & Then
        assertThatThrownBy(() -> classSchedule.complete())
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Class must have ended to be marked as completed");
    }

    @Test
    @DisplayName("Should throw exception when completing cancelled class")
    void shouldThrowExceptionWhenCompletingCancelledClass() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            location
        );

        classSchedule.cancel();

        // When & Then
        assertThatThrownBy(() -> classSchedule.complete())
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Only scheduled classes can be completed");
    }

    @Test
    @DisplayName("Should check if class is full")
    void shouldCheckIfClassIsFull() {
        // Given
        Capacity smallCapacity = Capacity.of(1);
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            smallCapacity,
            location
        );

        assertThat(classSchedule.isFull()).isFalse();

        // When
        classSchedule.book();

        // Then
        assertThat(classSchedule.isFull()).isTrue();
    }

    @Test
    @DisplayName("Should get remaining seats")
    void shouldGetRemainingSeats() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            location
        );

        assertThat(classSchedule.getRemainingSeats()).isEqualTo(20);

        // When
        classSchedule.book();

        // Then
        assertThat(classSchedule.getRemainingSeats()).isEqualTo(19);
    }

    @Test
    @DisplayName("Should check time status")
    void shouldCheckTimeStatus() {
        // Given
        ClassSchedule futureClass = ClassSchedule.create(
            "Future Class",
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            location
        );

        ClassSchedule pastClass = ClassSchedule.create(
            "Past Class",
            "Description",
            instructor,
            pastTimeRange,
            capacity,
            location
        );

        ClassSchedule ongoingClass = ClassSchedule.create(
            "Ongoing Class",
            "Description",
            instructor,
            ongoingTimeRange,
            capacity,
            location
        );

        // Then
        assertThat(futureClass.hasStarted()).isFalse();
        assertThat(futureClass.hasEnded()).isFalse();
        assertThat(futureClass.isInProgress()).isFalse();

        assertThat(pastClass.hasStarted()).isTrue();
        assertThat(pastClass.hasEnded()).isTrue();
        assertThat(pastClass.isInProgress()).isFalse();

        assertThat(ongoingClass.hasStarted()).isTrue();
        assertThat(ongoingClass.hasEnded()).isFalse();
        assertThat(ongoingClass.isInProgress()).isTrue();
    }

    @Test
    @DisplayName("Should get duration in minutes")
    void shouldGetDurationInMinutes() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            location
        );

        // When
        long duration = classSchedule.getDurationInMinutes();

        // Then
        assertThat(duration).isEqualTo(120); // 2小时 = 120分钟
    }

    @ParameterizedTest
    @ValueSource(ints = {30, 15, 5})
    @DisplayName("Should check if starting soon")
    void shouldCheckIfStartingSoon(int minutesBefore) {
        // Given
        LocalDateTime start = now.plusMinutes(minutesBefore);
        LocalDateTime end = start.plusHours(1);
        TimeRange timeRange = TimeRange.of(start, end);

        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            timeRange,
            capacity,
            location
        );

        // When
        boolean isStartingSoon = classSchedule.isStartingSoon(30);

        // Then
        if (minutesBefore <= 30) {
            assertThat(isStartingSoon).isTrue();
        } else {
            // 注意：TimeRange.isStartingSoon的逻辑是：如果现在时间 >= (开始时间 - 分钟数)，则返回true
            // 对于minutesBefore > 30的情况，应该返回false
            assertThat(isStartingSoon).isFalse();
        }
    }

    @Test
    @DisplayName("Should have correct string representation")
    void shouldHaveCorrectStringRepresentation() {
        // Given
        ClassSchedule classSchedule = ClassSchedule.create(
            "Yoga Class",
            "Description",
            instructor,
            futureTimeRange,
            capacity,
            location
        );
        classSchedule.setId(1L);
        classSchedule.book(); // 1个预订

        // When
        String toString = classSchedule.toString();

        // Then
        assertThat(toString).contains("ClassSchedule{id=1")
                            .contains("name='Yoga Class'")
                            .contains("status='SCHEDULED'")
                            .contains("bookings=1/20");
    }
}