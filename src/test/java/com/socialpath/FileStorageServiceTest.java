package com.socialpath;

import com.socialpath.service.impl.FileStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * File ids end up in URLs (/images/{id}) and are therefore attacker-controlled:
 * nothing outside the media directory may ever be read, written or deleted.
 */
class FileStorageServiceTest {

    private static final String ID = "0f8fad5b-d9cb-469f-a165-70867728950e.jpg";

    @TempDir
    Path sandbox;

    private Path mediaDir;
    private Path secret;
    private FileStorageServiceImpl storage;

    @BeforeEach
    void setUp() throws IOException {
        mediaDir = sandbox.resolve("media");
        secret = Files.writeString(sandbox.resolve("secret.txt"), "outside the media directory");
        storage = new FileStorageServiceImpl(mediaDir.toString());
    }

    @Test
    void storedFile_IsServedBackByItsId() throws IOException {
        storage.storeFile(ID, "picture".getBytes(StandardCharsets.UTF_8));

        Resource resource = storage.getFileById(ID);

        assertNotNull(resource);
        assertEquals("picture", new String(resource.getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void thumbnailIds_AreValid() throws IOException {
        String thumbnailId = "0f8fad5b-d9cb-469f-a165-70867728950e_thumb.jpg";
        storage.storeFile(thumbnailId, new byte[]{1});

        assertNotNull(storage.getFileById(thumbnailId));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "../secret.txt",
            "..\\secret.txt",
            "..%2Fsecret.txt",
            "/etc/passwd",
            "C:\\Windows\\win.ini",
            "secret.txt",
            "0f8fad5b-d9cb-469f-a165-70867728950e/../../secret.txt",
            ""
    })
    void getFileById_MalformedIds_ReachNothing(String id) {
        assertNull(storage.getFileById(id));
    }

    @Test
    void deleteFileById_CannotDeleteOutsideTheMediaDirectory() {
        storage.deleteFileById("../secret.txt");

        assertTrue(Files.exists(secret));
    }

    @Test
    void storeFile_UnderMalformedId_IsRefusedAndWritesNothing() {
        assertThrows(IllegalArgumentException.class,
                () -> storage.storeFile("../planted.sh", new byte[]{1}));

        assertFalse(Files.exists(sandbox.resolve("planted.sh")));
    }

    @Test
    void storeFile_UploadNameIsIgnored() throws IOException {
        MockMultipartFile upload = new MockMultipartFile(
                "media", "../../planted.html", "text/html", new byte[]{1, 2, 3});

        String id = storage.storeFile(upload, "jpg");

        // A fresh server-generated name with the extension the server chose.
        assertTrue(id.matches("[a-f0-9-]{36}\\.jpg"), id);
        assertTrue(Files.exists(mediaDir.resolve(id)));
        assertFalse(Files.exists(sandbox.resolve("planted.html")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../x", "j/pg", "", "HTML!"})
    void storeFile_MalformedExtension_IsRefused(String extension) {
        MockMultipartFile upload = new MockMultipartFile("media", "a.jpg", "image/jpeg", new byte[]{1});

        assertThrows(IllegalArgumentException.class, () -> storage.storeFile(upload, extension));
    }
}
