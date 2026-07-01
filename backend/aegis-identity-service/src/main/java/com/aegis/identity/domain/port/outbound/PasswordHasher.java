package com.aegis.identity.domain.port.outbound;

import com.aegis.identity.domain.model.PasswordHash;

public interface PasswordHasher {

    PasswordHash hash(String rawPassword);

    boolean matches(String rawPassword, PasswordHash hash);
}
