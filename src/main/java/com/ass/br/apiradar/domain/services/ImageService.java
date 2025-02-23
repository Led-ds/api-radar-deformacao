package com.ass.br.apiradar.domain.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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


}
