package com.jagrosh.jmusicbot.blindtest;

import com.jagrosh.jdautilities.command.CommandListener;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.function.BiFunction;

public class PropositionListener implements CommandListener {

    private final BlindTest blindTest;

    public PropositionListener(BlindTest blindTest) {
        this.blindTest = blindTest;
    }

    @Override
    public void onNonCommandMessage(MessageReceivedEvent event) {
        if (event.getMessage().isFromGuild()) {
            blindTest.onProposition(event);
        }
    }

}
