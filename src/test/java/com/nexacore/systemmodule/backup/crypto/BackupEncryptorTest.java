package com.nexacore.systemmodule.backup.crypto;

import com.nexacore.systemmodule.backup.config.BackupProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class BackupEncryptorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void writesVersionedAuthenticatedCiphertextThatCanBeDecrypted() throws Exception {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 7);
        BackupProperties properties = new BackupProperties();
        properties.setEncryptionKeyBase64(Base64.getEncoder().encodeToString(key));
        BackupEncryptor encryptor = new BackupEncryptor(properties);
        Path target = temporaryDirectory.resolve("backup.aesgcm");

        try (OutputStream output = encryptor.encryptedStream(target)) {
            output.write("sensitive backup".getBytes(StandardCharsets.UTF_8));
        }

        byte[] encrypted = Files.readAllBytes(target);
        assertThat(new String(encrypted, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("NCBK1");
        assertThat(new String(encrypted, StandardCharsets.UTF_8)).doesNotContain("sensitive backup");

        byte[] nonce = Arrays.copyOfRange(encrypted, 5, 17);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        byte[] plaintext = cipher.doFinal(Arrays.copyOfRange(encrypted, 17, encrypted.length));
        assertThat(new String(plaintext, StandardCharsets.UTF_8)).isEqualTo("sensitive backup");

        Path restored = temporaryDirectory.resolve("restored.zip");
        encryptor.decrypt(target, restored);
        assertThat(Files.readString(restored)).isEqualTo("sensitive backup");
    }

    @Test
    void rejectsKeysThatAreNotAes256() {
        BackupProperties properties = new BackupProperties();
        properties.setEncryptionKeyBase64(Base64.getEncoder().encodeToString(new byte[16]));
        BackupEncryptor encryptor = new BackupEncryptor(properties);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> encryptor.encryptedStream(temporaryDirectory.resolve("invalid")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
