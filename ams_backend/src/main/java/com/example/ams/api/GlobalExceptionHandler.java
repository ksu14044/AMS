package com.example.ams.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.ams.common.ApiResponse;
import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
		ErrorCode code = ex.errorCode();
		return ResponseEntity.status(code.httpStatus())
				.body(ApiResponse.fail(code.name(), ex.getMessage()));
	}

	@ExceptionHandler({ AccessDeniedException.class, AuthorizationDeniedException.class })
	public ResponseEntity<ApiResponse<Void>> handleAccessDenied(Exception ex) {
		return ResponseEntity.status(ErrorCode.FORBIDDEN.httpStatus())
				.body(ApiResponse.fail(ErrorCode.FORBIDDEN.name(), ErrorCode.FORBIDDEN.message()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(FieldError::getDefaultMessage)
				.orElse(ErrorCode.INVALID_REQUEST.message());
		return ResponseEntity.badRequest()
				.body(ApiResponse.fail(ErrorCode.INVALID_REQUEST.name(), message));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
		Throwable root = ex;
		while (root.getCause() != null) {
			root = root.getCause();
		}
		if (root instanceof BusinessException business) {
			return handleBusiness(business);
		}
		log.error("Unhandled exception", ex);
		return ResponseEntity.internalServerError()
				.body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.name(), ErrorCode.INTERNAL_ERROR.message()));
	}
}
