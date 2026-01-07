package com.sep490.backendclubmanagement.security;

import com.sep490.backendclubmanagement.entity.User;
import com.sep490.backendclubmanagement.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImp implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("Username cannot be null or empty");
        }

        // Find user in database
        var userOpt = userService.findByEmail(username.trim());
        if (userOpt.isEmpty()) {
            log.warn("User not found in database: {}", username);
            throw new UsernameNotFoundException("User not found: " + username);
        }

        User user = userOpt.get();
        
        // Check if user is active
        if (!user.getIsActive()) {
            log.warn("User is inactive: {}", username);
            throw new UsernameNotFoundException("User is inactive: " + username);
        }

        // Get system role
        String systemRole = "STUDENT"; // Default role
        if (user.getSystemRole() != null) {
            systemRole = user.getSystemRole().getRoleName();
        }

        log.debug("User loaded successfully: {} with role: {}", username, systemRole);

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_" + systemRole))
        );
    }
}


