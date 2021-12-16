package fr.svivien.btbot.commands.blindtest.pub;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.BTPublicCommand;

public class PoolCmd extends BTPublicCommand {

    public PoolCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest);
        this.name = "pool";
        this.help = "prints the song pool";
        this.guildOnly = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        blindTest.printSongPool();
    }
}
