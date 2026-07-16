package com.socialpath.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Stores and retrieves user-uploaded media on the local file system (a Docker
 * volume in the compose setup). File ids are generated file names of the form
 * {uuid}.{ext} and are what entities reference as their imageId / media
 * entries. Replaces the GridFS storage of the MongoDB edition.
 */
public interface FileStorageService {

    /**
     * Stores an uploaded file, keeping its extension so the content type can
     * be derived when serving it back.
     * @param file the uploaded file
     * @return the new file's id
     */
    String storeFile(MultipartFile file) throws IOException;

    /**
     * Retrieves a stored file by id for streaming.
     * @param fileId the file id
     * @return the resource, or null if no file has that id
     */
    Resource getFileById(String fileId);

    /**
     * Reports whether a stored file is a video (used to pick the render tag).
     * @param fileId the file id
     * @return true if the file exists and is an mp4
     */
    boolean isVideo(String fileId);

    /**
     * Deletes a stored file by id. No-op if the file does not exist.
     * @param fileId the file id
     */
    void deleteFileById(String fileId);
}
