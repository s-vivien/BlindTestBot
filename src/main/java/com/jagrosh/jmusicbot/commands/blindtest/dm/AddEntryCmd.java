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
package com.jagrosh.jmusicbot.commands.blindtest.dm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.blindtest.BlindTest;
import com.jagrosh.jmusicbot.blindtest.model.AddResult;
import com.jagrosh.jmusicbot.blindtest.model.TrackMetadata;
import com.jagrosh.jmusicbot.commands.BTDMCommand;
import com.sapher.youtubedl.YoutubeDL;
import com.sapher.youtubedl.YoutubeDLException;
import com.sapher.youtubedl.YoutubeDLRequest;
import com.sapher.youtubedl.YoutubeDLResponse;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddEntryCmd extends BTDMCommand {

    public AddEntryCmd(Bot bot, BlindTest blindTest) {
        super(bot, blindTest);
        this.name = "add";
        this.arguments = "<youtube video/playlist URL>";
        this.help = "adds song/playlist to the blindtest pool";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = false;
    }

    @Override
    public void doCommand(CommandEvent event) {
        if (blindTest.getLock()) event.reply("You can no longer change your submissions");
        else {
            String url = event.getMessage().getContentRaw().substring("!add ".length()).trim();
            if (url.startsWith("<") && url.endsWith(">")) url = url.substring(1, url.length() - 1);
            bot.getPlayerManager().loadItem(url, new AddEntryCmd.ResultHandler(event, url));
        }
    }

    private class ResultHandler implements AudioLoadResultHandler {

        private CommandEvent event;
        private String author;
        private String url;

        public ResultHandler(CommandEvent event, String url) {
            this.event = event;
            this.author = event.getMessage().getAuthor().getName();
            this.url = url;
        }

        private int getOffsetFromUrl() {
            Pattern pattern = Pattern.compile(".*t=(\\d+)s?", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(this.url);
            boolean matchFound = matcher.find();
            return matchFound ? Integer.parseInt(matcher.group(1)) : 0;
        }

        private AddResult addSingleTrack(AudioTrack audioTrack) {
            TrackMetadata info;
            AddResult addResult;
            try {
                info = extractArtistAndTrack(audioTrack.getInfo().uri);
                addResult = blindTest.addSongRequest(author, audioTrack, info, getOffsetFromUrl());
            } catch (Pegi18Exception e) {
                addResult = AddResult.PEGI18;
            }
            String reply = "Adding **" + audioTrack.getInfo().title + "** ... ";
            if (addResult == AddResult.ALREADY_ADDED) reply += ":no_entry_sign: This song has already been added";
            else if (addResult == AddResult.FULL_LIST) reply += ":no_entry_sign: Maximum number of songs has been reached";
            else if (addResult == AddResult.PEGI18) reply += ":no_entry_sign: This video is PEGI 18 and cannot be played";
            else reply += ":white_check_mark: Song successfully added" + (addResult == AddResult.SUCCESS_INCOMPLETE ? " (:rotating_light: there might be an error in artist and/or title)" : "");
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
                if (addSingleTrack(audioTrack) == AddResult.FULL_LIST) break;
            }
            finalReply();
        }

        @Override
        public void noMatches() {
            event.reply("Wrong parameter or invalid audio source..");
        }

        @Override
        public void loadFailed(FriendlyException e) {
            event.reply("The video/playlist loading failed..");
        }
    }

    private TrackMetadata extractArtistAndTrack(String videoUrl) throws Pegi18Exception {
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
            int ageLimit = root.get("age_limit").getAsInt();
            if (ageLimit >= 18) throw new Pegi18Exception();
            String track = root.get("track").getAsString();
            String artist = root.get("artist").getAsString();
            return new TrackMetadata(artist, track);
        } catch (RuntimeException | YoutubeDLException ignored) {
        }

        return new TrackMetadata();
    }

    static class Pegi18Exception extends Exception {}
}
