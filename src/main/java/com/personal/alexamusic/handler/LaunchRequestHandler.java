package com.personal.alexamusic.handler;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LaunchRequestHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.requestType(LaunchRequest.class));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        String speechText = "Bem-vindo ao Music Helper. Qual música você gostaria de ouvir?";
        String repromptText = "Diga algo como: toque Bohemian Rhapsody.";
        
        return input.getResponseBuilder()
                .withSpeech(speechText)
                .withReprompt(repromptText)
                .withSimpleCard("Music Helper", speechText)
                .build();
    }
}
