package fr.svivien.btbot.blindtest;

import com.jagrosh.jdautilities.command.CommandListener;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

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
