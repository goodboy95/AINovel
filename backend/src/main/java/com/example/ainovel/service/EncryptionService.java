package com.example.ainovel.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
public class EncryptionService {

    private final SecretKeySpec secretKey;

    public EncryptionService(@Value("${app.encryption.key}") String secretKeyString) {
        // Ensure the key is 16, 24, or 32 bytes for AES
        byte[] keyBytes = new byte[16];
        byte[] providedKeyBytes = secretKeyString.getBytes();
        System.arraycopy(providedKeyBytes, 0, keyBytes, 0, Math.min(providedKeyBytes.length, keyBytes.length));
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String strToEncrypt) {
        if (strToEncrypt == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Error while encrypting: " + e.toString());
        }
    }

    public String decrypt(String strToDecrypt) {
        if (strToDecrypt == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception e) {
            throw new RuntimeException("Error while decrypting: " + e.toString());
        }
    }
}
