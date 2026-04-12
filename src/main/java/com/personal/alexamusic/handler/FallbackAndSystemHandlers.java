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
                    .or(Predicates.intentName("AMAZON.CancelIntent")));
        }

        @Override
        public Optional<Response> handle(HandlerInput input) {
            String speechText = "Parando a rádio. Até logo!";
            return input.getResponseBuilder()
                    .withSpeech(speechText)
                    .addAudioPlayerStopDirective()
                    .build();
        }
    }

    @Component
    public static class PauseIntentHandler implements RequestHandler {
        @Override
        public boolean canHandle(HandlerInput input) {
            return input.matches(Predicates.intentName("AMAZON.PauseIntent"));
        }

        @Override
        public Optional<Response> handle(HandlerInput input) {
            return input.getResponseBuilder()
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
            String speechText = "Você pode dizer: toque Queen. E eu irei tocar Queen seguido de músicas aleatórias relacionadas. Você também pode pular ou voltar músicas.";
            return input.getResponseBuilder()
                    .withSpeech(speechText)
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
            return input.getResponseBuilder().build();
        }
    }
}
