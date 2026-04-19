package com.personal.alexamusic.handler;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.audioplayer.AudioPlayerState;
import com.amazon.ask.model.interfaces.audioplayer.PlayBehavior;
import com.amazon.ask.request.Predicates;
import com.personal.alexamusic.model.AudioTrack;
import com.personal.alexamusic.service.YoutubeMusicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.logging.Logger;

public class ControlHandlers {

    private static final Logger LOGGER = Logger.getLogger(ControlHandlers.class.getName());

    /**
     * Resolve o token atual a partir do AudioPlayerState ou do fallback global.
     */
    private static String resolveToken(HandlerInput input) {
        AudioPlayerState audioPlayer = input.getRequestEnvelope().getContext().getAudioPlayer();
        if (audioPlayer != null && audioPlayer.getToken() != null) {
            return audioPlayer.getToken();
        }
        return YoutubeMusicService.getLastToken();
    }

    /**
     * Extrai o seedVideoId do token (formato: "seedVideoId:index").
     */
    private static String getSeedVideoId(String token) {
        return token.split(":")[0];
    }

    /**
     * Extrai o index do token (formato: "seedVideoId:index").
     */
    private static int getIndex(String token) {
        return Integer.parseInt(token.split(":")[1]);
    }

    @Component
    public static class NextIntentHandler implements RequestHandler {
        private final YoutubeMusicService youtubeMusicService;

        @Autowired
        public NextIntentHandler(YoutubeMusicService youtubeMusicService) {
            this.youtubeMusicService = youtubeMusicService;
        }

        @Override
        public boolean canHandle(HandlerInput input) {
            return input.matches(Predicates.intentName("AMAZON.NextIntent"));
        }

        @Override
        public Optional<Response> handle(HandlerInput input) {
            String token = resolveToken(input);

            if (token == null || !token.contains(":")) {
                return input.getResponseBuilder()
                        .withSpeech("Não há música tocando para pular. Peça para tocar uma música primeiro.")
                        .build();
            }

            String seedVideoId = getSeedVideoId(token);
            int nextIndex = getIndex(token) + 1;

            try {
                AudioTrack nextTrack = youtubeMusicService.getSongFromPlaylist(seedVideoId, nextIndex);
                String nextToken = seedVideoId + ":" + nextIndex;
                YoutubeMusicService.setLastToken(nextToken);

                LOGGER.info("Pulando para: [" + nextIndex + "] " + nextTrack.getTitle());

                return input.getResponseBuilder()
                        .addAudioPlayerPlayDirective(PlayBehavior.REPLACE_ALL, 0L, null, nextToken, nextTrack.getUrl())
                        .build();
            } catch (Exception e) {
                LOGGER.severe("Erro ao pular música: " + e.getMessage());
                return input.getResponseBuilder()
                        .withSpeech("Ocorreu um erro ao pular a música.")
                        .build();
            }
        }
    }

    @Component
    public static class PreviousIntentHandler implements RequestHandler {
        private final YoutubeMusicService youtubeMusicService;

        @Autowired
        public PreviousIntentHandler(YoutubeMusicService youtubeMusicService) {
            this.youtubeMusicService = youtubeMusicService;
        }

        @Override
        public boolean canHandle(HandlerInput input) {
            return input.matches(Predicates.intentName("AMAZON.PreviousIntent"));
        }

        @Override
        public Optional<Response> handle(HandlerInput input) {
            String token = resolveToken(input);

            if (token == null || !token.contains(":")) {
                return input.getResponseBuilder()
                        .withSpeech("Não há lista para voltar.")
                        .build();
            }

            String seedVideoId = getSeedVideoId(token);
            int currentIndex = getIndex(token);

            if (currentIndex <= 1) {
                return input.getResponseBuilder()
                        .withSpeech("Você já está na primeira música.")
                        .build();
            }

            int prevIndex = currentIndex - 1;

            try {
                AudioTrack prevTrack = youtubeMusicService.getSongFromPlaylist(seedVideoId, prevIndex);
                String prevToken = seedVideoId + ":" + prevIndex;
                YoutubeMusicService.setLastToken(prevToken);

                LOGGER.info("Voltando para: [" + prevIndex + "] " + prevTrack.getTitle());

                return input.getResponseBuilder()
                        .addAudioPlayerPlayDirective(PlayBehavior.REPLACE_ALL, 0L, null, prevToken, prevTrack.getUrl())
                        .build();
            } catch (Exception e) {
                LOGGER.severe("Erro ao voltar música: " + e.getMessage());
                return input.getResponseBuilder()
                        .withSpeech("Ocorreu um erro ao voltar a música.")
                        .build();
            }
        }
    }

    @Component
    public static class ResumeIntentHandler implements RequestHandler {
        private final YoutubeMusicService youtubeMusicService;

        @Autowired
        public ResumeIntentHandler(YoutubeMusicService youtubeMusicService) {
            this.youtubeMusicService = youtubeMusicService;
        }

        @Override
        public boolean canHandle(HandlerInput input) {
            return input.matches(Predicates.intentName("AMAZON.ResumeIntent"));
        }

        @Override
        public Optional<Response> handle(HandlerInput input) {
            AudioPlayerState audioPlayer = input.getRequestEnvelope().getContext().getAudioPlayer();
            if (audioPlayer == null || audioPlayer.getToken() == null) {
                return input.getResponseBuilder()
                        .withSpeech("Não consegui encontrar a música pausada para retomar.")
                        .build();
            }

            String token = audioPlayer.getToken();
            long offset = audioPlayer.getOffsetInMilliseconds() != null ? audioPlayer.getOffsetInMilliseconds() : 0L;

            if (!token.contains(":")) {
                return input.getResponseBuilder()
                        .withSpeech("Não consegui identificar a música para retomar.")
                        .build();
            }

            String seedVideoId = getSeedVideoId(token);
            int index = getIndex(token);

            try {
                // Busca a URL de stream novamente (pode ter expirado durante a pausa)
                AudioTrack track = youtubeMusicService.getSongFromPlaylist(seedVideoId, index);

                LOGGER.info("Retomando: [" + index + "] " + track.getTitle() + " em offset " + offset + "ms");

                return input.getResponseBuilder()
                        .addAudioPlayerPlayDirective(PlayBehavior.REPLACE_ALL, offset, null, token, track.getUrl())
                        .build();
            } catch (Exception e) {
                LOGGER.severe("Erro ao retomar música: " + e.getMessage());
                return input.getResponseBuilder()
                        .withSpeech("Ocorreu um erro ao retomar a música.")
                        .build();
            }
        }
    }
}
