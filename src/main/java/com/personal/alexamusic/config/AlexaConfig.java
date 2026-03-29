package com.personal.alexamusic.config;

import com.amazon.ask.Skill;
import com.amazon.ask.Skills;
import com.amazon.ask.servlet.SkillServlet;
import com.personal.alexamusic.handler.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlexaConfig {

    @Autowired
    private LaunchRequestHandler launchRequestHandler;

    @Autowired
    private PlayIntentHandler playIntentHandler;

    @Autowired
    private FallbackAndSystemHandlers.CancelAndStopIntentHandler cancelAndStopIntentHandler;
    
    @Autowired
    private FallbackAndSystemHandlers.HelpIntentHandler helpIntentHandler;

    @Autowired
    private FallbackAndSystemHandlers.SessionEndedRequestHandler sessionEndedRequestHandler;

    @Autowired
    private AudioPlayerHandlers.AudioPlayerEventHandler audioPlayerEventHandler;

    @Bean
    public ServletRegistrationBean<SkillServlet> alexaServlet() {
        Skill skill = Skills.standard()
                .addRequestHandlers(
                        launchRequestHandler,
                        playIntentHandler,
                        cancelAndStopIntentHandler,
                        helpIntentHandler,
                        sessionEndedRequestHandler,
                        audioPlayerEventHandler
                )
                .build();

        SkillServlet skillServlet = new SkillServlet(skill);
        
        // Registra o servlet oficial da SDK no Spring Boot para o endpoint POST /alexa
        ServletRegistrationBean<SkillServlet> registrationBean = new ServletRegistrationBean<>(skillServlet, "/alexa");
        registrationBean.setName("AlexaServlet");
        return registrationBean;
    }
}
