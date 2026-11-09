package com.islamshariful.spring_mcp.security;

import java.util.HexFormat;

/** Lets tests outside this package hash API keys the same way the server does. */
public final class ApiKeyTestHashes {

    private ApiKeyTestHashes() {}

    public static String sha256Hex(String key) {
        return HexFormat.of().formatHex(ApiKeyAuthenticationFilter.sha256(key));
    }
}
