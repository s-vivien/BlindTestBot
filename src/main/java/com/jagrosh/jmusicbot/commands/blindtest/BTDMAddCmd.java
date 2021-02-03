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
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.blindtest.BlindTest;
import com.jagrosh.jmusicbot.commands.BTDMCommand;
import com.sapher.youtubedl.YoutubeDL;
import com.sapher.youtubedl.YoutubeDLRequest;
import com.sapher.youtubedl.YoutubeDLResponse;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class BTDMAddCmd extends BTDMCommand {

    private BlindTest blindTest;

    public BTDMAddCmd(Bot bot, BlindTest blindTest) {
        super(bot);
        this.blindTest = blindTest;
        this.name = "add";
        this.arguments = "<Youtube URL>";
        this.help = "adds song/playlist to the blindtest pool";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        if (blindTest.getLock()) commandEvent.reply("Il n'est plus possible de changer les propositions");
        else {
            String url = commandEvent.getMessage().getContentRaw().substring("!add ".length()).trim();
            if (url.startsWith("<") && url.endsWith(">")) url = url.substring(1, url.length() - 1);
            bot.getPlayerManager().loadItem(url, new BTDMAddCmd.ResultHandler(commandEvent));
        }
    }

    private class ResultHandler implements AudioLoadResultHandler {

        private CommandEvent event;
        private String author;

        public ResultHandler(CommandEvent event) {
            this.event = event;
            this.author = event.getMessage().getAuthor().getName();
        }

        private int addSingleTrack(AudioTrack audioTrack) {
            TrackInfo info = extractArtistAndTrack(audioTrack.getInfo().uri);
            int addResult = blindTest.addSongRequest(author, audioTrack.getInfo().uri, info != null ? info.artist : BlindTest.DEFAULT, info != null ? info.track : BlindTest.DEFAULT);
            String reply = "Ajout de **" + audioTrack.getInfo().title + "** ... ";
            if (addResult == 1) reply += ":no_entry_sign: Cette chanson avait déjà été ajoutée";
            else if (addResult == 2) reply += ":no_entry_sign: Il n'y a plus de place, nombre maximum de chansons atteint";
            else reply += ":white_check_mark: Chanson ajoutée avec succès" + (info == null ? " (:rotating_light: probable erreur dans le titre/artiste)" : "");
            event.reply(reply);
            return addResult;
        }

        private void finalReply() {
            List<String> lists = blindTest.getSongList(author);
            for (String list : lists) {
                event.reply(list);
            }
        }

        @Override
        public void trackLoaded(AudioTrack audioTrack) {
            addSingleTrack(audioTrack);
            finalReply();
        }

        @Override
        public void playlistLoaded(AudioPlaylist audioPlaylist) {
            for (AudioTrack audioTrack : audioPlaylist.getTracks()) {
                if (addSingleTrack(audioTrack) == 2) break;
            }
            finalReply();
        }

        @Override
        public void noMatches() {
            event.reply("Mauvais paramètre..");
        }

        @Override
        public void loadFailed(FriendlyException e) {
            event.reply("Le chargement de la vidéo/playlist a échoué..");
        }
    }

    private class TrackInfo {
        String artist;
        String track;

        public TrackInfo(String artist, String track) {
            this.artist = artist;
            this.track = track;
        }
    }

    private TrackInfo extractArtistAndTrack(String videoUrl) {
        try {
            // Build request
            YoutubeDLRequest request = new YoutubeDLRequest(videoUrl);
            request.setOption("simulate");
            request.setOption("dump-json");

            // Make request and return response
            YoutubeDLResponse response = YoutubeDL.execute(request);

            // Response
            String stdOut = response.getOut(); // Executable output

            JsonParser parser = new JsonParser();
            JsonElement parsedTree = parser.parse(stdOut);
            JsonObject root = parsedTree.getAsJsonObject();
            String track = root.get("track").getAsString();
            String artist = root.get("artist").getAsString();
            return new TrackInfo(artist, track);
        } catch (Exception ignored) {
        }

        return null;
    }

}
