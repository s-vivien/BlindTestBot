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
package com.jagrosh.jmusicbot.commands.blindtest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.BlindTest;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.listener.PropositionListener;
import com.sapher.youtubedl.YoutubeDL;
import com.sapher.youtubedl.YoutubeDLRequest;
import com.sapher.youtubedl.YoutubeDLResponse;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class BTNextCmd extends MusicCommand {

    private BlindTest blindTest;
    private Bot bot;
    private PropositionListener propositionListener;

    public BTNextCmd(Bot bot, BlindTest blindTest, PropositionListener propositionListener) {
        super(bot);
        this.propositionListener = propositionListener;
        this.blindTest = blindTest;
        this.bot = bot;
        this.name = "next";
        this.help = "Pick a random next song for the blindtest";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = false;
        this.guildOnly = true;
    }

    //    @Override
    //    protected void execute(CommandEvent commandEvent) {
    //        if (!blindTest.pickRandomNextSong()) {
    //            commandEvent.reply("Toutes les chansons ont été jouées :'(");
    //            return;
    //        }
    //        bot.getPlayerManager().loadItem(blindTest.getCurrentSongEntry().getUrl(), new BTNextCmd.ResultHandler(commandEvent));
    //    }

    @Override
    public void doCommand(CommandEvent commandEvent) {
        if (!blindTest.pickRandomNextSong()) {
            commandEvent.reply("Toutes les chansons ont été jouées :'(");
            return;
        }
        bot.getPlayerManager().loadItem(blindTest.getCurrentSongEntry().getUrl(), new BTNextCmd.ResultHandler(commandEvent));
    }

    private class ResultHandler implements AudioLoadResultHandler {

        private CommandEvent event;

        public ResultHandler(CommandEvent event) {
            this.event = event;
        }

        @Override
        public void trackLoaded(AudioTrack audioTrack) {
            BlindTest.SongEntry songEntry = blindTest.getCurrentSongEntry();
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            handler.setOnTrackEndLambda(() -> event.reply(blindTest.onTrackEnd()));
            propositionListener.setOnPropositionLambda((author, prop) -> {
                if (author.equalsIgnoreCase(songEntry.getOwner())) return null;
                String reply = blindTest.onProposition(author, prop);
                if (reply != null) {
                    event.reply(reply);
                }
                return null;
            });
            int pos = handler.addTrack(new QueuedTrack(audioTrack, event.getAuthor())) + 1;
            event.reply("\uD83D\uDEA8 Chanson proposée par " + songEntry.getOwner() + " qui ne pourra pas jouer durant ce tour \uD83D\uDEA8");
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
