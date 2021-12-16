package fr.svivien.btbot.commands;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;

import java.util.List;

public abstract class BTDMCommand extends BTCommand {

    public BTDMCommand(Bot bot, BlindTest blindTest) {
        super(bot, blindTest);
        this.category = new Category("Blind-Test DM", event -> !event.getMessage().isFromGuild());
    }

    protected void printEntryList(CommandEvent event) {
        String author = event.getMessage().getAuthor().getName();
        List<String> lists = blindTest.getSongList(author);
        for (String list : lists) {
            event.reply(list);
        }
    }
}
