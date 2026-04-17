package com.estorilflow.dto;

import com.estorilflow.entity.Role;

public record AuthenticatedUserResponse(
        Long id,
        String name,
        String username,
        String email,
        Role role
) {
}
