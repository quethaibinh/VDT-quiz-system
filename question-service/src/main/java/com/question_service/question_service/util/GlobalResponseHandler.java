package com.question_service.question_service.util;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {


    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Áp dụng cho tất cả các API trả về dữ liệu
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // Nếu Controller đã trả về sẵn cấu trúc ApiResponse hoặc ErrorResponse thì bỏ qua không bọc nữa
        if (body instanceof ApiResponse || body instanceof ApiErrorResponse) {
            return body;
        }

        //  Xử lý riêng cho kiểu String (vì StringHttpMessageConverter xử lý khác các Object thông thường)
        if (body instanceof String) {
            return body;
        }

        // Tự động bọc mọi dữ liệu thành công khác vào ApiResponse cấu trúc chuẩn
        return ApiResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .message("Success")
                .data(body)
                .build();
    }
}
