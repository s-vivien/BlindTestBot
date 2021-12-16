package fr.svivien.btbot.commands.blindtest.pub;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.BTPublicCommand;

public class PlaylistCmd extends BTPublicCommand {

    public PlaylistCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest);
        this.name = "playlist";
        this.help = "generates a YT playlist of all the songs that have been played so far";
        this.guildOnly = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        blindTest.printPoolPlaylists();
    }
}
