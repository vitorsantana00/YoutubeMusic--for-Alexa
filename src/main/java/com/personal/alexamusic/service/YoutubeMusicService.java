package com.personal.alexamusic.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Logger;

@Service
public class YoutubeMusicService {
    
    private static final Logger LOGGER = Logger.getLogger(YoutubeMusicService.class.getName());

    public String searchAndGetStreamUrl(String songName) {
        LOGGER.info("Buscando música: " + songName);
        try {
            // Executando yt-dlp para buscar e retornar o link de streaming
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "yt-dlp",
                    "-f", "bestaudio",
                    "-g",
                    "ytsearch1:" + songName
            );
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String streamUrl = null;
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("https://") || line.startsWith("http://")) {
                    streamUrl = line.trim();
                } else {
                    LOGGER.info("yt-dlp output: " + line);
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.warning("yt-dlp retornou erro no processo. Código: " + exitCode);
            }
            
            if (streamUrl != null && !streamUrl.isEmpty()) {
                LOGGER.info("URL obtida com sucesso.");
                return streamUrl;
            }
            
            throw new RuntimeException("Não foi possível encontrar a URL. Verifique logs do yt-dlp.");
        } catch (Exception e) {
            LOGGER.severe("Erro interno acessando yt-dlp: " + e.getMessage());
            throw new RuntimeException("Falha ao buscar música", e);
        }
    }
}
