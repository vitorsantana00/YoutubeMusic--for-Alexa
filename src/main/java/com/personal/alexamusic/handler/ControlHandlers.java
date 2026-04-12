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

public class ControlHandlers {

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
            AudioPlayerState audioPlayer = input.getRequestEnvelope().getContext().getAudioPlayer();
            String token = (audioPlayer != null && audioPlayer.getToken() != null) ? audioPlayer.getToken() : YoutubeMusicService.getLastToken();
            
            if (token != null && token.contains(":")) {
                String[] parts = token.split(":");
                String seedVideoId = parts[0];
                int index = Integer.parseInt(parts[1]);
                
                try {
                    int nextIndex = index + 1;
                    AudioTrack nextTrack = youtubeMusicService.getSongFromPlaylist(seedVideoId, nextIndex);
                    String nextToken = seedVideoId + ":" + nextIndex;
                    
                    YoutubeMusicService.setLastToken(nextToken);
                    
                    return input.getResponseBuilder()
                            .addAudioPlayerPlayDirective(PlayBehavior.REPLACE_ALL, 0L, null, nextToken, nextTrack.getUrl())
                            .build();
                } catch (Exception e) {
                    return input.getResponseBuilder().withSpeech("Ocorreu um erro ao pular a música.").build();
                }
            }
            return input.getResponseBuilder().withSpeech("Não há música tocando para pular.").build();
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
            AudioPlayerState audioPlayer = input.getRequestEnvelope().getContext().getAudioPlayer();
            String token = (audioPlayer != null && audioPlayer.getToken() != null) ? audioPlayer.getToken() : YoutubeMusicService.getLastToken();
            
            if (token != null && token.contains(":")) {
                String[] parts = token.split(":");
                String seedVideoId = parts[0];
                int index = Integer.parseInt(parts[1]);
                
                if (index > 1) {
                    try {
                        int prevIndex = index - 1;
                        AudioTrack prevTrack = youtubeMusicService.getSongFromPlaylist(seedVideoId, prevIndex);
                        String prevToken = seedVideoId + ":" + prevIndex;
                        
                        YoutubeMusicService.setLastToken(prevToken);
                        
                        return input.getResponseBuilder()
                                .addAudioPlayerPlayDirective(PlayBehavior.REPLACE_ALL, 0L, null, prevToken, prevTrack.getUrl())
                                .build();
                    } catch (Exception e) {
                        return input.getResponseBuilder().withSpeech("Ocorreu um erro ao voltar a música.").build();
                    }
                } else {
                    return input.getResponseBuilder().withSpeech("Você já está na primeira música.").build();
                }
            }
            return input.getResponseBuilder().withSpeech("Não há lista para voltar.").build();
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
            if (audioPlayer != null && audioPlayer.getToken() != null) {
                String token = audioPlayer.getToken();
                long offset = audioPlayer.getOffsetInMilliseconds() != null ? audioPlayer.getOffsetInMilliseconds() : 0L;
                
                if (token.contains(":")) {
                    String[] parts = token.split(":");
                    String seedVideoId = parts[0];
                    int index = Integer.parseInt(parts[1]);
                    
                    try {
                        AudioTrack track = youtubeMusicService.getSongFromPlaylist(seedVideoId, index);
                        return input.getResponseBuilder()
                                .addAudioPlayerPlayDirective(PlayBehavior.REPLACE_ALL, offset, null, token, track.getUrl())
                                .build();
                    } catch (Exception e) {
                        return input.getResponseBuilder().withSpeech("Ocorreu um erro ao retomar a música.").build();
                    }
                }
            }
            return input.getResponseBuilder().withSpeech("Não consegui encontrar a música pausada para retomar.").build();
        }
    }
}

