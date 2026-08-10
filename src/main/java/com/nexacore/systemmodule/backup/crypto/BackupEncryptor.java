package com.nexacore.systemmodule.backup.crypto;

import com.nexacore.systemmodule.backup.config.BackupProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class BackupEncryptor {
    private static final byte[] MAGIC = "NCBK1".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private final BackupProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public OutputStream encryptedStream(Path target) throws Exception {
        byte[] key = key();
        byte[] nonce = new byte[12];
        secureRandom.nextBytes(nonce);
        Cipher cipher = cipher(key, nonce);
        OutputStream raw = Files.newOutputStream(target);
        raw.write(MAGIC);
        raw.write(nonce);
        return new CipherOutputStream(raw, cipher);
    }

    /** Decrypts an artifact into a caller-owned private restore-verification workspace. */
    public void decrypt(Path source, Path target) throws Exception {
        try (InputStream raw = Files.newInputStream(source)) {
            byte[] magic = raw.readNBytes(MAGIC.length);
            if (!java.util.Arrays.equals(magic, MAGIC)) throw new IllegalArgumentException("Unsupported backup artifact format");
            byte[] nonce = raw.readNBytes(12);
            if (nonce.length != 12) throw new IllegalArgumentException("Backup artifact header is incomplete");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(128, nonce));
            try (CipherInputStream decrypted = new CipherInputStream(raw, cipher);
                 OutputStream output = Files.newOutputStream(target)) {
                decrypted.transferTo(output);
            }
        }
    }

    private byte[] key() {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(properties.getEncryptionKeyBase64());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("BACKUP_ENCRYPTION_KEY_BASE64 must be valid Base64", exception);
        }
        if (key.length != 32) throw new IllegalStateException("BACKUP_ENCRYPTION_KEY_BASE64 must decode to 32 bytes");
        return key;
    }

    private Cipher cipher(byte[] key, byte[] nonce) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        return cipher;
    }
}
