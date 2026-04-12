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
                PlaybackNearlyFinishedRequest request = (PlaybackNearlyFinishedRequest) input.getRequestEnvelope().getRequest();
                String token = request.getToken();
                
                if (token != null && token.contains(":")) {
                    String[] parts = token.split(":");
                    String seedVideoId = parts[0];
                    int index = Integer.parseInt(parts[1]);
                    
                    try {
                        int nextIndex = index + 1;
                        AudioTrack nextTrack = youtubeMusicService.getSongFromPlaylist(seedVideoId, nextIndex);
                        String nextToken = seedVideoId + ":" + nextIndex;
                        
                        LOGGER.info("Enfileirando prox musica: " + nextTrack.getTitle());
                        YoutubeMusicService.setLastToken(nextToken);
                        
                        return input.getResponseBuilder()
                                .addAudioPlayerPlayDirective(PlayBehavior.ENQUEUE, 0L, token, nextToken, nextTrack.getUrl())
                                .build();
                    } catch (Exception e) {
                        LOGGER.warning("Falha ao preparar proxima musica: " + e.getMessage());
                    }
                }
            } else if (input.matches(Predicates.requestType(PlaybackStartedRequest.class))) {
                PlaybackStartedRequest request = (PlaybackStartedRequest) input.getRequestEnvelope().getRequest();
                if (request.getToken() != null) {
                    YoutubeMusicService.setLastToken(request.getToken());
                }
            }

            // Para os demais eventos, retornamos vazio para manter o servidor feliz.
            return input.getResponseBuilder().build();
        }
    }
}

