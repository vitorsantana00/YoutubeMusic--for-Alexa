package com.personal.alexamusic.controller;

import com.amazon.ask.Skill;
import com.amazon.ask.Skills;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.ResponseEnvelope;
import com.amazon.ask.util.JacksonSerializer;
import com.personal.alexamusic.handler.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AlexaController {

    private final Skill skill;
    private final JacksonSerializer serializer = new JacksonSerializer();

    @Autowired
    public AlexaController(
            LaunchRequestHandler launchRequestHandler,
            PlayIntentHandler playIntentHandler,
            ControlHandlers.NextIntentHandler nextIntentHandler,
            ControlHandlers.PreviousIntentHandler previousIntentHandler,
            ControlHandlers.ResumeIntentHandler resumeIntentHandler,
            FallbackAndSystemHandlers.CancelAndStopIntentHandler cancelAndStopIntentHandler,
            FallbackAndSystemHandlers.PauseIntentHandler pauseIntentHandler,
            FallbackAndSystemHandlers.HelpIntentHandler helpIntentHandler,
            FallbackAndSystemHandlers.SessionEndedRequestHandler sessionEndedRequestHandler,
            AudioPlayerHandlers.AudioPlayerEventHandler audioPlayerEventHandler) {

        this.skill = Skills.custom()
                .addRequestHandlers(
                        launchRequestHandler,
                        playIntentHandler,
                        nextIntentHandler,
                        previousIntentHandler,
                        resumeIntentHandler,
                        cancelAndStopIntentHandler,
                        pauseIntentHandler,
                        helpIntentHandler,
                        sessionEndedRequestHandler,
                        audioPlayerEventHandler
                )
                .build();
    }

    @PostMapping("/alexa")
    public ResponseEnvelope handleAlexaRequest(@RequestBody byte[] requestBytes) {
        try {
            RequestEnvelope requestEnvelope = serializer.deserialize(new String(requestBytes, "UTF-8"), RequestEnvelope.class);
            return skill.invoke(requestEnvelope);
        } catch (Exception e) {
            throw new RuntimeException("Error executing Alexa Request", e);
        }
    }
}
