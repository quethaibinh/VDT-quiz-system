package com.result_service.result_service.service.livequiz;

import com.result_service.result_service.model.dto.livequiz.TeacherLiveQuizResultRowDTO;
import com.result_service.result_service.model.entity.ExamResult;
import com.result_service.result_service.util.exception.NotFoundException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class LiveQuizResultExportService {

    private final LiveQuizResultQueryService queryService;

    public LiveQuizResultExportService(LiveQuizResultQueryService queryService) {
        this.queryService = queryService;
    }

    @Transactional(readOnly = true)
    public byte[] exportRoom(UUID teacherId, UUID roomId) {
        // Goi query service truoc de tai su dung check quyen giao vien va sort theo finalRank.
        List<ExamResult> results = queryService.sortedRoomResults(roomId);
        if (results.isEmpty()) {
            throw new NotFoundException("LIVE_QUIZ_RESULTS_NOT_FOUND");
        }
        // Goi teacherResults de dung chung rule phan quyen voi API man hinh.
        // Export khong tu check owner rieng de tranh lech policy.
        queryService.teacherResults(teacherId, roomId, 0, Math.max(results.size(), 1));

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            // File export la snapshot sau close: rank/score se khong doi sau khi da release.
            var sheet = workbook.createSheet("live-quiz-results");
            String[] headers = {
                    "rank", "studentCode", "studentName", "score", "maxScore", "percentage",
                    "answered", "totalQuestions", "correct", "wrong", "timeout", "notReached",
                    "averageResponseMs", "finishedAt", "releasedAt"
            };
            Row header = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                header.createCell(index).setCellValue(headers[index]);
            }
            int rowIndex = 1;
            for (ExamResult result : results) {
                TeacherLiveQuizResultRowDTO rowDto = queryService.teacherRow(result);
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(rowDto.finalRank());
                row.createCell(1).setCellValue(text(rowDto.studentCode()));
                row.createCell(2).setCellValue(text(rowDto.studentName()));
                row.createCell(3).setCellValue(rowDto.score().doubleValue());
                row.createCell(4).setCellValue(rowDto.maxScore().doubleValue());
                row.createCell(5).setCellValue(rowDto.percentage().doubleValue());
                row.createCell(6).setCellValue(rowDto.answeredCount());
                row.createCell(7).setCellValue(rowDto.totalQuestions());
                row.createCell(8).setCellValue(rowDto.correctCount());
                row.createCell(9).setCellValue(rowDto.wrongCount());
                row.createCell(10).setCellValue(rowDto.timeoutCount());
                row.createCell(11).setCellValue(rowDto.notReachedCount());
                row.createCell(12).setCellValue(rowDto.averageResponseMs() != null ? rowDto.averageResponseMs() : 0);
                row.createCell(13).setCellValue(date(rowDto.finishedAt()));
                row.createCell(14).setCellValue(date(rowDto.releasedAt()));
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("LIVE_QUIZ_RESULT_EXPORT_FAILED", exception);
        }
    }

    private String text(String value) {
        return value != null ? value : "";
    }

    private String date(OffsetDateTime value) {
        return value != null ? value.toString() : "";
    }
}
