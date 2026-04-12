package com.personal.alexamusic.service;

import com.personal.alexamusic.model.AudioTrack;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class YoutubeMusicService {
    
    private static final Logger LOGGER = Logger.getLogger(YoutubeMusicService.class.getName());
    
    // Armazena a última música que comecou para evitar que comandos "Próxima" se percam.
    private static String LAST_TOKEN = null;
    
    public static String getLastToken() {
        return LAST_TOKEN;
    }
    
    public static void setLastToken(String token) {
        LAST_TOKEN = token;
        LOGGER.info("LAST_TOKEN atualizado para: " + token);
    }

    public AudioTrack searchAndGetFirstSong(String songName) {
        LOGGER.info("Buscando música: " + songName);
        List<String> command = new ArrayList<>(List.of(
                "yt-dlp",
                "--print", "id",
                "--print", "title",
                "--print", "urls",
                "-f", "bestaudio"
        ));
        
        command.add("ytsearch1:" + songName);
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        return executeYtDlp(processBuilder, "searchAndGetFirstSong");
    }

    public AudioTrack getSongFromPlaylist(String seedVideoId, int index) {
        LOGGER.info("Buscando prox música do Mix. Raiz: " + seedVideoId + " | Index: " + index);
        List<String> command = new ArrayList<>(List.of(
                "yt-dlp",
                "--playlist-items", String.valueOf(index),
                "--print", "id",
                "--print", "title",
                "--print", "urls",
                "-f", "bestaudio"
        ));
        
        command.add("https://www.youtube.com/watch?v=" + seedVideoId + "&list=RD" + seedVideoId);
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        return executeYtDlp(processBuilder, "getSongFromPlaylist");
    }

    // Removida inteçao de cookies devido a bloqueio do YouTube por Challenge PO

    private AudioTrack executeYtDlp(ProcessBuilder processBuilder, String context) {
        try {
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            List<String> validOutput = new ArrayList<>();
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("WARNING") || line.startsWith("ERROR") || line.startsWith("Extracting")) {
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
