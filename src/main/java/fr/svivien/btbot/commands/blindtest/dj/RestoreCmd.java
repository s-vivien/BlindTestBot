package fr.svivien.btbot.commands.blindtest.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.BTDJCommand;

public class RestoreCmd extends BTDJCommand {

    public RestoreCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest, false);
        this.name = "restore";
        this.arguments = "<backup name>";
        this.help = "restores the state of the game from a named backup";
        this.guildOnly = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        event.reply(blindTest.restoreState(event.getArgs()));
    }
}
