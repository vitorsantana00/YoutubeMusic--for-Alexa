package com.personal.alexamusic.handler;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.interfaces.audioplayer.PlayBehavior;
import com.amazon.ask.request.Predicates;
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
            String songName = songSlot.getValue();
            speechText = "Buscando e tocando " + songName + ".";
            
            try {
                // Buscando a música de forma síncrona
                String streamUrl = youtubeMusicService.searchAndGetStreamUrl(songName);
                
                // O token pode ser qualquer identificador único longo e seguro
                String token = "TOKEN_" + System.currentTimeMillis();
                
                return input.getResponseBuilder()
                        .withSpeech(speechText)
                        // A diretiva chave que ordena ao dispositivo a reproduzir uma URL de áudio publicamente acessível
                        .addAudioPlayerPlayDirective(PlayBehavior.REPLACE_ALL, 0L, null, token, streamUrl)
                        .withSimpleCard("Music Helper", "Tocando: " + songName)
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
