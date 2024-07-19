package fr.svivien.btbot.commands.blindtest.dm;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fr.svivien.btbot.Bot;
import fr.svivien.btbot.blindtest.BlindTest;
import fr.svivien.btbot.blindtest.model.TrackMetadata;
import fr.svivien.btbot.blindtest.model.operation.EntryOperationResult;
import fr.svivien.btbot.commands.BTDMCommand;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddCmd extends BTDMCommand {

    private final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public AddCmd(Bot bot, BlindTest blindTest) {
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
            bot.getPlayerManager().loadItem(url, new AddCmd.ResultHandler(event, url));
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

        private EntryOperationResult addSingleTrack(AudioTrack audioTrack) {
            TrackMetadata info;
            EntryOperationResult entryOperationResult;
            try {
                info = extractArtistAndTrack(audioTrack.getInfo().uri);
                entryOperationResult = blindTest.addSongRequest(author, audioTrack, info, getOffsetFromUrl());
            } catch (Pegi18Exception e) {
                entryOperationResult = EntryOperationResult.PEGI18;
            }
            String reply = "Adding **" + audioTrack.getInfo().title + "** ... ";
            if (entryOperationResult == EntryOperationResult.ALREADY_ADDED)
                reply += ":no_entry_sign: This song has already been added";
            else if (entryOperationResult == EntryOperationResult.FULL_LIST)
                reply += ":no_entry_sign: Maximum number of songs has been reached";
            else if (entryOperationResult == EntryOperationResult.PEGI18)
                reply += ":no_entry_sign: This video is PEGI 18 and cannot be played";
            else
                reply += ":white_check_mark: Song successfully added" + (entryOperationResult == EntryOperationResult.SUCCESS_INCOMPLETE ? " (:rotating_light: there might be an error in artist and/or title)" : "");
            event.reply(reply);
            return entryOperationResult;
        }

        @Override
        public void trackLoaded(AudioTrack audioTrack) {
            addSingleTrack(audioTrack);
            printEntryList(event);
        }

        @Override
        public void playlistLoaded(AudioPlaylist audioPlaylist) {
            for (AudioTrack audioTrack : audioPlaylist.getTracks()) {
                if (addSingleTrack(audioTrack) == EntryOperationResult.FULL_LIST) break;
            }
            printEntryList(event);
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
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(String.format("yt-dlp.exe %s --dump-json", videoUrl));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            YTResponse response = mapper.readValue(sb.toString(), YTResponse.class);
            if (response.age_limit >= 18) throw new Pegi18Exception();

            String track = null;
            String artist = null;

            String title = response.title;
            Pattern pattern = Pattern.compile("(.*) - ([^\\(\\[]*)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(title);
            boolean matchFound = matcher.find();
            if (matchFound) {
                artist = matcher.group(1).split(",")[0];
                track = matcher.group(2);
            }
            if (response.track != null) {
                track = response.track;
            }
            if (response.artist != null) {
                artist = response.artist.split(",")[0];
            }
            return new TrackMetadata(artist, track);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

        return new TrackMetadata();
    }

    private static class YTResponse {
        public String title;
        public int age_limit;
        public String track;
        public String artist;
    }

    static class Pegi18Exception extends Exception {
    }
}
