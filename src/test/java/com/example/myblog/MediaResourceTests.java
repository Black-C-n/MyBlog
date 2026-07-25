package com.example.myblog;

import com.example.myblog.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MediaResourceTests {

    @LocalServerPort
    private int port;

    @Autowired
    private FileStorageService fileStorageService;

    @Test
    void originalBlogImageIsServedFromJavaProject() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/media/images/user/1.png"))
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("image/png"));
        assertTrue(response.body().length > 0);
    }

    @Test
    void loginPageBackgroundImageUsesSpringStaticPath() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/images/1.jpg"))
                .GET()
                .build();

        HttpResponse<byte[]> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofByteArray());

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("image/jpeg"));
        assertTrue(response.body().length > 0);
    }

    @Test
    void newlyUploadedImageIsServedFromProjectStorage() throws Exception {
        byte[] imageBytes;
        try (InputStream inputStream = getClass().getResourceAsStream(
                "/static/media/images/article/5.png")) {
            if (inputStream == null) {
                throw new IllegalStateException("测试图片不存在");
            }
            imageBytes = inputStream.readAllBytes();
        }

        MockMultipartFile image = new MockMultipartFile(
                "photoFile", "test.png", "image/png", imageBytes);
        String relativePath = fileStorageService.store(image, "images/test");
        Path storedFile = Path.of("target/test-uploads").toAbsolutePath().normalize().resolve(relativePath);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/media/" + relativePath))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofByteArray());

            assertEquals(200, response.statusCode());
            assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("image/png"));
            assertArrayEquals(imageBytes, response.body());
        } finally {
            Files.deleteIfExists(storedFile);
        }
    }
}
