package com.example.ams.api.dto;

import java.util.List;

public record ParentChildResponse(long studentId, String studentName, List<ClassResponse> classes) {
}
