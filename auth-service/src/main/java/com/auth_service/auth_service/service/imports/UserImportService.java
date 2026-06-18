package com.auth_service.auth_service.service.imports;

import com.auth_service.auth_service.model.dto.imports.ImportUserErrorDTO;
import com.auth_service.auth_service.model.dto.imports.ImportUserResultDTO;
import com.auth_service.auth_service.model.dto.imports.ImportUserRowDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
/**
 * Dieu phoi qua trinh doc, kiem tra va tao nguoi dung tu Excel.
 */
public class UserImportService {

    private final ExcelUserParser excelUserParser;
    private final ImportUserValidator importUserValidator;
    private final ImportedUserCreationService importedUserCreationService;

    public UserImportService(
            ExcelUserParser excelUserParser,
            ImportUserValidator importUserValidator,
            ImportedUserCreationService importedUserCreationService
    ) {
        this.excelUserParser = excelUserParser;
        this.importUserValidator = importUserValidator;
        this.importedUserCreationService = importedUserCreationService;
    }

    /**
     * Xu ly tung dong doc lap de dong loi khong huy cac dong hop le.
     */
    public ImportUserResultDTO importUsers(MultipartFile file) throws Exception {
        List<ImportUserRowDTO> rows = excelUserParser.parse(file);
        ImportUserResultDTO result = new ImportUserResultDTO();
        result.setTotalRows(rows.size());

        Set<String> seenUsernames = new HashSet<>();
        Set<String> seenStudentCodes = new HashSet<>();
        Set<String> seenTeacherCodes = new HashSet<>();

        for (ImportUserRowDTO row : rows) {
            Optional<ImportUserErrorDTO> validationError = importUserValidator.validate(
                    row,
                    seenUsernames,
                    seenStudentCodes,
                    seenTeacherCodes
            );

            if (validationError.isPresent()) {
                result.addError(validationError.get());
                continue;
            }

            try {
                importedUserCreationService.createUser(row);
                result.increaseSuccessCount();
            } catch (Exception exception) {
                result.addError(new ImportUserErrorDTO(
                        row.getRowNumber(),
                        "row",
                        "CREATE_USER_FAILED",
                        exception.getMessage()
                ));
            }
        }

        return result;
    }
}
