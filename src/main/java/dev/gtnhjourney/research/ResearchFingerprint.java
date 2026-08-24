package dev.gtnhjourney.research;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/** Fixed-size SHA-256 token for network lookup of a full ResearchKey. */
public final class ResearchFingerprint implements Comparable<ResearchFingerprint> {

    public static final int BYTE_LENGTH = 32;
    private final byte[] bytes;

    private ResearchFingerprint(byte[] bytes) {
        if (bytes == null || bytes.length != BYTE_LENGTH)
            throw new IllegalArgumentException("fingerprint must be 32 bytes");
        this.bytes = bytes.clone();
    }

    public static ResearchFingerprint of(ResearchKey key) {
        if (key == null) throw new IllegalArgumentException("key must not be null");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateUtf8(digest, key.getItemId());
            updateInt(digest, key.getMeta());
            updateUtf8(digest, key.getCanonicalNbt());
            return new ResearchFingerprint(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public static ResearchFingerprint fromBytes(byte[] bytes) {
        return new ResearchFingerprint(bytes);
    }

    public byte[] toBytes() {
        return bytes.clone();
    }

    public String toHex() {
        StringBuilder out = new StringBuilder(BYTE_LENGTH * 2);
        for (byte value : bytes) out.append(String.format("%02x", value & 0xff));
        return out.toString();
    }

    @Override
    public int compareTo(ResearchFingerprint other) {
        for (int i = 0; i < BYTE_LENGTH; i++) {
            int a = bytes[i] & 0xff;
            int b = other.bytes[i] & 0xff;
            if (a != b) return a < b ? -1 : 1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
            || obj instanceof ResearchFingerprint && Arrays.equals(bytes, ((ResearchFingerprint) obj).bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return toHex();
    }

    private static void updateUtf8(MessageDigest digest, String value) {
        byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
        updateInt(digest, utf8.length);
        digest.update(utf8);
    }

    private static void updateInt(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }
}
