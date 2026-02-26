package fr.svivien.btbot.commands.blindtest.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.audio.AudioHandler;
import fr.svivien.btbot.audio.QueuedTrack;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.BTDJCommand;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.managers.AudioManager;

public class NextCmd extends BTDJCommand {

    private static final int MAX_RETRIES = 10;

    public NextCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest, false);
        this.name = "next";
        this.help = "picks a random next song for the blindtest";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = false;
        this.guildOnly = true;
    }

    @Override
    public void doCommand(CommandEvent commandEvent) {
        AudioHandler handler = (AudioHandler) commandEvent.getGuild().getAudioManager().getSendingHandler();
        if (handler == null) return;
        if (handler.getPlayer().getPlayingTrack() != null) {
            commandEvent.reply("Previous song is still playing :upside_down:");
            return;
        }
        if (!blindTest.pickRandomNextSong()) {
            commandEvent.reply("No more songs to play :tired_face:");
            return;
        }

        // uncomment this shit if YT's APIs are broken again and you need to play from local files...
//        bot.getPlayerManager().loadItem(blindTest.getLocalFilePath() + File.separator + blindTest.getCurrentSongEntry().getYtId(), new NextCmd.ResultHandler(commandEvent));
        bot.getPlayerManager().loadItem(blindTest.getCurrentSongEntry().getUrl(), new NextCmd.ResultHandler(commandEvent));
    }

    private class ResultHandler implements AudioLoadResultHandler {

        private final CommandEvent event;
        private int tries = 0;

        public ResultHandler(CommandEvent event) {
            this.event = event;
        }

        private boolean queueTrack(AudioHandler handler, AudioTrack track) {
            if (tries++ < MAX_RETRIES) {
                handler.addTrack(new QueuedTrack(track.makeClone(), event.getAuthor()));
                return true;
            }
            event.reply(":tired_face: Failed to launch the track after " + MAX_RETRIES + " tries...");
            return false;
        }

        @Override
        public void trackLoaded(AudioTrack audioTrack) {
            AudioManager manager = event.getGuild().getAudioManager();
            AudioHandler handler = (AudioHandler) manager.getSendingHandler();
            if (handler == null) return;

            handler.setOnTrackStartLambda((AudioTrack t) -> {
                // Forwarding to the provided timestamp
                if (blindTest.getCurrentSongEntry().getStartOffset() > 0) {
                    t.setPosition(1000L * blindTest.getCurrentSongEntry().getStartOffset());
                }
            });
            handler.setOnTrackEndLambda((Long position) -> {
                if (position == 0) { // The track most likely crashed at loading, retrying
                    handler.stopAndClear();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                    if (!queueTrack(handler, audioTrack)) {
                        blindTest.onTrackEnd();
                    }
                } else {
                    // Add unknown players to the leaderboard
                    var vc = manager.getConnectedChannel();
                    if (vc != null) {
                        for (Member member : vc.getMembers()) {
                            if (!member.getUser().isBot()) blindTest.addScore(member.getUser().getName(), 0);
                        }
                    }

                    blindTest.onTrackEnd();
                }
            });
            queueTrack(handler, audioTrack);
            event.reply("\uD83D\uDEA8 Submission " + blindTest.getDoneEntriesSize() + "/" + blindTest.getEntriesSize() + " from **" + blindTest.getCurrentSongEntry().getOwner() + "** who cannot play during this round \uD83D\uDEA8 " +
                    blindTest.whatsLeftToFind());
        }

        @Override
        public void playlistLoaded(AudioPlaylist audioPlaylist) {
            event.reply("Error while loading next track");
        }

        @Override
        public void noMatches() {
            event.reply("Error while loading next track");
        }

        @Override
        public void loadFailed(FriendlyException e) {
            event.reply("Error while loading next track");
        }
    }

}
