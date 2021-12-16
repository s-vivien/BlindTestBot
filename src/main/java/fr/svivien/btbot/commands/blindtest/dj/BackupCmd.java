package fr.svivien.btbot.commands.blindtest.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.BTDJCommand;

public class BackupCmd extends BTDJCommand {

    public BackupCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest, false);
        this.name = "backup";
        this.arguments = "<backup name>";
        this.help = "backups the state of the game";
        this.guildOnly = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        event.reply(blindTest.backupState(event.getArgs(), true));
    }
}
