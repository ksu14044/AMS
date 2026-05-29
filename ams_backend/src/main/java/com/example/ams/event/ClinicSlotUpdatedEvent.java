package com.example.ams.event;

import java.util.List;

public record ClinicSlotUpdatedEvent(long classId, long slotId, String slotLabel, List<Long> studentIds) {
}
