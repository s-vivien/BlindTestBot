package fr.svivien.btbot.commands;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.audio.AudioHandler;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.settings.Settings;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.exceptions.PermissionException;

public abstract class BTCommand extends CustomCommand {

    protected final Bot bot;
    protected boolean bePlaying;
    protected boolean beListening;
    protected final BlindTest blindTest;

    public BTCommand(Bot bot, BlindTest blindTest) {
        this.bot = bot;
        this.guildOnly = true;
        this.category = new Category("Blind-Test");
        this.blindTest = blindTest;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getMessage().isFromGuild()) {
            Settings settings = event.getClient().getSettingsFor(event.getGuild());
            var tchannel = settings.getTextChannel(event.getGuild());
            if (tchannel != null && !event.getTextChannel().equals(tchannel)) {
                try {
                    event.getMessage().delete().queue();
                } catch (PermissionException ignore) {}
                event.replyInDm(event.getClient().getError() + " You can only use that command in " + tchannel.getAsMention() + "!");
                return;
            }
            bot.getPlayerManager().setUpHandler(event.getGuild()); // no point constantly checking for this later
            if (bePlaying && !((AudioHandler) event.getGuild().getAudioManager().getSendingHandler()).isMusicPlaying(event.getJDA())) {
                event.reply(event.getClient().getError() + " There must be music playing to use that!");
                return;
            }
            if (beListening) {
                var current = event.getGuild().getSelfMember().getVoiceState().getChannel();
                GuildVoiceState userState = event.getMember().getVoiceState();
                if (!userState.inAudioChannel() || userState.isDeafened() || (current != null && !userState.getChannel().equals(current))) {
                    event.replyError("You must be listening in " + (current == null ? "a voice channel" : "**" + current.getName() + "**") + " to use that!");
                    return;
                }

                var afkChannel = userState.getGuild().getAfkChannel();
                if (afkChannel != null && afkChannel.equals(userState.getChannel())) {
                    event.replyError("You cannot use that command in an AFK channel!");
                    return;
                }

                if (!event.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
                    try {
                        event.getGuild().getAudioManager().openAudioConnection(userState.getChannel());
                    } catch (PermissionException ex) {
                        event.reply(event.getClient().getError() + " I am unable to connect to **" + userState.getChannel().getName() + "**!");
                        return;
                    }
                }
            }
        }

        doCommand(event);
    }

    public abstract void doCommand(CommandEvent event);
}
