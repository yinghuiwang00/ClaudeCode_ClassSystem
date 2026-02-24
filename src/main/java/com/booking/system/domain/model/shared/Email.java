package com.booking.system.domain.model.shared;

import com.booking.system.domain.shared.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Email值对象
 * 封装邮箱地址验证和规范化逻辑
 */
@Embeddable
@Getter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Email extends ValueObject {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    @Column(name = "email", nullable = false, length = 100)
    private String value;

    private Email(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        String normalizedEmail = normalize(value);
        if (!isValid(normalizedEmail)) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }

        this.value = normalizedEmail;
    }

    /**
     * 工厂方法创建Email值对象
     */
    public static Email of(String email) {
        return new Email(email);
    }

    /**
     * 验证邮箱格式
     */
    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 规范化邮箱地址（转为小写，去除首尾空格）
     */
    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    /**
     * 获取邮箱的本地部分（@之前的部分）
     */
    public String getLocalPart() {
        int atIndex = value.indexOf('@');
        return value.substring(0, atIndex);
    }

    /**
     * 获取邮箱的域名部分（@之后的部分）
     */
    public String getDomain() {
        int atIndex = value.indexOf('@');
        return value.substring(atIndex + 1);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    protected List<Object> getEqualityComponents() {
        return asList(value);
    }
}