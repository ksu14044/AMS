package com.example.ams.security;

import com.example.ams.domain.user.UserRole;

public record JwtPrincipal(long userId, long academyId, UserRole role, String status) {
}
