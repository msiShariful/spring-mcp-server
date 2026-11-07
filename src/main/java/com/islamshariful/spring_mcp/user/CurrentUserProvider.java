package com.islamshariful.spring_mcp.user;

/**
 * Tells the tools WHO is making the request, so each user only sees their own data.
 *
 * <p>{@link SecurityUserProvider} returns the user logged in with an API key.
 * If you switch to another login method (e.g. OAuth2/JWT), this interface and the
 * tools stay the same.
 */
public interface CurrentUserProvider {

    String currentUserId();
}
