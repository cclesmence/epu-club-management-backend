package com.sep490.backendclubmanagement.util;

import com.sep490.backendclubmanagement.service.user.UserService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        SecurityUtils.applicationContext = context;
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        // Debug: Log để xem principal là gì
        System.out.println("Principal type: " + principal.getClass().getName());
        System.out.println("Principal: " + principal);

        // Kiểm tra nếu là anonymous user
        if (principal instanceof String && "anonymousUser".equals(principal)) {
            throw new RuntimeException("User not authenticated - please login first");
        }

        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            System.out.println("Username: " + userDetails.getUsername());

            // Kiểm tra nếu là anonymous user
            if ("anonymousUser".equals(userDetails.getUsername())) {
                throw new RuntimeException("User not authenticated - please login first");
            }

            // JWT token chứa email, cần tìm user ID từ email
            String email = userDetails.getUsername();
            System.out.println("Looking up user by email: " + email);

            try {
                UserService userService = applicationContext.getBean(UserService.class);
                return userService.findByEmail(email)
                        .map(user -> {
                            System.out.println("Found user ID: " + user.getId());
                            return user.getId();
                        })
                        .orElseThrow(() -> new RuntimeException("User not found for email: " + email));
            } catch (Exception e) {
                throw new RuntimeException("Error finding user by email: " + email, e);
            }
        }


        // Nếu principal là String (username)
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid user ID format: " + principal);
            }
        }

        throw new RuntimeException("Unable to get current user ID. Principal type: " + principal.getClass().getName());
    }
}
