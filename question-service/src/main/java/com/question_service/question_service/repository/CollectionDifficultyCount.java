package com.question_service.question_service.repository;

import com.question_service.question_service.model.entity.Difficulty;

public interface CollectionDifficultyCount {
    Difficulty getDifficulty();
    long getCount();
}
