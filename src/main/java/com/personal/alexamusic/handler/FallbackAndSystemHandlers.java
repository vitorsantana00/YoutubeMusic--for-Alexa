package com.personal.alexamusic.handler;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.SessionEndedRequest;
import com.amazon.ask.request.Predicates;
import org.springframework.stereotype.Component;

import java.util.Optional;

public class FallbackAndSystemHandlers {

    @Component
    public static class CancelAndStopIntentHandler implements RequestHandler {
        @Override
        public boolean canHandle(HandlerInput input) {
            return input.matches(Predicates.intentName("AMAZON.StopIntent")
                    .or(Predicates.intentName("AMAZON.CancelIntent"))
                    .or(Predicates.intentName("AMAZON.PauseIntent")));
        }

        @Override
        public Optional<Response> handle(HandlerInput input) {
            String speechText = "Parando a música e encerrando o Helper. Até logo!";
            return input.getResponseBuilder()
                    .withSpeech(speechText)
                    .withSimpleCard("Music Helper", speechText)
                    // Este atributo pára o audioplayer ativamente
                    .addAudioPlayerStopDirective()
                    .build();
        }
    }

    @Component
    public static class HelpIntentHandler implements RequestHandler {
        @Override
        public boolean canHandle(HandlerInput input) {
            return input.matches(Predicates.intentName("AMAZON.HelpIntent"));
        }

        @Override
        public Optional<Response> handle(HandlerInput input) {
            String speechText = "Você pode dizer a mim para tocar qualquer música existente, usando seu nome ou o nome do artista. Exemplo: toque Queen.";
            return input.getResponseBuilder()
                    .withSpeech(speechText)
                    .withSimpleCard("Music Helper", speechText)
                    .withReprompt(speechText)
                    .build();
        }
    }

    @Component
    public static class SessionEndedRequestHandler implements RequestHandler {
        @Override
        public boolean canHandle(HandlerInput input) {
            return input.matches(Predicates.requestType(SessionEndedRequest.class));
        }

        @Override
        public Optional<Response> handle(HandlerInput input) {
            // Apenas limpar recursos, a sessão acabou e não geramos saída falada.
            return input.getResponseBuilder().build();
        }
    }
}
