package com.careflow.document.storage;

import com.careflow.document.exception.DocumentStorageException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Local filesystem implementation of the StorageService abstraction (§34).
 * Enforces file sanitization, directory isolation, streaming SHA-256 computation,
 * and path-traversal safeguards.
 */
@Service
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path rootLocation;

    public LocalStorageService(@Value("${careflow.storage.documents-dir:./data/documents}") String storageLocation) {
        this.rootLocation = Paths.get(storageLocation).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            log.info("Medical document storage initialized at: {}", rootLocation);
        } catch (IOException e) {
            throw new DocumentStorageException("Failed to create document storage root directory: " + rootLocation, e);
        }
    }

    @Override
    public StoredFile store(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new DocumentStorageException("Cannot store an empty or null file.", HttpStatus.BAD_REQUEST);
        }

        String rawFilename = file.getOriginalFilename();
        if (rawFilename == null || rawFilename.isBlank()) {
            rawFilename = "unnamed_document";
        }

        String cleanedFilename = StringUtils.cleanPath(rawFilename);
        if (cleanedFilename.contains("..")) {
            throw new DocumentStorageException("Filename contains invalid relative path sequence: " + cleanedFilename, HttpStatus.BAD_REQUEST);
        }

        Path targetDir = rootLocation;
        if (subDirectory != null && !subDirectory.isBlank()) {
            String cleanedSubdir = StringUtils.cleanPath(subDirectory);
            if (cleanedSubdir.contains("..")) {
                throw new DocumentStorageException("Directory path contains invalid sequence: " + cleanedSubdir, HttpStatus.BAD_REQUEST);
            }
            targetDir = rootLocation.resolve(cleanedSubdir).normalize();
            if (!targetDir.startsWith(rootLocation)) {
                throw new DocumentStorageException("Target directory escapes root storage path.", HttpStatus.BAD_REQUEST);
            }
        }

        try {
            Files.createDirectories(targetDir);

            String fileExtension = "";
            int dotIndex = cleanedFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                fileExtension = cleanedFilename.substring(dotIndex);
            }

            String storedFileName = UUID.randomUUID() + fileExtension;
            Path destinationPath = targetDir.resolve(storedFileName).normalize();

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream is = file.getInputStream();
                 DigestInputStream dis = new DigestInputStream(is, md)) {
                Files.copy(dis, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String checksumSha256 = HexFormat.of().formatHex(md.digest());
            String relativeStoragePath = rootLocation.relativize(destinationPath).toString().replace('\\', '/');

            String mimeType = file.getContentType();
            if (mimeType == null || mimeType.isBlank()) {
                mimeType = "application/octet-stream";
            }

            log.debug("Successfully stored file [{}] at [{}] with SHA-256 [{}]",
                    cleanedFilename, relativeStoragePath, checksumSha256);

            return new StoredFile(
                    cleanedFilename,
                    storedFileName,
                    relativeStoragePath,
                    mimeType,
                    file.getSize(),
                    checksumSha256
            );
        } catch (IOException e) {
            throw new DocumentStorageException("Failed to store file: " + cleanedFilename, e);
        } catch (NoSuchAlgorithmException e) {
            throw new DocumentStorageException("SHA-256 algorithm not available in current runtime environment.", e);
        }
    }

    @Override
    public Resource loadAsResource(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new DocumentStorageException("Storage path cannot be empty.", HttpStatus.BAD_REQUEST);
        }

        try {
            Path filePath = rootLocation.resolve(storagePath).normalize();
            if (!filePath.startsWith(rootLocation)) {
                throw new DocumentStorageException("Storage path escapes root storage directory.", HttpStatus.FORBIDDEN);
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new DocumentStorageException("Document file not found or not readable: " + storagePath, HttpStatus.NOT_FOUND);
            }
        } catch (MalformedURLException e) {
            throw new DocumentStorageException("Malformed file storage URI for path: " + storagePath, HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }

        try {
            Path filePath = rootLocation.resolve(storagePath).normalize();
            if (filePath.startsWith(rootLocation)) {
                Files.deleteIfExists(filePath);
                log.debug("Deleted physical file at: {}", filePath);
            }
        } catch (IOException e) {
            log.warn("Could not delete stored physical file [{}]: {}", storagePath, e.getMessage());
        }
    }

    @Override
    public boolean exists(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return false;
        }
        Path filePath = rootLocation.resolve(storagePath).normalize();
        return filePath.startsWith(rootLocation) && Files.exists(filePath);
    }
}
