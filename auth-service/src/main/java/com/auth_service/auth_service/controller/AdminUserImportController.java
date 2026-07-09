package com.auth_service.auth_service.controller;

import com.auth_service.auth_service.model.dto.imports.ImportUserResultDTO;
import com.auth_service.auth_service.service.imports.UserImportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/api/admin/auth-service/users")
/**
 * Cung cap API de quan tri vien nhap nguoi dung tu tep Excel.
 */
public class AdminUserImportController {

    private final UserImportService userImportService;

    public AdminUserImportController(UserImportService userImportService) {
        this.userImportService = userImportService;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    /**
     * Xu ly tep import va tra ve thong ke thanh cong cung loi theo tung dong.
     */
    public ImportUserResultDTO importUsers(
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        return userImportService.importUsers(file);
    }
}
