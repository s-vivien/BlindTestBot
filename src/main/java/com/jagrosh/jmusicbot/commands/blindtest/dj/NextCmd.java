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
package com.jagrosh.jmusicbot.commands.blindtest.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.blindtest.BlindTest;
import com.jagrosh.jmusicbot.blindtest.model.SongEntry;
import com.jagrosh.jmusicbot.commands.BTDJCommand;
import com.jagrosh.jmusicbot.listener.PropositionListener;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class NextCmd extends BTDJCommand {

    private PropositionListener propositionListener;

    public NextCmd(Bot bot, BlindTest blindTest, PropositionListener propositionListener) {
        super(bot, blindTest, false);
        this.propositionListener = propositionListener;
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

        public ResultHandler(CommandEvent event) {
            this.event = event;
        }

        @Override
        public void trackLoaded(AudioTrack audioTrack) {
            SongEntry songEntry = blindTest.getCurrentSongEntry();
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            handler.setOnTrackEndLambda(() -> {
                event.reply(blindTest.onTrackEnd());
                propositionListener.setOnPropositionLambda(null);
            });
            propositionListener.setOnPropositionLambda((author, prop) -> {
                if (author.equalsIgnoreCase(songEntry.getOwner())) return null;
                String reply = blindTest.onProposition(author, prop);
                if (reply != null) {
                    event.reply(reply + " " + blindTest.whatsLeftToFind());
                }
                return null;
            });
            handler.addTrack(new QueuedTrack(audioTrack, event.getAuthor()));
            if (blindTest.getCurrentSongEntry().getStartOffset() > 0) {
                AudioTrack playingTrack = handler.getPlayer().getPlayingTrack();
                playingTrack.setPosition(1000L * blindTest.getCurrentSongEntry().getStartOffset());
            }
            event.reply("\uD83D\uDEA8 Submission " + blindTest.getDoneEntriesSize() + "/" + blindTest.getEntriesSize() + " from **" + songEntry.getOwner() + "** who cannot play during this round \uD83D\uDEA8 " + blindTest.whatsLeftToFind());
        }

        @Override
        public void playlistLoaded(AudioPlaylist audioPlaylist) {
            event.reply("Error 1");
        }

        @Override
        public void noMatches() {
            event.reply("Error 2");
        }

        @Override
        public void loadFailed(FriendlyException e) {
            event.reply("Error 3");
        }
    }

}
