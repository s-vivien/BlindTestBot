package com.jagrosh.jmusicbot.listener;

import com.jagrosh.jdautilities.command.CommandListener;
import com.jagrosh.jmusicbot.BlindTest;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class PropositionListener implements CommandListener {

    private BlindTest blindTest;

    public PropositionListener(BlindTest blindTest) {
        this.blindTest = blindTest;
    }

    @Override
    public void onNonCommandMessage(MessageReceivedEvent event) {
//        if ()
    }

}
