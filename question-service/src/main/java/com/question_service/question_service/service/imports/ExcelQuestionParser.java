package com.question_service.question_service.service.imports;

import com.question_service.question_service.model.dto.imports.ImportQuestionRowDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
/**
 * Doc sheet dau tien cua tep XLSX thanh cac dong import cau hoi.
 */
public class ExcelQuestionParser {

    private final DataFormatter formatter = new DataFormatter();

    /**
     * Bo qua dong rong va giu so dong goc de bao loi chinh xac.
     */
    public List<ImportQuestionRowDTO> parse(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only .xlsx files are supported");
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("Excel file has no sheet");
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel file has no header row");
            }

            Map<String, Integer> headers = readHeaders(headerRow);
            List<ImportQuestionRowDTO> rows = new ArrayList<>();

            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                rows.add(new ImportQuestionRowDTO(
                        i + 1,
                        getValue(row, headers, "topicName"),
                        getValue(row, headers, "questionType"),
                        getValue(row, headers, "content"),
                        getValue(row, headers, "optionA"),
                        getValue(row, headers, "optionB"),
                        getValue(row, headers, "optionC"),
                        getValue(row, headers, "optionD"),
                        getValue(row, headers, "optionE"),
                        getValue(row, headers, "optionF"),
                        getValue(row, headers, "optionG"),
                        getValue(row, headers, "correctOptions"),
                        getValue(row, headers, "difficulty"),
                        getValue(row, headers, "defaultScore"),
                        getValue(row, headers, "estimatedSecond"),
                        getValue(row, headers, "explanation"),
                        getValue(row, headers, "visibility"),
                        getValue(row, headers, "contentFormat")
                ));
            }

            return rows;
        }
    }

    private Map<String, Integer> readHeaders(Row headerRow) {
        Map<String, Integer> headers = new HashMap<>();
        for (Cell cell : headerRow) {
            String name = formatter.formatCellValue(cell).trim();
            if (!name.isBlank()) {
                headers.put(name, cell.getColumnIndex());
            }
        }
        return headers;
    }

    private String getValue(Row row, Map<String, Integer> headers, String headerName) {
        Integer index = headers.get(headerName);
        if (index == null) {
            return null;
        }

        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }

        String value = formatter.formatCellValue(cell).trim();
        return value.isBlank() ? null : value;
    }

    private boolean isEmptyRow(Row row) {
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).trim().isBlank()) {
                return false;
            }
        }
        return true;
    }

}
