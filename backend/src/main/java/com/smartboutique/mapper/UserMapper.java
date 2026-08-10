package com.smartboutique.mapper;

import com.smartboutique.dto.UserResponse;
import com.smartboutique.entity.User;
import org.springframework.stereotype.Component;

/** Conversion entre l'entite User et ses DTO de sortie. */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.isPlatformAdmin(),
                user.isActive(),
                user.getPhone(),
                user.getAddress(),
                user.getGovernorat(),
                user.getCreatedAt()
        );
    }
}
