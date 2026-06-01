package com.example.ams.api.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record UpdateClinicReservationResultRequest(@NotNull Map<String, Object> result) {
}
