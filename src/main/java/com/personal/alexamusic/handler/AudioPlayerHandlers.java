package com.personal.alexamusic.handler;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.audioplayer.PlaybackNearlyFinishedRequest;
import com.amazon.ask.model.interfaces.audioplayer.PlaybackStartedRequest;
import com.amazon.ask.model.interfaces.audioplayer.PlaybackStoppedRequest;
import com.amazon.ask.model.interfaces.audioplayer.PlaybackFinishedRequest;
import com.amazon.ask.model.interfaces.audioplayer.PlaybackFailedRequest;
import com.amazon.ask.request.Predicates;
import org.springframework.stereotype.Component;

import java.util.Optional;

public class AudioPlayerHandlers {

    @Component
    public static class AudioPlayerEventHandler implements RequestHandler {

        @Override
        public boolean canHandle(HandlerInput input) {
            return input.matches(Predicates.requestType(PlaybackStartedRequest.class)
                    .or(Predicates.requestType(PlaybackStoppedRequest.class))
                    .or(Predicates.requestType(PlaybackNearlyFinishedRequest.class))
                    // Tratando eventuais falhas do audioplayer ou finalização
                    .or(Predicates.requestType(PlaybackFinishedRequest.class))
                    .or(Predicates.requestType(PlaybackFailedRequest.class)));
        }

        @Override
        public Optional<Response> handle(HandlerInput input) {
            // A Alexa exige que sua Skill atenda as solicitações do AudioPlayer
            // sem nenhuma resposta falada e indicando o sucesso do consumo do evento.
            // Para isso apenas retornamos um build vazio, o que satisfaz a Alexa.
            return input.getResponseBuilder().build();
        }
    }
}
