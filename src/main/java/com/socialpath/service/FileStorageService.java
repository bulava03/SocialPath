package com.socialpath.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Stores and retrieves user-uploaded media on the local file system (a Docker
 * volume in the compose setup). File ids are generated file names of the form
 * {uuid}.{ext} and are what entities reference as their imageId / media
 * entries. Replaces the GridFS storage of the MongoDB edition.
 */
public interface FileStorageService {

    /**
     * Stores an uploaded file under a fresh id with the given extension. The
     * extension comes from the type the server detected in the content, never
     * from the name the client sent, because it decides how the file is
     * served back.
     * @param file the uploaded file
     * @param extension the extension for the stored file, without a dot
     * @return the new file's id
     */
    String storeFile(MultipartFile file, String extension) throws IOException;

    /**
     * Stores content the server generated itself (such as a resized avatar)
     * under an id chosen by the caller.
     * @param fileId the id to store under, in the same {uuid}.{ext} form
     * @param content the file content
     */
    void storeFile(String fileId, byte[] content) throws IOException;

    /**
     * The location of a stored file, for tools that work on files (ffmpeg).
     * @param fileId a valid file id
     * @return its path inside the media directory, whether or not it exists yet
     * @throws IllegalArgumentException for a malformed id
     */
    Path pathOf(String fileId);

    /**
     * Moves a finished file into storage under the given id. The file
     * appears under that id complete or not at all, so it is never served
     * half written.
     * @param file a file outside the media directory, such as a temporary one
     * @param fileId the id to store it under
     */
    void moveIn(Path file, String fileId) throws IOException;

    /**
     * Retrieves a stored file by id for streaming.
     * @param fileId the file id
     * @return the resource, or null if no file has that id
     */
    Resource getFileById(String fileId);

    /**
     * Reports whether a stored file is a video (used to pick the render tag).
     * @param fileId the file id
     * @return true if the file exists and has a video format's extension
     */
    boolean isVideo(String fileId);

    /**
     * Deletes a stored file by id. No-op if the file does not exist.
     * @param fileId the file id
     */
    void deleteFileById(String fileId);
}
