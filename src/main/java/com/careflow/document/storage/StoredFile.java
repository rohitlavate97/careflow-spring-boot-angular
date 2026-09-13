package com.careflow.document.storage;

/**
 * Encapsulates metadata and cryptographic details of a physically stored file (§34).
 */
public record StoredFile(
        String originalFileName,
        String storedFileName,
        String storagePath,
        String mimeType,
        long fileSize,
        String checksumSha256
) {
}
