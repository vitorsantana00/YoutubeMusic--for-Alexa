package com.personal.alexamusic.handler;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.interfaces.audioplayer.PlayBehavior;
import com.amazon.ask.request.Predicates;
import com.personal.alexamusic.model.AudioTrack;
import com.personal.alexamusic.service.YoutubeMusicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class PlayIntentHandler implements RequestHandler {

    private final YoutubeMusicService youtubeMusicService;

    @Autowired
    public PlayIntentHandler(YoutubeMusicService youtubeMusicService) {
        this.youtubeMusicService = youtubeMusicService;
    }

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.intentName("PlayIntent"));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        IntentRequest intentRequest = (IntentRequest) input.getRequestEnvelope().getRequest();
        Map<String, Slot> slots = intentRequest.getIntent().getSlots();
        Slot songSlot = slots.get("SongName");

        String speechText;

        if (songSlot != null && songSlot.getValue() != null) {
            String songName = songSlot.getValue().toLowerCase();
            
            // Bypass para caso a música parede e ela tente mandar "tocar a proxima musica" via texto.
            if (songName.equals("proxima") || songName.equals("próxima") || songName.contains("proxima musica") || songName.contains("próxima música") || songName.equals("pular")) {
                String lastToken = YoutubeMusicService.getLastToken();
                if (lastToken != null && lastToken.contains(":")) {
                    try {
                        String[] parts = lastToken.split(":");
                        String seedVideoId = parts[0];
                        int index = Integer.parseInt(parts[1]);
                        int nextIndex = index + 1;
                        
                        AudioTrack nextTrack = youtubeMusicService.getSongFromPlaylist(seedVideoId, nextIndex);
                        String nextToken = seedVideoId + ":" + nextIndex;
                        YoutubeMusicService.setLastToken(nextToken);
                        
                        return input.getResponseBuilder()
                                .addAudioPlayerPlayDirective(PlayBehavior.REPLACE_ALL, 0L, null, nextToken, nextTrack.getUrl())
                                .build();
                    } catch (Exception e) {
                        return input.getResponseBuilder().withSpeech("Tive um problema ao tentar pular.").build();
                    }
                } else {
                    return input.getResponseBuilder().withSpeech("Não tenho registro da última música para pular. Por favor, peça uma música específica.").build();
                }
            }

            speechText = "Buscando e tocando " + songName + " na Rádio.";
            
            try {
                AudioTrack track = youtubeMusicService.searchAndGetFirstSong(songName);
                
                String token = track.getVideoId() + ":1";
                YoutubeMusicService.setLastToken(token); // Atualiza memória global!
                
                return input.getResponseBuilder()
                        .withSpeech("Tocando " + track.getTitle())
                        .addAudioPlayerPlayDirective(PlayBehavior.REPLACE_ALL, 0L, null, token, track.getUrl())
                        .withSimpleCard("Music Helper", "Tocando: " + track.getTitle())
                        .build();

            } catch (Exception e) {
                speechText = "Desculpe, ocorreu um erro ao tentar buscar a música.";
                return input.getResponseBuilder()
                        .withSpeech(speechText)
                        .build();
            }
        } else {
            speechText = "Não entendi o nome da música. Por favor, tente novamente.";
            return input.getResponseBuilder()
                    .withSpeech(speechText)
                    .withReprompt("Diga: toque o nome de uma música.")
                    .build();
        }
    }
}


