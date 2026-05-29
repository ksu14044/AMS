package com.example.ams.domain.academy;

import java.time.Instant;

public record Academy(long academyId, String name, String code, Instant createdAt) {
}
