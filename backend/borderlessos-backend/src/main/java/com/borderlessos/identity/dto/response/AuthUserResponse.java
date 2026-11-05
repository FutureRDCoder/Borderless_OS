package com.borderlessos.identity.dto.response;

import java.util.Set;

public record AuthUserResponse(
        String email,
        Set<String> roles
) {}