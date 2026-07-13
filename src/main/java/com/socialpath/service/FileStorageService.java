package com.socialpath.service;

import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Stores and retrieves user-uploaded media in MongoDB GridFS. File ids are the
 * string form of the GridFS ObjectId and are what documents reference as their
 * imageId / media entries.
 */
public interface FileStorageService {

    /**
     * Stores an uploaded file in GridFS, preserving its content type.
     * @param file the uploaded file
     * @return the new file's id, as a string
     */
    String storeFile(MultipartFile file) throws IOException;

    /**
     * Retrieves a stored file by id for streaming.
     * @param fileId the file id
     * @return the resource, or null if no file has that id
     */
    GridFsResource getFileById(String fileId);

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
