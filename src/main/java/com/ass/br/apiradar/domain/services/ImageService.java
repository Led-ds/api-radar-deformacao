package com.ass.br.apiradar.domain.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
@RequiredArgsConstructor
public class ImageService {

    public byte[] imageBufferize(String imageUrl) throws IOException {
        // Cria uma URL a partir da string
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        try (InputStream inputStream = connection.getInputStream(); ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];

            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            return byteArrayOutputStream.toByteArray();
        }
    }

    public MultipartFile imageToMultipartFile(byte[] imageBuffer, String imageName) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return imageName;
            }

            @Override
            public String getOriginalFilename() {
                return imageName;
            }

            @Override
            public String getContentType() {
                return "image/png";
            }

            @Override
            public boolean isEmpty() {
                return imageBuffer.length == 0;
            }

            @Override
            public long getSize() {
                return imageBuffer.length;
            }

            @Override
            public byte[] getBytes() {
                return imageBuffer;
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(imageBuffer);
            }

            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException {
                try (FileOutputStream fileOutputStream = new FileOutputStream(dest)) {
                    fileOutputStream.write(imageBuffer);
                }
            }
        };
    }

}
