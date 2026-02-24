package com.booking.system.service;

import com.booking.system.dto.request.LoginRequest;
import com.booking.system.dto.request.RegisterRequest;
import com.booking.system.dto.response.AuthResponse;
import com.booking.system.entity.User;
import com.booking.system.exception.AuthenticationException;
import com.booking.system.repository.UserRepository;
import com.booking.system.security.JwtTokenProvider;
import com.booking.system.domain.service.AuthDomainService;
import com.booking.system.infrastructure.adapters.UserAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AuthDomainService authDomainService;

    @Autowired
    private UserAdapter userAdapter;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 使用AuthDomainService注册用户
        com.booking.system.domain.model.user.User domainUser;
        try {
            domainUser = authDomainService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName()
            );
        } catch (com.booking.system.domain.shared.DomainException e) {
            // 将领域异常转换为应用层异常
            throw new AuthenticationException(e.getMessage());
        }

        // 通过UserAdapter转换为旧实体User以进行后续处理
        User legacyUser = userAdapter.toLegacy(domainUser);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String token = tokenProvider.generateToken(authentication);

        return new AuthResponse(token, legacyUser.getEmail(), legacyUser.getUsername(), legacyUser.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String token = tokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        return new AuthResponse(token, user.getEmail(), user.getUsername(), user.getRole());
    }
}
