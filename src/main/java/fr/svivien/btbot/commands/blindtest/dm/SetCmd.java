package fr.svivien.btbot.commands.blindtest.dm;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.blindtest.model.operation.ValueOperationResult;
import fr.svivien.btbot.commands.BTDMCommand;

public class SetCmd extends BTDMCommand {

    public SetCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest);
        this.name = "set";
        this.help = "updates/adds a value to guess";
        this.arguments = "<song index> <value name> <value>";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = false;
    }

    @Override
    public void doCommand(CommandEvent event) {
        String author = event.getMessage().getAuthor().getName();
        String[] spl = event.getArgs().split(" ", 3);

        int idx;
        String name, value;

        try {
            idx = Integer.parseInt(spl[0]);
            name = spl[1];
            value = spl[2];
        } catch (Exception e) {
            event.reply("Invalid parameters, expected " + arguments);
            return;
        }

        ValueOperationResult result = blindTest.setValue(author, idx, name, value);
        switch (result) {
            case ENTRY_NOT_FOUND:
                event.reply("Error : no submission with that index");
                break;
            case FULL:
                event.reply("Error : you cannot add another extra value");
                break;
            case ADDED:
                event.reply("Value `" + name + "` successfully added");
                printEntryList(event);
                break;
            case UPDATED:
                event.reply("Value `" + name + "` successfully updated");
                printEntryList(event);
                break;
        }
    }

}
