package fr.svivien.btbot.commands.blindtest.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.BTDJCommand;

public class ResetCmd extends BTDJCommand {

    public ResetCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest, false);
        this.name = "reset";
        this.help = "resets the whole state of the blind-test";
        this.guildOnly = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        blindTest.reset();
        event.reply(":recycle: Game successfully reset !");
    }
}
