package com.personal.alexamusic.service;

import com.personal.alexamusic.model.AudioTrack;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Service
public class YoutubeMusicService {

    private static final Logger LOGGER = Logger.getLogger(YoutubeMusicService.class.getName());

    // Armazena a última música que começou para evitar que comandos "Próxima" se percam.
    private static volatile String LAST_TOKEN = null;

    // Cache: seedVideoId → lista ordenada de faixas do Radio Mix
    // Isso garante que a playlist seja consistente entre chamadas
    private static final Map<String, List<TrackInfo>> PLAYLIST_CACHE = new ConcurrentHashMap<>();

    // Informação leve de uma faixa (sem URL de stream, que expira rápido)
    private static class TrackInfo {
        final String videoId;
        final String title;

        TrackInfo(String videoId, String title) {
            this.videoId = videoId;
            this.title = title;
        }
    }

    public static String getLastToken() {
        return LAST_TOKEN;
    }

    public static void setLastToken(String token) {
        LAST_TOKEN = token;
        LOGGER.info("LAST_TOKEN atualizado para: " + token);
    }

    /**
     * Busca uma música pelo nome no YouTube e retorna a primeira correspondência
     * com ID, título e URL de stream pronta para tocar.
     */
    public AudioTrack searchAndGetFirstSong(String songName) {
        LOGGER.info("Buscando música: " + songName);
        List<String> command = List.of(
                "yt-dlp",
                "--print", "id",
                "--print", "title",
                "--print", "urls",
                "-f", "bestaudio",
                "--no-playlist",
                "ytsearch1:" + songName
        );

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        AudioTrack track = executeYtDlpForTrack(processBuilder, "searchAndGetFirstSong");

        // Dispara carregamento do Radio Mix em background para esta seed
        String seedVideoId = track.getVideoId();
        Thread.startVirtualThread(() -> {
            try {
                loadRadioMix(seedVideoId);
            } catch (Exception e) {
                LOGGER.warning("Falha ao pré-carregar Radio Mix para " + seedVideoId + ": " + e.getMessage());
            }
        });

        return track;
    }

    /**
     * Carrega o Radio Mix inteiro (até 50 faixas) do YouTube para uma seed de vídeo.
     * Usa --flat-playlist para obter apenas IDs e títulos (rápido, sem downloads).
     * O resultado é guardado em cache para manter consistência.
     */
    public void loadRadioMix(String seedVideoId) {
        if (PLAYLIST_CACHE.containsKey(seedVideoId)) {
            LOGGER.info("Radio Mix já está em cache para: " + seedVideoId);
            return;
        }

        LOGGER.info("Carregando Radio Mix completo para seed: " + seedVideoId);
        String mixUrl = "https://www.youtube.com/watch?v=" + seedVideoId + "&list=RD" + seedVideoId;

        List<String> command = List.of(
                "yt-dlp",
                "--flat-playlist",
                "--print", "id",
                "--print", "title",
                "--playlist-end", "50",
                mixUrl
        );

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            List<String> validOutput = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("WARNING") || line.startsWith("ERROR") || line.startsWith("Extracting")
                        || line.startsWith("[") || line.startsWith("Downloading")) {
                    LOGGER.fine("yt-dlp log (loadRadioMix): " + line);
                } else if (!line.trim().isEmpty()) {
                    validOutput.add(line.trim());
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.warning("yt-dlp retornou exit code " + exitCode + " em loadRadioMix");
            }

            // Output vem em pares: id, title, id, title, ...
            List<TrackInfo> tracks = new ArrayList<>();
            for (int i = 0; i + 1 < validOutput.size(); i += 2) {
                String id = validOutput.get(i);
                String title = validOutput.get(i + 1);
                // Filtrar linhas que claramente não são IDs de vídeo (11 chars alfanuméricos)
                if (id.length() >= 8 && id.length() <= 16 && !id.contains(" ") && !id.contains("/")) {
                    tracks.add(new TrackInfo(id, title));
                }
            }

