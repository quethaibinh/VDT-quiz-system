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
/**
 * Ma hoa du lieu nhay cam va tao blind index de ho tro tra cuu.
 */
public class CryptoUtil {

    // AES-GCM cung cap ca ma hoa va kiem tra tinh toan ven du lieu.
    private final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";

    // GCM thuong dung IV 12 byte.
    private final int IV_LENGTH_BYTE = 12;

    // The xac thuc dai 128 bit.
    private final int TAG_LENGTH_BIT = 128;

    @Value("${app.security.aes.key}")
    private String secretKeyStr;

    @Value("${app.security.hmac.key}")
    private String hmacKeyStr;

    /**
     * Ma hoa du lieu va ghep IV vao ket qua Base64 de co the giai ma sau nay.
     */
    public String encrypt(String plainText) throws Exception {
        // Khoa AES duoc cau hinh o dang Base64.
        byte[] keyBytes = Base64.getDecoder().decode(this.secretKeyStr);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

        // Moi lan ma hoa dung IV ngau nhien rieng.
        byte[] iv = new byte[IV_LENGTH_BYTE];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

        // Luu IV o dau goi tin de luc giai ma co the tach ra ma khong can cot rieng.
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
        byteBuffer.put(iv);
        byteBuffer.put(encryptedBytes);
        byte[] cipherTextWithIv = byteBuffer.array();

        return Base64.getEncoder().encodeToString(cipherTextWithIv);
    }

    /**
     * Tach IV khoi goi Base64 va giai ma ve du lieu ban dau.
     */
    public String decrypt(String cipherTextWithIvStr) throws Exception {
        byte[] cipherTextWithIv = Base64.getDecoder().decode(cipherTextWithIvStr);
        byte[] keyBytes = Base64.getDecoder().decode(this.secretKeyStr);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

        // IV nam trong 12 byte dau cua goi tin.
        ByteBuffer byteBuffer = ByteBuffer.wrap(cipherTextWithIv);
        byte[] iv = new byte[IV_LENGTH_BYTE];
        byteBuffer.get(iv);

        byte[] encryptedBytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(encryptedBytes);

        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        byte[] plainTextBytes = cipher.doFinal(encryptedBytes);
        return new String(plainTextBytes, "UTF-8");
    }

    /**
     * Tao blind index HMAC on dinh de tim kiem ma khong luu du lieu goc.
     */
    public String generateBlindIndex(String plainText) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(hmacKeyStr.getBytes("UTF-8"), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKey);

        byte[] hmacBytes = mac.doFinal(plainText.getBytes("UTF-8"));

        // Doi HMAC sang hex de luu vao cot index.
        StringBuilder hexString = new StringBuilder();
        for (byte b : hmacBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Sinh khoa AES 256 bit o dang Base64.
     */
    public static String generateAES256Key() throws Exception {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[32]; // 32 byte tuong duong 256 bit.
        secureRandom.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
