package fr.svivien.btbot.commands.blindtest.dm;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.BTDMCommand;

public class TimestampCmd extends BTDMCommand {

    public TimestampCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest);
        this.name = "timestamp";
        this.help = "sets the time at which a song should be started, in seconds";
        this.arguments = "<song index> <time>";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = false;
    }

    @Override
    public void doCommand(CommandEvent commandEvent) {
        String author = commandEvent.getMessage().getAuthor().getName();
        String[] spl = commandEvent.getArgs().split(" ", 2);

        int songIdx;
        int offset;

        try {
            songIdx = Integer.parseInt(spl[0]);
            offset = Integer.parseInt(spl[1]);
        } catch (Exception e) {
            commandEvent.reply("Invalid parameters, expected " + arguments);
            return;
        }

        boolean updateResult = blindTest.setStartOffset(author, songIdx, offset);
        if (!updateResult) commandEvent.reply("Error : could not find any submission with that index");
        else commandEvent.reply("Start time successfully updated");
        printEntryList(commandEvent);
    }

}
