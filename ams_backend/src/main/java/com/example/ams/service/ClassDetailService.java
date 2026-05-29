package com.example.ams.service;

import org.springframework.stereotype.Service;

import com.example.ams.domain.clazz.Clazz;

@Service
public class ClassDetailService {

	private final ClassAccessService classAccessService;

	public ClassDetailService(ClassAccessService classAccessService) {
		this.classAccessService = classAccessService;
	}

	public Clazz getDetail(long classId) {
		return classAccessService.requireReadableClass(classId);
	}

	public boolean canManageContent(Clazz clazz) {
		return classAccessService.canManageClassContent(clazz);
	}
}
