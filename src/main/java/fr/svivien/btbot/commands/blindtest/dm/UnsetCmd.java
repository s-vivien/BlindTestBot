package fr.svivien.btbot.commands.blindtest.dm;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.blindtest.model.operation.ValueOperationResult;
import fr.svivien.btbot.commands.BTDMCommand;

public class UnsetCmd extends BTDMCommand {

    public UnsetCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest);
        this.name = "unset";
        this.help = "removes an extra value to guess";
        this.arguments = "<song index> <value name>";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = false;
    }

    @Override
    public void doCommand(CommandEvent event) {
        String author = event.getMessage().getAuthor().getName();
        String[] spl = event.getArgs().split(" ", 2);

        int songIdx;
        String name;

        try {
            songIdx = Integer.parseInt(spl[0]);
            name = spl[1];
        } catch (Exception e) {
            event.reply("Invalid parameters, expected " + arguments);
            return;
        }

        ValueOperationResult result = blindTest.unsetValue(author, songIdx, name);
        switch (result) {
            case ENTRY_NOT_FOUND:
                event.reply("Error : no submission with that index");
                break;
            case FORBIDDEN:
                event.reply("Error : you cannot unset that value");
                break;
            case VALUE_NOT_FOUND:
                event.reply("Value `" + name + "` not found");
                break;
            case UPDATED:
                event.reply("Value `" + name + "` successfully removed");
                printEntryList(event);
                break;
        }
    }

}
