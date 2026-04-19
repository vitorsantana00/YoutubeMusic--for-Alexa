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
import java.util.logging.Logger;

@Component
public class PlayIntentHandler implements RequestHandler {

    private static final Logger LOGGER = Logger.getLogger(PlayIntentHandler.class.getName());
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

        if (songSlot == null || songSlot.getValue() == null) {
            return input.getResponseBuilder()
                    .withSpeech("Não entendi o nome da música. Por favor, tente novamente.")
                    .withReprompt("Diga: toque o nome de uma música ou artista.")
                    .build();
        }

        String songName = songSlot.getValue();
        LOGGER.info("PlayIntent recebido para: " + songName);

        try {
            // Busca a música no YouTube — o service já dispara o carregamento
            // do Radio Mix em background automaticamente
            AudioTrack track = youtubeMusicService.searchAndGetFirstSong(songName);

            // Token formato: seedVideoId:index
            // Index 1 = a música original que o usuário pediu
            // A partir do index 2, são as músicas do Radio Mix (relacionadas)
            String token = track.getVideoId() + ":1";
            YoutubeMusicService.setLastToken(token);

            LOGGER.info("Tocando: " + track.getTitle() + " (seed: " + track.getVideoId() + ")");

            return input.getResponseBuilder()
                    .withSpeech("Tocando " + track.getTitle())
                    .addAudioPlayerPlayDirective(PlayBehavior.REPLACE_ALL, 0L, null, token, track.getUrl())
                    .withSimpleCard("Music Helper", "Tocando: " + track.getTitle())
                    .build();

        } catch (Exception e) {
            LOGGER.severe("Erro ao buscar música '" + songName + "': " + e.getMessage());
            return input.getResponseBuilder()
                    .withSpeech("Desculpe, ocorreu um erro ao buscar " + songName + ". Tente novamente.")
                    .build();
        }
    }
}
