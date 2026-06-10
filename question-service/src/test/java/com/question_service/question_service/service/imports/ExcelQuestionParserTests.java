package com.question_service.question_service.service.imports;

import com.question_service.question_service.model.dto.imports.ImportQuestionRowDTO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelQuestionParserTests {

    private final ExcelQuestionParser parser = new ExcelQuestionParser();

    @Test
    void parsesQuestionRowsAndSkipsBlankRows() throws Exception {
        byte[] content;

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("questions");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("topicName");
            header.createCell(1).setCellValue("questionType");
            header.createCell(2).setCellValue("content");
            header.createCell(3).setCellValue("optionA");
            header.createCell(4).setCellValue("optionB");
            header.createCell(5).setCellValue("correctOptions");
            header.createCell(6).setCellValue("difficulty");
            header.createCell(7).setCellValue("status");

            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("Algebra");
            row.createCell(1).setCellValue("SINGLE_CHOICE");
            row.createCell(2).setCellValue("1 + 1 = ?");
            row.createCell(3).setCellValue("2");
            row.createCell(4).setCellValue("3");
            row.createCell(5).setCellValue("A");
            row.createCell(6).setCellValue("EASY");
            row.createCell(7).setCellValue("PUBLIC");

            sheet.createRow(2);

            workbook.write(outputStream);
            content = outputStream.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "questions.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                content
        );

        List<ImportQuestionRowDTO> rows = parser.parse(file);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getRowNumber()).isEqualTo(2);
        assertThat(rows.getFirst().getTopicName()).isEqualTo("Algebra");
        assertThat(rows.getFirst().getStatus()).isEqualTo("PUBLIC");
    }

}
