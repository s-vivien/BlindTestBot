/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.svivien.btbot.commands.blindtest.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.audio.AudioHandler;
import fr.svivien.btbot.audio.QueuedTrack;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.commands.BTDJCommand;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

public class NextCmd extends BTDJCommand {

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
        if (handler.getPlayer().getPlayingTrack() != null) {
            commandEvent.reply("Previous song is still playing :upside_down:");
            return;
        }
        if (!blindTest.pickRandomNextSong()) {
            commandEvent.reply("No more songs to play :tired_face:");
            return;
        }

        bot.getPlayerManager().loadItem(blindTest.getCurrentSongEntry().getUrl(), new NextCmd.ResultHandler(commandEvent));
    }

    private class ResultHandler implements AudioLoadResultHandler {

        private CommandEvent event;
        private int tries = 0;

        public ResultHandler(CommandEvent event) {
            this.event = event;
        }

        private boolean queueTrack(AudioHandler handler, AudioTrack track) {
            if (tries++ < 10) {
                handler.addTrack(new QueuedTrack(track.makeClone(), event.getAuthor()));
                return true;
            }
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
                    if (!queueTrack(handler, audioTrack)) {
                        blindTest.onTrackEnd();
                    }
                } else {
                    // Add unknown players to the leaderboard
                    VoiceChannel vc = manager.getConnectedChannel();
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
