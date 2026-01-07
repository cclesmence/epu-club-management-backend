package com.sep490.backendclubmanagement.service.user;

import com.sep490.backendclubmanagement.entity.SystemRole;
import com.sep490.backendclubmanagement.entity.User;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.repository.SystemRoleRepository;
import com.sep490.backendclubmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class
UserService {

    private final UserRepository userRepository;
    private final SystemRoleRepository systemRoleRepository;


    public Long getCurrentUserId() throws AppException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        Object principal = authentication.getPrincipal();
        log.info("Current principal: {}", principal);
        if (principal instanceof String && "anonymousUser".equals(principal)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (principal instanceof UserDetails userDetails) {
            String email = userDetails.getUsername();
            return userRepository.findByEmail(email)
                    .map(User::getId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }

        if (principal instanceof String email) {
            return userRepository.findByEmail(email)
                    .map(User::getId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }

        throw new RuntimeException("Cannot extract current user info");
    }

    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String email && !"anonymousUser".equals(email)) {
            return email;
        }

        throw new RuntimeException("Cannot extract current user email");
    }

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.trim());
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return userRepository.findById(id);
    }
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    /**
     * Save user
     */
    @Transactional
    public User save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        return userRepository.save(user);
    }


    /**
     * Update user information
     */
    @Transactional
    public User updateUser(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User and user ID cannot be null");
        }

        Optional<User> existingUser = findById(user.getId());
        if (existingUser.isEmpty()) {
            throw new IllegalArgumentException("User with ID " + user.getId() + " not found");
        }

        return save(user);
    }

    /**
     * Activate/deactivate user
     */
    @Transactional
    public User setUserActiveStatus(Long userId, boolean isActive) {
        Optional<User> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User with ID " + userId + " not found");
        }

        User user = userOpt.get();
        user.setIsActive(isActive);
        return save(user);
    }

    /**
     * Update system roles for users
     */
    @Transactional
    public User updateUserSystemRole(Long userId, String systemRoleName) {
        Optional<User> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User with ID " + userId + " not found");
        }

        SystemRole systemRole = findOrCreateSystemRole(systemRoleName);
        User user = userOpt.get();
        user.setSystemRole(systemRole);
        return save(user);
    }

    /**
     * Get all users
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Get all active users
     */
    public List<User> findAllActive() {
        return userRepository.findAll().stream()
                .filter(User::getIsActive)
                .toList();
    }

    /**
     * Find or create a system role
     */
    private SystemRole findOrCreateSystemRole(String roleName) {
        final String finalRoleName = (roleName == null || roleName.trim().isEmpty()) ? "STUDENT" : roleName.trim();

        return systemRoleRepository.findByRoleName(roleName.trim())
                .orElseGet(() -> {
                    SystemRole newRole = SystemRole.builder()
                            .roleName(finalRoleName.trim())
                            .description("System role for " + finalRoleName.toLowerCase())
                            .build();
                    return systemRoleRepository.save(newRole);
                });
    }

    /**
     * Check if user exists and is active
     */
    public boolean isUserActive(String email) {
        Optional<User> userOpt = findByEmail(email);
        return userOpt.isPresent() && userOpt.get().getIsActive();
    }

    /**
     * Get user system role
     */
    public Optional<String> getUserSystemRole(String email) {
        Optional<User> userOpt = findByEmail(email);
        if (userOpt.isPresent() && userOpt.get().getSystemRole() != null) {
            return Optional.of(userOpt.get().getSystemRole().getRoleName());
        }
        return Optional.empty();
    }
    @Transactional(readOnly = true)
    public Long getIdByEmail(String email) {
        return userRepository.findIdByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}

