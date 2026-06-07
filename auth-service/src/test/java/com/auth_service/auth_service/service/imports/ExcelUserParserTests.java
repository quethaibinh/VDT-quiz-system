package com.auth_service.auth_service.service.imports;

import com.auth_service.auth_service.model.dto.imports.ImportUserRowDTO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelUserParserTests {

    private final ExcelUserParser parser = new ExcelUserParser();

    @Test
    void parsesUserRowAndNormalizesExcelDate() throws Exception {
        byte[] content;

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("users");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("fullName");
            header.createCell(1).setCellValue("email");
            header.createCell(2).setCellValue("studentCode");
            header.createCell(3).setCellValue("birthDate");
            header.createCell(4).setCellValue("gender");
            header.createCell(5).setCellValue("unknownColumn");

            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("Nguyen Van A");
            row.createCell(1).setCellValue("a@gmail.com");
            row.createCell(2).setCellValue("SV001");
            var dateCell = row.createCell(3);
            dateCell.setCellValue(Date.from(
                    LocalDate.of(2004, 1, 2)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
            ));
            var dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-MM-dd"));
            dateCell.setCellStyle(dateStyle);
            row.createCell(4).setCellValue("MALE");

            workbook.write(outputStream);
            content = outputStream.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                content
        );

        List<ImportUserRowDTO> rows = parser.parse(file);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getRowNumber()).isEqualTo(2);
        assertThat(rows.getFirst().getFullName()).isEqualTo("Nguyen Van A");
        assertThat(rows.getFirst().getStudentCode()).isEqualTo("SV001");
        assertThat(rows.getFirst().getBirthDate()).isEqualTo("2004-01-02");
        assertThat(rows.getFirst().getGender()).isEqualTo("MALE");
    }

    @Test
    void parsesBirthDateStoredAsText() throws Exception {
        byte[] content;

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("users");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("studentCode");
            header.createCell(1).setCellValue("birthDate");

            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("SV001");
            row.createCell(1).setCellValue("2004-01-02");

            workbook.write(outputStream);
            content = outputStream.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                content
        );

        List<ImportUserRowDTO> rows = parser.parse(file);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getBirthDate()).isEqualTo("2004-01-02");
    }
}
