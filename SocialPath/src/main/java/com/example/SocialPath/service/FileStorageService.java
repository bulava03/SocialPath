package com.example.SocialPath.service;

import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {
    String storeFile(MultipartFile file) throws IOException;
    GridFsResource getFileById(String fileId);
    void deleteFileById(String fileId);
    String convertGridFsFileToBase64(GridFsResource gridFsFile) throws IOException;
}
