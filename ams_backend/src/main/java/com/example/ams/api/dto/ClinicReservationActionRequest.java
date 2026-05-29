package com.example.ams.api.dto;

import jakarta.validation.constraints.NotNull;

public record ClinicReservationActionRequest(@NotNull Long slotId) {
}
