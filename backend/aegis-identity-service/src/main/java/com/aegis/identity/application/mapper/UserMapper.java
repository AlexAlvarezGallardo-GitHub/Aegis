package com.aegis.identity.application.mapper;

import com.aegis.identity.application.dto.UserRegistrationResponse;
import com.aegis.identity.domain.model.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserRegistrationResponse toResponse(User user) {
        return new UserRegistrationResponse(
                user.getUserId().value(),
                user.getEmail().value(),
                user.getStatus().name(),
                user.getRegisteredAt()
        );
    }
}
