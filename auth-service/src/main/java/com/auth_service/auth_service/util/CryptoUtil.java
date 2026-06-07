package com.auth_service.auth_service.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CryptoUtil {

    // AES-GCM-NoPadding là tiêu chuẩn an toàn cao nhất cho AES
    private final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";

    // Kích thước IV cho GCM tiêu chuẩn là 12 bytes (96 bits)
    private final int IV_LENGTH_BYTE = 12;

    // Kích thước Auth Tag cho GCM tiêu chuẩn là 16 bytes (128 bits)
    private final int TAG_LENGTH_BIT = 128;

    @Value("${app.security.aes.key}")
    private String secretKeyStr;

    @Value("${app.security.hmac.key}")
    private String hmacKeyStr;

    /**
     * Hàm mã hóa dữ liệu (CCCD/SĐT)
     * @param plainText Dữ liệu gốc cần mã hóa
     * @return Chuỗi mã hóa dạng Base64 (đã gộp cả IV bên trong)
     */
    public String encrypt(String plainText) throws Exception {
        // 1. Chuyển đổi Secret Key từ Base64 về dạng byte[]
        byte[] keyBytes = Base64.getDecoder().decode(this.secretKeyStr);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

        // 2. Tạo IV ngẫu nhiên (Bắt buộc phải ngẫu nhiên cho mỗi lần mã hóa)
        byte[] iv = new byte[IV_LENGTH_BYTE];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // 3. Khởi tạo Cipher cấu hình AES/GCM
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        // 4. Tiến hành mã hóa dữ liệu
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

        // 5. Gộp [IV] và [Dữ liệu đã mã hóa + Auth Tag] vào làm 1 gói duy nhất để dễ lưu trữ
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
        byteBuffer.put(iv);
        byteBuffer.put(encryptedBytes);
        byte[] cipherTextWithIv = byteBuffer.array();

        // 6. Trả về chuỗi Base64 để lưu vào Database
        return Base64.getEncoder().encodeToString(cipherTextWithIv);
    }

    /**
     * Hàm giải mã dữ liệu
     * @param cipherTextWithIvStr Chuỗi mã hóa dạng Base64 (đã chứa IV)
     * @return Dữ liệu gốc ban đầu
     */
    public String decrypt(String cipherTextWithIvStr) throws Exception {
        // 1. Giải mã chuỗi Base64 đầu vào
        byte[] cipherTextWithIv = Base64.getDecoder().decode(cipherTextWithIvStr);
        byte[] keyBytes = Base64.getDecoder().decode(this.secretKeyStr);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

        // 2. Tách IV ra khỏi gói tin (12 byte đầu tiên)
        ByteBuffer byteBuffer = ByteBuffer.wrap(cipherTextWithIv);
        byte[] iv = new byte[IV_LENGTH_BYTE];
        byteBuffer.get(iv);

        // 3. Tách phần dữ liệu đã mã hóa còn lại
        byte[] encryptedBytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(encryptedBytes);

        // 4. Khởi tạo Cipher cấu hình AES/GCM giải mã
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        // 5. Giải mã và trả về text gốc
        byte[] plainTextBytes = cipher.doFinal(encryptedBytes);
        return new String(plainTextBytes, "UTF-8");
    }

    /**
     * Hàm tạo Blind Index bằng HMAC-SHA256 (Chuỗi trả ra luôn cố định với cùng đầu vào)
     */
    public String generateBlindIndex(String plainText) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(hmacKeyStr.getBytes("UTF-8"), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKey);

        byte[] hmacBytes = mac.doFinal(plainText.getBytes("UTF-8"));

        // Trả về chuỗi dạng Hex (hoặc Base64) để lưu vào cột index trong DB
        StringBuilder hexString = new StringBuilder();
        for (byte b : hmacBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Hàm phụ trợ: Sinh ngẫu nhiên một Secret Key 256-bit chuẩn AES
     */
    public static String generateAES256Key() throws Exception {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[32]; // 32 bytes = 256 bits
        secureRandom.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
