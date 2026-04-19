package com.personal.alexamusic.handler;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.audioplayer.PlaybackNearlyFinishedRequest;
import com.amazon.ask.model.interfaces.audioplayer.PlaybackStartedRequest;
import com.amazon.ask.model.interfaces.audioplayer.PlaybackStoppedRequest;
import com.amazon.ask.model.interfaces.audioplayer.PlaybackFinishedRequest;
import com.amazon.ask.model.interfaces.audioplayer.PlaybackFailedRequest;
import com.amazon.ask.model.interfaces.audioplayer.PlayBehavior;
import com.amazon.ask.request.Predicates;
import com.personal.alexamusic.model.AudioTrack;
import com.personal.alexamusic.service.YoutubeMusicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.logging.Logger;

public class AudioPlayerHandlers {

    @Component
    public static class AudioPlayerEventHandler implements RequestHandler {

        private static final Logger LOGGER = Logger.getLogger(AudioPlayerEventHandler.class.getName());
        private final YoutubeMusicService youtubeMusicService;

        @Autowired
        public AudioPlayerEventHandler(YoutubeMusicService youtubeMusicService) {
            this.youtubeMusicService = youtubeMusicService;
        }

        @Override
        public boolean canHandle(HandlerInput input) {
            return input.matches(Predicates.requestType(PlaybackNearlyFinishedRequest.class)
                    .or(Predicates.requestType(PlaybackStartedRequest.class))
                    .or(Predicates.requestType(PlaybackStoppedRequest.class))
                    .or(Predicates.requestType(PlaybackFinishedRequest.class))
                    .or(Predicates.requestType(PlaybackFailedRequest.class)));
        }

        @Override
        public Optional<Response> handle(HandlerInput input) {
            if (input.matches(Predicates.requestType(PlaybackNearlyFinishedRequest.class))) {
                return handleNearlyFinished(input);
            } else if (input.matches(Predicates.requestType(PlaybackStartedRequest.class))) {
                PlaybackStartedRequest request = (PlaybackStartedRequest) input.getRequestEnvelope().getRequest();
                if (request.getToken() != null) {
                    YoutubeMusicService.setLastToken(request.getToken());
                }
            } else if (input.matches(Predicates.requestType(PlaybackFailedRequest.class))) {
                PlaybackFailedRequest request = (PlaybackFailedRequest) input.getRequestEnvelope().getRequest();
                LOGGER.warning("Playback falhou! Token: " + request.getToken()
                        + " | Erro: " + (request.getError() != null ? request.getError().getMessage() : "desconhecido"));
            }

            // Para os demais eventos, retornamos vazio para manter o servidor feliz.
            return input.getResponseBuilder().build();
        }

        /**
         * Quando a música atual está quase terminando, enfileira a próxima do Radio Mix.
         * Usa o cache da playlist para garantir que a próxima música seja relacionada.
         */
        private Optional<Response> handleNearlyFinished(HandlerInput input) {
            PlaybackNearlyFinishedRequest request = (PlaybackNearlyFinishedRequest) input.getRequestEnvelope().getRequest();
            String token = request.getToken();

            if (token == null || !token.contains(":")) {
                LOGGER.warning("Token inválido no PlaybackNearlyFinished: " + token);
                return input.getResponseBuilder().build();
            }

            String[] parts = token.split(":");
            String seedVideoId = parts[0];
            int currentIndex = Integer.parseInt(parts[1]);
            int nextIndex = currentIndex + 1;

            try {
                // getSongFromPlaylist usa o cache — não faz nova busca no YouTube
                // Apenas busca a URL de stream sob demanda
                AudioTrack nextTrack = youtubeMusicService.getSongFromPlaylist(seedVideoId, nextIndex);
                String nextToken = seedVideoId + ":" + nextIndex;

                LOGGER.info("Enfileirando próxima música: [" + nextIndex + "] " + nextTrack.getTitle());
                YoutubeMusicService.setLastToken(nextToken);

                return input.getResponseBuilder()
                        .addAudioPlayerPlayDirective(PlayBehavior.ENQUEUE, 0L, token, nextToken, nextTrack.getUrl())
                        .build();
            } catch (Exception e) {
                LOGGER.warning("Falha ao preparar próxima música (index " + nextIndex + "): " + e.getMessage());
                return input.getResponseBuilder().build();
            }
        }
    }
}
