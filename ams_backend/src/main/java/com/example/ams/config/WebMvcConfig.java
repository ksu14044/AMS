package com.example.ams.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.ams.config.AmsUploadProperties;
import com.example.ams.security.AcademyAdminInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	private final AcademyAdminInterceptor academyAdminInterceptor;
	private final AmsUploadProperties uploadProperties;

	public WebMvcConfig(AcademyAdminInterceptor academyAdminInterceptor, AmsUploadProperties uploadProperties) {
		this.academyAdminInterceptor = academyAdminInterceptor;
		this.uploadProperties = uploadProperties;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String location = "file:" + uploadProperties.dir() + "/";
		registry.addResourceHandler("/api/v1/media/**").addResourceLocations(location);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(academyAdminInterceptor).addPathPatterns("/api/v1/admin/**");
	}
}
