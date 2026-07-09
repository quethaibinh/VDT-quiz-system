package com.auth_service.auth_service.service.imports;

import com.auth_service.auth_service.model.dto.imports.ImportUserRowDTO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
/**
 * Doc tep XLSX va chuyen cac dong co du lieu thanh DTO import nguoi dung.
 */
public class ExcelUserParser {

    private final DataFormatter formatter = new DataFormatter();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * Doc sheet dau tien va giu so dong Excel de tra loi chinh xac.
     */
    public List<ImportUserRowDTO> parse(MultipartFile file) throws Exception {
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
            List<ImportUserRowDTO> rows = new ArrayList<>();

            // Doc tung dong va giu lai so dong that trong Excel de bao loi.
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                rows.add(new ImportUserRowDTO(
                        i + 1,
                        getValue(row, headers, "username"),
                        getValue(row, headers, "password"),
                        getValue(row, headers, "fullName"),
                        getValue(row, headers, "email"),
                        getValue(row, headers, "studentCode"),
                        getValue(row, headers, "teacherCode"),
                        getValue(row, headers, "phone"),
                        getValue(row, headers, "nationalId"),
                        getValue(row, headers, "birthDate"),
                        getValue(row, headers, "gender")
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

        // Chi chuyen doi khi Excel luu ngay sinh duoi dang so/ngay.
        if ("birthDate".equals(headerName)
                && cell.getCellType() == CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(dateFormatter);
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
