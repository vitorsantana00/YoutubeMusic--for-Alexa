package com.personal.alexamusic.service;

import com.personal.alexamusic.model.AudioTrack;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class YoutubeMusicService {
    
    private static final Logger LOGGER = Logger.getLogger(YoutubeMusicService.class.getName());

    public AudioTrack searchAndGetFirstSong(String songName) {
        LOGGER.info("Buscando música: " + songName);
        ProcessBuilder processBuilder = new ProcessBuilder(
                "yt-dlp",
                "--print", "id",
                "--print", "title",
                "--print", "urls",
                "-f", "bestaudio",
                "ytsearch1:" + songName
        );
        return executeYtDlp(processBuilder, "searchAndGetFirstSong");
    }

    public AudioTrack getSongFromPlaylist(String seedVideoId, int index) {
        LOGGER.info("Buscando prox música do Mix. Raiz: " + seedVideoId + " | Index: " + index);
        ProcessBuilder processBuilder = new ProcessBuilder(
                "yt-dlp",
                "--playlist-items", String.valueOf(index),
                "--print", "id",
                "--print", "title",
                "--print", "urls",
                "-f", "bestaudio",
                "https://www.youtube.com/watch?v=" + seedVideoId + "&list=RD" + seedVideoId
        );
        return executeYtDlp(processBuilder, "getSongFromPlaylist");
    }

    private AudioTrack executeYtDlp(ProcessBuilder processBuilder, String context) {
        try {
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            List<String> validOutput = new ArrayList<>();
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("WARNING") || line.startsWith("ERROR")) {
                    LOGGER.info("yt-dlp log: " + line);
                } else if (!line.trim().isEmpty()) {
                    validOutput.add(line.trim());
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.warning("yt-dlp retornou exit code " + exitCode + " no metodo " + context);
            }
            
            // Esperamos 3 linhas de output na ordem: id, title, url
            if (validOutput.size() >= 3) {
                // Como pegamos de trás pra frente caso haja outputs sujos no começo:
                String url = validOutput.get(validOutput.size() - 1);
                String title = validOutput.get(validOutput.size() - 2);
                String id = validOutput.get(validOutput.size() - 3);
                
                if ((url.startsWith("https://") || url.startsWith("http://"))) {
                    return new AudioTrack(id, title, url);
                }
            }
            
            throw new RuntimeException("Falha ao extrair ID, Titulo e URL. Output recebido: " + validOutput);
        } catch (Exception e) {
            LOGGER.severe("Erro interno acessando yt-dlp (" + context + "): " + e.getMessage());
            throw new RuntimeException("Falha no yt-dlp", e);
        }
    }
}
