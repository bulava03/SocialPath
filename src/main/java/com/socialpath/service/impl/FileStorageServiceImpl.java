package com.socialpath.service.impl;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.socialpath.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final GridFsTemplate gridFsTemplate;
    private final GridFsOperations gridFsOperations;

    @Override
    public String storeFile(MultipartFile file) throws IOException {
        return gridFsTemplate.store(
                file.getInputStream(),
                Objects.requireNonNull(file.getOriginalFilename()),
                file.getContentType()).toString();
    }

    @Override
    public GridFsResource getFileById(String fileId) {
        GridFSFile gridFsFile = gridFsTemplate.findOne(byId(fileId));
        // The null check is intentional despite IDE/Sonar flagging it as always
        // false: GridFsOperations.findOne is documented to return null when no
        // file matches, but its package's @NonNullApi default is not overridden
        // with @Nullable on this method, so static analysis wrongly infers
        // non-null. A dangling imageId (pointing at an already-deleted file)
        // really does yield null here, and dropping the check would NPE on the
        // next line; callers rely on null meaning "no image".
        return gridFsFile == null ? null : gridFsOperations.getResource(gridFsFile);
    }

    @Override
    public boolean isVideo(String fileId) {
        // See getFileById: findOne can return null in practice (missing file),
        // even though static analysis flags this check as always true.
        GridFSFile gridFsFile = gridFsTemplate.findOne(byId(fileId));
        return gridFsFile != null && gridFsFile.getFilename().toLowerCase().endsWith(".mp4");
    }

    @Override
    public void deleteFileById(String fileId) {
        gridFsTemplate.delete(byId(fileId));
    }

    /**
     * Builds an id query for a GridFS file. The stored id is a String form of
     * an ObjectId (see storeFile), so it is parsed back to ObjectId here to
     * match the type of the _id field consistently across every lookup.
     * @param fileId string form of the GridFS file's ObjectId
     * @return a query matching that single file
     */
    private Query byId(String fileId) {
        return new Query(Criteria.where("_id").is(new ObjectId(fileId)));
    }
}
