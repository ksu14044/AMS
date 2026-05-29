package com.example.ams.api.clazz;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ams.api.dto.ClassDetailResponse;
import com.example.ams.api.dto.ClassResponse;
import com.example.ams.common.ApiResponse;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.service.ClassDetailService;
import com.example.ams.service.ClassQueryService;

@RestController
@RequestMapping("/api/v1/classes")
public class ClassController {

	private final ClassQueryService classQueryService;
	private final ClassDetailService classDetailService;

	public ClassController(ClassQueryService classQueryService, ClassDetailService classDetailService) {
		this.classQueryService = classQueryService;
		this.classDetailService = classDetailService;
	}

	@GetMapping
	public ApiResponse<List<ClassResponse>> list() {
		List<ClassResponse> classes = classQueryService.listForCurrentUser().stream()
				.map(ClassResponse::from)
				.toList();
		return ApiResponse.ok(classes);
	}

	@GetMapping("/{classId}")
	public ApiResponse<ClassDetailResponse> getDetail(@PathVariable long classId) {
		Clazz clazz = classDetailService.getDetail(classId);
		boolean canManage = classDetailService.canManageContent(clazz);
		return ApiResponse.ok(ClassDetailResponse.from(clazz, canManage));
	}
}
