package com.exam_service.exam_service.model.dto.exams;

/**
 * Trang thai kha dung cua ca thi doi voi hoc sinh.
 */
public enum StudentExamAvailability {
    UPCOMING, // Ca thi chua bat dau
    OPEN,     // Ca thi dang mo lam bai
    ENDED     // Ca thi da ket thuc
}
