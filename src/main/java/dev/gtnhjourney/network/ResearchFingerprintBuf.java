package dev.gtnhjourney.network;

import dev.gtnhjourney.research.ResearchFingerprint;
import io.netty.buffer.ByteBuf;

final class ResearchFingerprintBuf {

    private ResearchFingerprintBuf() {}

    static void write(ByteBuf buf, ResearchFingerprint fingerprint) {
        if (fingerprint == null) throw new IllegalArgumentException("fingerprint must not be null");
        buf.writeBytes(fingerprint.toBytes());
    }

    static ResearchFingerprint read(ByteBuf buf) {
        byte[] bytes = new byte[ResearchFingerprint.BYTE_LENGTH];
        buf.readBytes(bytes);
        return ResearchFingerprint.fromBytes(bytes);
    }
}
