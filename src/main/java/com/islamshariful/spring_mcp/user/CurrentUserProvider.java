package com.islamshariful.spring_mcp.user;

/**
 * Tells the tools WHO is making the request, so each user only sees their own data.
 *
 * <p>Today this returns a fixed demo user ({@link DemoUserProvider}).
 * When you add authentication (e.g. Spring Security with OAuth2 or an API key),
 * create a new implementation that reads the logged-in user, for example:
 *
 * <pre>{@code
 * @Component
 * @Primary
 * public class SecurityUserProvider implements CurrentUserProvider {
 *     public String currentUserId() {
 *         return SecurityContextHolder.getContext().getAuthentication().getName();
 *     }
 * }
 * }</pre>
 *
 * No tool code needs to change.
 */
public interface CurrentUserProvider {

    String currentUserId();
}
