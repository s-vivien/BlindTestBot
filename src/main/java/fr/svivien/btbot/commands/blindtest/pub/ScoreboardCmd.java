package fr.svivien.btbot.commands.blindtest.pub;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.BTPublicCommand;

public class ScoreboardCmd extends BTPublicCommand {

    public ScoreboardCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest);
        this.name = "scores";
        this.help = "prints the scoreboard";
        this.guildOnly = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        blindTest.printScoreBoard();
    }
}