            if (!tracks.isEmpty()) {
                PLAYLIST_CACHE.put(seedVideoId, tracks);
                LOGGER.info("Radio Mix carregado com " + tracks.size() + " faixas para seed: " + seedVideoId);
                for (int i = 0; i < Math.min(5, tracks.size()); i++) {
                    LOGGER.info("  [" + (i + 1) + "] " + tracks.get(i).title + " (" + tracks.get(i).videoId + ")");
                }
            } else {
                LOGGER.warning("Não foi possível extrair faixas do Radio Mix. Output: " + validOutput);
            }
        } catch (Exception e) {
            LOGGER.severe("Erro ao carregar Radio Mix: " + e.getMessage());
        }
    }

    /**
     * Busca a URL de stream para um vídeo específico do YouTube.
     * URLs de stream expiram rápido (~6h), então buscamos sob demanda.
     */
    public String getStreamUrl(String videoId) {
        LOGGER.info("Buscando URL de stream para: " + videoId);
        List<String> command = List.of(
                "yt-dlp",
                "--print", "urls",
                "-f", "bestaudio",
                "--no-playlist",
                "https://www.youtube.com/watch?v=" + videoId
        );

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String streamUrl = null;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
                    streamUrl = trimmed;
                } else if (!trimmed.isEmpty()) {
                    LOGGER.fine("yt-dlp log (getStreamUrl): " + trimmed);
                }
            }

            process.waitFor();

            if (streamUrl != null) {
                return streamUrl;
            }
            throw new RuntimeException("Não foi possível extrair URL de stream para vídeo: " + videoId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processo interrompido ao buscar stream URL", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao buscar stream URL", e);
        }
    }

    /**
     * Obtém uma faixa específica do Radio Mix pelo índice.
     * Usa o cache da playlist para manter a ordem consistente.
     * Se o cache não existe ainda, carrega o Mix primeiro.
     *
     * @param seedVideoId O ID do vídeo que iniciou o Radio Mix
     * @param index       Índice da faixa (1-based, onde 1 = a seed original)
     * @return AudioTrack com URL de stream pronta para tocar
     */
    public AudioTrack getSongFromPlaylist(String seedVideoId, int index) {
        LOGGER.info("Buscando faixa do Mix. Seed: " + seedVideoId + " | Índice: " + index);

        // Garante que o cache existe
        if (!PLAYLIST_CACHE.containsKey(seedVideoId)) {
            loadRadioMix(seedVideoId);
        }

        List<TrackInfo> playlist = PLAYLIST_CACHE.get(seedVideoId);
        if (playlist == null || playlist.isEmpty()) {
            throw new RuntimeException("Não foi possível carregar o Radio Mix para: " + seedVideoId);
        }

        // Converte index 1-based para 0-based
        // Se index exceder a lista, faz loop (volta ao início)
        int adjustedIndex = ((index - 1) % playlist.size());
        if (adjustedIndex < 0) adjustedIndex += playlist.size();

        TrackInfo trackInfo = playlist.get(adjustedIndex);
        LOGGER.info("Faixa selecionada: [" + (adjustedIndex + 1) + "/" + playlist.size() + "] "
                + trackInfo.title + " (" + trackInfo.videoId + ")");

        // Busca a URL de stream sob demanda (elas expiram)
        String streamUrl = getStreamUrl(trackInfo.videoId);

        return new AudioTrack(trackInfo.videoId, trackInfo.title, streamUrl);
    }

    /**
     * Limpa o cache de uma playlist específica (útil se quiser forçar recarregamento).
     */
    public void clearPlaylistCache(String seedVideoId) {
        PLAYLIST_CACHE.remove(seedVideoId);
        LOGGER.info("Cache limpo para seed: " + seedVideoId);
    }

    /**
     * Executa yt-dlp e extrai um AudioTrack (id, title, url) do output.
     */
    private AudioTrack executeYtDlpForTrack(ProcessBuilder processBuilder, String context) {
        try {
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            List<String> validOutput = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("WARNING") || line.startsWith("ERROR") || line.startsWith("Extracting")
                        || line.startsWith("[") || line.startsWith("Downloading")) {
                    LOGGER.info("yt-dlp log: " + line);
                } else if (!line.trim().isEmpty()) {
                    validOutput.add(line.trim());
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.warning("yt-dlp retornou exit code " + exitCode + " no método " + context);
            }

            // Esperamos 3 linhas de output na ordem: id, title, url
            if (validOutput.size() >= 3) {
                String url = validOutput.get(validOutput.size() - 1);
                String title = validOutput.get(validOutput.size() - 2);
                String id = validOutput.get(validOutput.size() - 3);

                if (url.startsWith("https://") || url.startsWith("http://")) {
                    return new AudioTrack(id, title, url);
                }
            }

            throw new RuntimeException("Falha ao extrair ID, Título e URL. Output recebido: " + validOutput);
        } catch (Exception e) {
            LOGGER.severe("Erro interno acessando yt-dlp (" + context + "): " + e.getMessage());
            throw new RuntimeException("Falha no yt-dlp", e);
        }
    }
}
