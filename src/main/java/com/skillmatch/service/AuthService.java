package com.skillmatch.service;

import com.skillmatch.dto.request.LoginRequest;
import com.skillmatch.dto.request.RegisterRequest;
import com.skillmatch.dto.response.AuthResponse;
import com.skillmatch.entity.User;
import com.skillmatch.repository.UserRepository;
import com.skillmatch.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final StatsService statsService;

    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        String token = jwtUtil.generateToken(authentication);

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        statsService.updateLoginStreak(user, LocalDate.now());

        return new AuthResponse(token, user.getUserId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getUserType(), user.getIsPaid());
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        // Check if user already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }

        // Create new user
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        String phone = registerRequest.getPhone();
        if (phone != null && phone.isBlank()) {
            phone = null;
        }
        user.setPhone(phone);
        user.setUserType(registerRequest.getUserType());
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        // Initialize streak on first login day (optional)
        statsService.updateLoginStreak(savedUser, LocalDate.now());

        // Generate token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(registerRequest.getEmail(), registerRequest.getPassword())
        );

        String token = jwtUtil.generateToken(authentication);

        return new AuthResponse(token, savedUser.getUserId(), savedUser.getEmail(),
                savedUser.getFirstName(), savedUser.getLastName(), savedUser.getUserType(), savedUser.getIsPaid());
    }
}
