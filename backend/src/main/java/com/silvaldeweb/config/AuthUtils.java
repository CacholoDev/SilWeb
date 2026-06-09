package com.silvaldeweb.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;

public final class AuthUtils {

    private AuthUtils() {
    }

    public static String currentEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("No authenticated user.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return authentication.getName();
    }

    public static User currentUser(Authentication authentication, UserRepository userRepository) {
        String email = currentEmail(authentication);
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "Authenticated user not found in DB: " + email));
    }

    public static boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + Role.ADMIN.name()));
    }
}
