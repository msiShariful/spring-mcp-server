package com.islamshariful.spring_mcp.user;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Returns the user logged in with an API key (see {@code ApiKeyAuthenticationFilter}).
 * Refuses anonymous callers, which is what protects the task tools.
 */
@Component
public class SecurityUserProvider implements CurrentUserProvider {

    @Override
    public String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationRequiredException();
        }
        return authentication.getName();
    }

    /** Shown to the AI client as the tool error message. */
    public static class AuthenticationRequiredException extends RuntimeException {
        public AuthenticationRequiredException() {
            super("Authentication required: this tool needs an API key. "
                    + "Send it as the header 'Authorization: Bearer <your-api-key>'.");
        }
    }
}
