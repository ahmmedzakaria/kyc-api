package com.nexacore.commonmodule.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

public final class AssignmentVersion {
    private AssignmentVersion() {}

    public static String of(Collection<?> keys) {
        String canonical = keys.stream().map(String::valueOf).sorted().reduce((a, b) -> a + "\n" + b).orElse("");
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public static void requireCurrent(String supplied, Collection<?> keys) {
        if (supplied == null || supplied.isBlank() || !MessageDigest.isEqual(
                of(keys).getBytes(StandardCharsets.US_ASCII), supplied.getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalArgumentException("Assignment version is stale; reload before replacing assignments");
        }
    }
}
