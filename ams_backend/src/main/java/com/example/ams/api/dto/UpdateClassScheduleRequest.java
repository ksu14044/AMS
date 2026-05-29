package com.example.ams.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpdateClassScheduleRequest(@NotNull @Valid List<ScheduleSlotRequest> slots) {
}
