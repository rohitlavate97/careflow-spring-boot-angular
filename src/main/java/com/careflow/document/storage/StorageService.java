package com.careflow.document.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Storage service abstraction decoupling document metadata from underlying binary providers (§34).
 */
public interface StorageService {

    /**
     * Stores a multipart file into the storage provider.
     *
     * @param file the incoming multipart payload
     * @param subDirectory optional logical directory partition (e.g. "patient-123/2026/09")
     * @return StoredFile details including checksum and storage path
     */
    StoredFile store(MultipartFile file, String subDirectory);

    /**
     * Loads a file as a Spring Resource for streaming or downloading.
     *
     * @param storagePath the stored file reference
     * @return Resource representing the file stream
     */
    Resource loadAsResource(String storagePath);

    /**
     * Deletes a stored file.
     *
     * @param storagePath the stored file reference
     */
    void delete(String storagePath);

    /**
     * Checks if a stored file physically exists.
     *
     * @param storagePath the stored file reference
     * @return true if file exists, false otherwise
     */
    boolean exists(String storagePath);
}
