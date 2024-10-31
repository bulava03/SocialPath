package com.example.SocialPath.service.impl;

import com.example.SocialPath.service.FileStorageService;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Objects;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Autowired
    private GridFsTemplate gridFsTemplate;
    @Autowired
    private GridFsOperations gridFsOperations;

    public String storeFile(MultipartFile file) throws IOException {
        return gridFsTemplate.store(file.getInputStream(), Objects.requireNonNull(file.getOriginalFilename())).toString();
    }

    public GridFsResource getFileById(String fileId) {
        GridFSFile gridFsFile = gridFsTemplate.findOne(new org.springframework.data.mongodb.core.query.Query().addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("_id").is(fileId)));
        assert gridFsFile != null;
        return gridFsOperations.getResource(gridFsFile);
    }

    public void deleteFileById(String fileId) {
        ObjectId objectId = new ObjectId(fileId);
        gridFsTemplate.delete(new Query(Criteria.where("_id").is(objectId)));
    }

    public String convertGridFsFileToBase64(GridFsResource gridFsFile) throws IOException {
        try (InputStream inputStream = gridFsFile.getInputStream()) {
            byte[] fileBytes = inputStream.readAllBytes();
            return Base64.getEncoder().encodeToString(fileBytes);
        }
    }

}
