package com.socialpath;

import com.socialpath.service.FileStorageService;
import com.socialpath.web.ImageController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageControllerTest {

    private static final String ID = "0f8fad5b-d9cb-469f-a165-70867728950e.jpg";
    private static final String THUMB_ID = "0f8fad5b-d9cb-469f-a165-70867728950e_thumb.jpg";

    private FileStorageService fileStorageService;
    private ImageController controller;

    @BeforeEach
    void setUp() {
        fileStorageService = mock(FileStorageService.class);
        controller = new ImageController(fileStorageService);
    }

    private static Resource named(String filename) {
        return new ByteArrayResource(new byte[]{1}) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    @Test
    void thumbnailRequested_AndPresent_ServesTheThumbnail() throws Exception {
        Resource thumbnail = named(THUMB_ID);
        when(fileStorageService.getFileById(THUMB_ID)).thenReturn(thumbnail);

        ResponseEntity<Resource> response = controller.getImage(ID, "thumb");

        assertSame(thumbnail, response.getBody());
    }

    @Test
    void thumbnailRequested_ButMissing_FallsBackToTheFullFile() throws Exception {
        // Avatars not converted yet and publication media have no thumbnail.
        Resource full = named(ID);
        when(fileStorageService.getFileById(ID)).thenReturn(full);

        ResponseEntity<Resource> response = controller.getImage(ID, "thumb");

        assertSame(full, response.getBody());
        assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
    }

    @Test
    void unknownFile_IsNotFound() throws Exception {
        assertEquals(HttpStatus.NOT_FOUND, controller.getImage(ID, null).getStatusCode());
    }

    @Test
    void video_IsServedAsMp4() throws Exception {
        String videoId = "0f8fad5b-d9cb-469f-a165-70867728950e.mp4";
        when(fileStorageService.getFileById(videoId)).thenReturn(named(videoId));

        ResponseEntity<Resource> response = controller.getImage(videoId, null);

        assertEquals(MediaType.parseMediaType("video/mp4"), response.getHeaders().getContentType());
    }

    @Test
    void storedHtml_IsOfferedAsADownload_NeverRenderedAsAPage() throws Exception {
        // Files saved before uploads were checked may have any extension.
        String htmlId = "0f8fad5b-d9cb-469f-a165-70867728950e.html";
        when(fileStorageService.getFileById(htmlId)).thenReturn(named(htmlId));

        ResponseEntity<Resource> response = controller.getImage(htmlId, null);

        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        assertEquals("attachment", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals("sandbox", response.getHeaders().getFirst("Content-Security-Policy"));
    }

    @Test
    void pictures_AreShownInline_InASandbox() throws Exception {
        when(fileStorageService.getFileById(ID)).thenReturn(named(ID));

        ResponseEntity<Resource> response = controller.getImage(ID, null);

        assertNull(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals("sandbox", response.getHeaders().getFirst("Content-Security-Policy"));
    }
}
