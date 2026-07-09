package com.examruntime_service.examruntime_service.service.livequiz;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Sinh ma phong ngan cho live quiz. Ma nay la secret truy cap nen dung SecureRandom.
 *
 * Alphabet bo cac ky tu de nham nhu I, O, 0, 1 de giao vien doc ma phong tren lop
 * it bi sai hon.
 */
@Component
public class LiveQuizRoomCodeGenerator {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    /**
     * Tao ma phong 6 ky tu de student nhap khi join live quiz.
     */
    public String generate() {
        char[] code = new char[CODE_LENGTH];
        for (int index = 0; index < CODE_LENGTH; index++) {
            // SecureRandom giup ma phong kho doan hon UUID cat ngan hay Random thuong.
            code[index] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(code);
    }
}
