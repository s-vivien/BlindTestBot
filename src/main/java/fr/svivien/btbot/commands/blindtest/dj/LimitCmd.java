package fr.svivien.btbot.commands.blindtest.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.BTDJCommand;

public class LimitCmd extends BTDJCommand {

    public LimitCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest, false);
        this.name = "limit";
        this.arguments = "<maximum number of songs per player>";
        this.help = "updates the song limit for players";
        this.guildOnly = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        try {
            Integer limit = Integer.valueOf(event.getArgs());
            blindTest.setSongsPerPlayer(limit);
            event.reply("Players are now limited to " + limit + " submissions" + (limit > 1 ? "s" : ""));
        } catch (Exception e) {
            event.reply("Invalid parameters, expected " + arguments);
        }
    }
}
