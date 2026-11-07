package com.islamshariful.spring_mcp.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Temporary user provider used until authentication is added.
 * Every request acts as the user set in {@code app.demo-user} (default: "demo").
 */
@Component
public class DemoUserProvider implements CurrentUserProvider {

    private final String demoUser;

    public DemoUserProvider(@Value("${app.demo-user:demo}") String demoUser) {
        this.demoUser = demoUser;
    }

    @Override
    public String currentUserId() {
        return demoUser;
    }
}
