package fr.svivien.btbot.commands.blindtest.dm;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.BTDMCommand;

public class RemoveCmd extends BTDMCommand {

    public RemoveCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest);
        this.name = "remove";
        this.arguments = "<song index>";
        this.help = "removes song from the blindtest pool";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = false;
    }

    @Override
    public void doCommand(CommandEvent commandEvent) {
        if (blindTest.getLock()) commandEvent.reply("You can no longer change your submissions");
        else {
            String author = commandEvent.getMessage().getAuthor().getName();
            int idx;
            try {
                idx = Integer.parseInt(commandEvent.getArgs());
            } catch (Exception e) {
                commandEvent.reply("Badly formatted index");
                return;
            }
            if (!blindTest.removeSongRequest(author, idx)) commandEvent.reply("Error : could not find any submission with that index");
            else {
                commandEvent.reply("Song successfully removed");
                printEntryList(commandEvent);
            }
        }
    }

}
