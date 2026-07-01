package com.examruntime_service.examruntime_service.model.entity.enums;

public enum LiveQuizRoomStatus {
    // Dang hydrate snapshot, chua cho student join.
    PREPARING,
    // Phong cho: student duoc join lobby nhung chua duoc lam bai.
    OPEN,
    // Teacher da bam bat dau: khoa join moi, cho student da join lam bai.
    STARTED,
    // Ket thuc phong, khong cho join/play nua.
    CLOSED,
    ARCHIVED
}
