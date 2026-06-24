package com.examruntime_service.examruntime_service.util;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
/**
 * Chuan hoa du lieu thanh cong ve cau truc ApiResponse dung chung.
 */
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {


    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Ap dung cho moi du lieu tra ve tu controller.
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // Khong boc lai du lieu da dung cau truc chuan.
        if (body instanceof ApiResponse || body instanceof ApiErrorResponse) {
            return body;
        }

        // Chuoi dung bo chuyen doi rieng nen giu nguyen de tranh loi ep kieu.
        if (body instanceof String) {
            return body;
        }

        // Boc cac du lieu thanh cong con lai vao cau truc chung.
        return ApiResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .message("Success")
                .data(body)
                .build();
    }
}
