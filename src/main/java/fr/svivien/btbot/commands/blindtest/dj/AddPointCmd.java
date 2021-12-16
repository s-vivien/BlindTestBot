package fr.svivien.btbot.commands.blindtest.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.BTDJCommand;

public class AddPointCmd extends BTDJCommand {

    public AddPointCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest, false);
        this.name = "addpoint";
        this.arguments = "<points> <nick>";
        this.help = "adds points to some user's score";
        this.guildOnly = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        String[] spl = event.getArgs().split(" ", 2);

        try {
            int pts = Integer.parseInt(spl[0]);
            String nick = spl[1];
            String realNick = blindTest.checkNick(nick);
            if (realNick != null) {
                int score = blindTest.addScore(realNick, pts);
                event.reply(pts + " point" + ((pts > 1 || pts < -1) ? "s" : "") + " added to `" + realNick + "`, who now has " + score);
            } else {
                event.reply("Unknown player..");
            }
        } catch (Exception e) {
            event.reply("Invalid parameters, expected " + arguments);
        }
    }
}
