package fr.svivien.btbot.commands.blindtest.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.BTDJCommand;

public class LockPoolCmd extends BTDJCommand {

    public LockPoolCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest, false);
        this.name = "lock";
        this.help = "lock/unlock the submissions";
        this.guildOnly = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        boolean lock = blindTest.swapLock();
        event.reply("Submissions are now **" + (lock ? "LOCKED :lock:" : "UNLOCKED :unlock:") + "**");
    }
}
