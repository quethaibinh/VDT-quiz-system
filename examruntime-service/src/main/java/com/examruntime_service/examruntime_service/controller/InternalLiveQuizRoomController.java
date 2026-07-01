package com.examruntime_service.examruntime_service.controller;

import com.examruntime_service.examruntime_service.model.dto.livequiz.CreateLiveQuizRoomRequestDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizRoomDTO;
import com.examruntime_service.examruntime_service.service.livequiz.LiveQuizRoomService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/internal/examruntime-service/live-quizzes")
/**
 * Internal API de Exam Service tao room sau khi prepare snapshot live quiz.
 *
 * Endpoint nay khong danh cho client truc tiep. Security filter dung X-Internal-Api-Key
 * de chi Exam Service duoc phep tao room tu snapshot da commit.
 */
public class InternalLiveQuizRoomController {

    private final LiveQuizRoomService roomService;

    public InternalLiveQuizRoomController(LiveQuizRoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/rooms")
    public LiveQuizRoomDTO createRoom(@Valid @RequestBody CreateLiveQuizRoomRequestDTO request) {
        return roomService.createRoom(request);
    }
}
