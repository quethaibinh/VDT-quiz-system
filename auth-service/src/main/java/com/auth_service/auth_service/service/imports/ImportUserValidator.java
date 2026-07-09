package com.auth_service.auth_service.service.imports;

import com.auth_service.auth_service.model.dto.imports.ImportUserErrorDTO;
import com.auth_service.auth_service.model.dto.imports.ImportUserRowDTO;
import com.auth_service.auth_service.model.entity.UserGender;
import com.auth_service.auth_service.repository.UserRepo;
import com.auth_service.auth_service.util.Checker;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Set;

@Component
/**
 * Kiem tra quy tac du lieu va trung lap truoc khi tao nguoi dung import.
 */
public class ImportUserValidator {

    private final UserRepo userRepo;
    private final Checker checker;

    public ImportUserValidator(UserRepo userRepo, Checker checker) {
        this.userRepo = userRepo;
        this.checker = checker;
    }

    /**
     * Tra ve loi dau tien cua mot dong, hoac rong khi dong hop le.
     */
    public Optional<ImportUserErrorDTO> validate(
            ImportUserRowDTO row,
            Set<String> seenUsernames,
            Set<String> seenStudentCodes,
            Set<String> seenTeacherCodes
    ) {
        if (isBlank(row.getFullName())) {
            return error(row, "fullName", "REQUIRED", "Full name is required");
        }

        boolean hasStudentCode = !isBlank(row.getStudentCode());
        boolean hasTeacherCode = !isBlank(row.getTeacherCode());

        if (!hasStudentCode && !hasTeacherCode) {
            return error(row, "studentCode", "MISSING_CODE", "Student code or teacher code is required");
        }

        if (hasStudentCode && hasTeacherCode) {
            return error(row, "teacherCode", "INVALID_CODE", "Only one of studentCode or teacherCode is allowed");
        }

        String generatedUsername = getGeneratedUsername(row);
        if (!seenUsernames.add(generatedUsername)) {
            return error(row, "username", "DUPLICATE_USERNAME_IN_FILE", "Username is duplicated in file");
        }

        if (hasStudentCode && !seenStudentCodes.add(row.getStudentCode())) {
            return error(row, "studentCode", "DUPLICATE_STUDENT_CODE_IN_FILE", "Student code is duplicated in file");
        }

        if (hasTeacherCode && !seenTeacherCodes.add(row.getTeacherCode())) {
            return error(row, "teacherCode", "DUPLICATE_TEACHER_CODE_IN_FILE", "Teacher code is duplicated in file");
        }

        if (userRepo.existsByUsername(generatedUsername)) {
            return error(row, "username", "DUPLICATE_USERNAME", "Username already exists");
        }

        if (hasStudentCode && userRepo.existsByStudentCode(row.getStudentCode())) {
            return error(row, "studentCode", "DUPLICATE_STUDENT_CODE", "Student code already exists");
        }

        if (hasTeacherCode && userRepo.existsByTeacherCode(row.getTeacherCode())) {
            return error(row, "teacherCode", "DUPLICATE_TEACHER_CODE", "Teacher code already exists");
        }

        if (!isBlank(row.getEmail()) && !checker.emailChecker(row.getEmail())) {
            return error(row, "email", "INVALID_EMAIL", "Email is invalid");
        }

        if (!isBlank(row.getBirthDate())) {
            try {
                LocalDate.parse(row.getBirthDate());
            } catch (DateTimeParseException exception) {
                return error(row, "birthDate", "INVALID_BIRTH_DATE", "Birth date must use yyyy-MM-dd format");
            }
        }

        if (!isBlank(row.getGender())) {
            try {
                UserGender.valueOf(row.getGender().toUpperCase());
            } catch (IllegalArgumentException exception) {
                return error(row, "gender", "INVALID_GENDER", "Gender must be MALE, FEMALE, or OTHER");
            }
        }

        return Optional.empty();
    }

    public String getGeneratedUsername(ImportUserRowDTO row) {
        return !isBlank(row.getStudentCode()) ? row.getStudentCode() : row.getTeacherCode();
    }

    public boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Optional<ImportUserErrorDTO> error(ImportUserRowDTO row, String field, String code, String message) {
        return Optional.of(new ImportUserErrorDTO(row.getRowNumber(), field, code, message));
    }
}
