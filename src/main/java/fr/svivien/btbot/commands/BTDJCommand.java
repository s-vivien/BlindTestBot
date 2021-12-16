package fr.svivien.btbot.commands;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.settings.Settings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;

public abstract class BTDJCommand extends BTCommand {

    public BTDJCommand(Bot bot, BlindTest blindTest, boolean allowCurrentEntryAuthor) {
        super(bot, blindTest);
        this.category = new Category("DJ", event -> checkDJPermission(event) || (allowCurrentEntryAuthor && blindTest.getCurrentSongEntry() != null && event.getAuthor().getName().equals(blindTest.getCurrentSongEntry().getOwner())));
    }

    public static boolean checkDJPermission(CommandEvent event) {
        if (event.getAuthor().getId().equals(event.getClient().getOwnerId()))
            return true;
        if (event.getGuild() == null)
            return true;
        if (event.getMember().hasPermission(Permission.MANAGE_SERVER))
            return true;
        Settings settings = event.getClient().getSettingsFor(event.getGuild());
        Role dj = settings.getRole(event.getGuild());
        return dj != null && (event.getMember().getRoles().contains(dj) || dj.getIdLong() == event.getGuild().getIdLong());
    }
}
