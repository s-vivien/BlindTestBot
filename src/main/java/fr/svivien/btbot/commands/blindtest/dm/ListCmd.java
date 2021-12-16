package fr.svivien.btbot.commands.blindtest.dm;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.BTDMCommand;

public class ListCmd extends BTDMCommand {

    public ListCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest);
        this.name = "list";
        this.help = "list the added songs";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = false;
    }

    @Override
    public void doCommand(CommandEvent commandEvent) {
        printEntryList(commandEvent);
    }

}
