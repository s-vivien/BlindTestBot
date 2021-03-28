package com.jagrosh.jmusicbot.blindtest;

import com.jagrosh.jdautilities.command.CommandListener;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.function.BiFunction;

public class PropositionListener implements CommandListener {

    private BiFunction<String, String, Void> onPropositionLambda;

    public void setOnPropositionLambda(BiFunction<String, String, Void> onPropositionLambda) {
        this.onPropositionLambda = onPropositionLambda;
    }

    @Override
    public void onNonCommandMessage(MessageReceivedEvent event) {
        if (event.getMessage().isFromGuild() && onPropositionLambda != null) {
            onPropositionLambda.apply(event.getAuthor().getName(), event.getMessage().getContentRaw());
        }
    }

}
