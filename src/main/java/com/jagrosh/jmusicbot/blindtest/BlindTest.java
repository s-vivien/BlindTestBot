package com.jagrosh.jmusicbot.blindtest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.jagrosh.jmusicbot.BotConfig;
import com.jagrosh.jmusicbot.blindtest.model.AddResult;
import com.jagrosh.jmusicbot.blindtest.model.SongEntry;
import com.jagrosh.jmusicbot.blindtest.model.TrackMetadata;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BlindTest {

    private Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String AUTOBACKUP_NAME = "AUTO";
    private static final String EMOJI = ":fire:";
    private static final int SINGLE_SCORE = 1;
    private static final String SUCCESS_REPLY_TEMPLATE = EMOJI + " %s found %s `[%s]` ! (+%d) " + EMOJI;
    private static final int COMBO_SCORE = 3;
    private static final int NOTFOUND_SCORE = -1;
    private static final int MAX_DIST_RATIO = 6;
    private static final int MAX_DIST_OFFSET = 3;
    private static final double INCLUDE_TOLERANCE = 1.6;
    private static final OkHttpClient client = new OkHttpClient();

    // State
    private ConcurrentHashMap<String, LinkedHashSet<SongEntry>> entries = new ConcurrentHashMap<>();
    private Map<String, Integer> scores = new HashMap<>();
    private boolean locked = false;

    private Integer songsPerPlayer;
    private Integer maximumExtrasNumber;
    private String backupPath;

    // Current song
    private SongEntry currentSongEntry = null;
    private boolean trackFound, artistFound;
    private boolean[] extrasFound;
    private int extrasYetToFind;
    private int maxDistCombo, maxDistArtist, maxDistTitle;
    private int[] maxDistExtras;

    public BlindTest(BotConfig cfg) {
        songsPerPlayer = cfg.getSongsPerPlayer();
        backupPath = cfg.getBackupPath();
        maximumExtrasNumber = cfg.getMaximumExtrasNumber();
        extrasFound = new boolean[maximumExtrasNumber];
        maxDistExtras = new int[maximumExtrasNumber];
    }

    public List<String> getPoolPlaylists() {
        List<String> data = new ArrayList<>();
        List<String> ytIds = getFlatEntries().stream().filter(SongEntry::isDone).map(SongEntry::getYtId).collect(Collectors.toList());
        if (!ytIds.isEmpty()) {
            data.add("Temporary Youtube playlist(s) of the " + ytIds.size() + " already played songs (:warning: will expire in a few hours) :");
            for (int i = 0; i < ytIds.size(); i += 50) {
                String longUrl = "http://www.youtube.com/watch_videos?video_ids=" + String.join(",", ytIds.subList(i, Math.min(i + 50, ytIds.size())));
                // Call YT to get the short playlist link
                Request request = new Request.Builder()
                        .url(longUrl)
                        .get()
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    data.add("<" + response.request().url().toString() + ">");
                } catch (IOException e) {
                    // If an error occurs, discard the whole list ..
                    data.clear();
                    data.add("Error while creating YT playlist :angry:");
                    break;
                }
            }
        }
        return data;
    }

    public void clearCurrentSong() {
        trackFound = false;
        artistFound = false;
        Arrays.fill(extrasFound, false);
        currentSongEntry = null;
    }

    public String whatsLeftToFind() {
        if (trackFound && artistFound && extrasYetToFind == 0) return "Everything has been found";
        String yetToFind = "Left to find :";
        int parts = 0;
        if (!trackFound) yetToFind += (parts++ > 0 ? "," : "") + " title";
        if (!artistFound) yetToFind += (parts++ > 0 ? "," : "") + " artist";
        if (extrasYetToFind > 0) yetToFind += (parts++ > 0 ? ", " : " ") + extrasYetToFind + " extra(s)";
        return yetToFind;
    }

    public String onProposition(String author, String proposition) {
        if (currentSongEntry == null) return null;
        if (artistFound && trackFound && extrasYetToFind == 0) return null;

        proposition = cleanLight(proposition);

        int combo = Math.min(calculateDistance(proposition, currentSongEntry.getArtist() + " " + currentSongEntry.getTitle()), calculateDistance(proposition, currentSongEntry.getTitle() + " " + currentSongEntry.getArtist()));
        if (combo <= maxDistCombo) {
            if (!artistFound && !trackFound) {
                artistFound = true;
                trackFound = true;
                addScore(author, COMBO_SCORE);
                return String.format(SUCCESS_REPLY_TEMPLATE, author, "the artist and the title", currentSongEntry.getArtist() + "][" + currentSongEntry.getTitle(), COMBO_SCORE);
            } else if (!artistFound) {
                artistFound = true;
                addScore(author, SINGLE_SCORE);
                return String.format(SUCCESS_REPLY_TEMPLATE, author, "the artist", currentSongEntry.getArtist(), SINGLE_SCORE);
            } else if (!trackFound) {
                trackFound = true;
                addScore(author, SINGLE_SCORE);
                return String.format(SUCCESS_REPLY_TEMPLATE, author, "the title", currentSongEntry.getTitle(), SINGLE_SCORE);
            }
        }

        if (!artistFound) {
            int artistAlone = calculateDistance(proposition, currentSongEntry.getArtist());
            if (artistAlone <= maxDistArtist || (proposition.contains(currentSongEntry.getArtist()) && proposition.length() <= INCLUDE_TOLERANCE * currentSongEntry.getArtist().length())) {
                artistFound = true;
                addScore(author, SINGLE_SCORE);
                return String.format(SUCCESS_REPLY_TEMPLATE, author, "the artist", currentSongEntry.getArtist(), SINGLE_SCORE);
            }
        }

        if (!trackFound) {
            int trackAlone = calculateDistance(proposition, currentSongEntry.getTitle());
            if (trackAlone <= maxDistTitle || (proposition.contains(currentSongEntry.getTitle()) && proposition.length() <= INCLUDE_TOLERANCE * currentSongEntry.getTitle().length())) {
                trackFound = true;
                addScore(author, SINGLE_SCORE);
                return String.format(SUCCESS_REPLY_TEMPLATE, author, "the title", currentSongEntry.getTitle(), SINGLE_SCORE);
            }
        }

        if (extrasYetToFind > 0) {
            for (int i = 0; i < currentSongEntry.getExtras().size(); i++) {
                if (extrasFound[i]) continue;
                String extra = currentSongEntry.getExtras().get(i);
                int extraAlone = calculateDistance(proposition, extra);
                if (extraAlone <= maxDistExtras[i] || (proposition.contains(extra) && proposition.length() <= INCLUDE_TOLERANCE * extra.length())) {
                    extrasFound[i] = true;
                    extrasYetToFind--;
                    addScore(author, SINGLE_SCORE);
                    return String.format(SUCCESS_REPLY_TEMPLATE, author, "one extra", extra, SINGLE_SCORE);
                }
            }
        }

        return null;
    }

    public void setSongsPerPlayer(Integer songsPerPlayer) {
        this.songsPerPlayer = songsPerPlayer;
    }

    public boolean pickRandomNextSong() {
        backupState(AUTOBACKUP_NAME, false);
        List<SongEntry> entryList = getFlatEntries();
        entryList = entryList.stream().filter(e -> !e.isDone()).collect(Collectors.toList());
        if (entryList.isEmpty()) return false;
        Collections.shuffle(entryList);
        clearCurrentSong();
        currentSongEntry = entryList.get(0);
        extrasYetToFind = currentSongEntry.getExtras().size();
        currentSongEntry.setDone(true);
        maxDistCombo = Math.max(0, (currentSongEntry.getArtist().length() + currentSongEntry.getTitle().length() + 1) - MAX_DIST_OFFSET) / MAX_DIST_RATIO;
        maxDistArtist = Math.max(0, currentSongEntry.getArtist().length() - MAX_DIST_OFFSET) / MAX_DIST_RATIO;
        maxDistTitle = Math.max(0, currentSongEntry.getTitle().length() - MAX_DIST_OFFSET) / MAX_DIST_RATIO;
        for (int i = 0; i < currentSongEntry.getExtras().size(); i++) {
            maxDistExtras[i] = Math.max(0, currentSongEntry.getExtras().get(i).length() - MAX_DIST_OFFSET) / MAX_DIST_RATIO;
        }
        return true;
    }

    public String getSongPool() {
        backupState(AUTOBACKUP_NAME, false);
        String pool = "\uD83D\uDCBF Submission pool\n";

        int total = 0;

        if (!entries.isEmpty()) {
            pool += "```";
            for (Map.Entry<String, LinkedHashSet<SongEntry>> e : entries.entrySet()) {
                int empty = 0;
                for (SongEntry se : e.getValue()) if (se.isIncomplete()) empty++;
                String warning = (empty > 0 ? " (" + empty + " incomplete submission" + (empty > 1 ? "s" : "") + ")" : "");
                pool += String.format("%1$-8s", e.getValue().size() + "/" + songsPerPlayer) + " " + e.getKey() + warning + "\n";
                total += e.getValue().size();
            }
            pool += "```";
        }
        pool += "**TOTAL** : " + total;
        return pool;
    }

    public int getEntriesSize() {
        return entries.entrySet().stream().mapToInt(e -> e.getValue().size()).sum();
    }

    public int getDoneEntriesSize() {
        return entries.entrySet().stream().mapToInt(e -> (int) e.getValue().stream().filter(s -> s.isDone()).count()).sum();
    }

    public String getScoreBoard() {
        backupState(AUTOBACKUP_NAME, false);
        int doneEntrySize = getDoneEntriesSize();

        TreeMap<Integer, List<String>> scoreMap = new TreeMap<>(Collections.reverseOrder());
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            scoreMap.computeIfAbsent(e.getValue(), k -> new ArrayList<>());
            scoreMap.get(e.getValue()).add(e.getKey());
        }

        String scoreboard = "⏫ Scores (" + doneEntrySize + " track" + (doneEntrySize > 1 ? "s" : "") + " played out of " + getEntriesSize() + ") :";
        scoreboard += "```";
        for (Map.Entry<Integer, List<String>> e : scoreMap.entrySet()) {
            scoreboard += "\n" + String.format("%1$-11s", e.getKey() + " point" + ((e.getKey() > 1 || e.getKey() < -1) ? "s" : "")) + "   " + String.join(", ", e.getValue());
        }
        scoreboard += "```";

        return scoreboard;
    }

    public AddResult addSongRequest(String author, AudioTrack audioTrack, TrackMetadata trackMetadata, int startOffset) {
        entries.computeIfAbsent(author, k -> new LinkedHashSet<>());
        scores.putIfAbsent(author, 0);
        if (entries.get(author).size() >= songsPerPlayer) return AddResult.FULL_LIST;
        SongEntry se = new SongEntry(audioTrack.getInfo().uri, author, cleanLight(trackMetadata.getArtist()), cleanTitle(trackMetadata.getTitle()),
                trackMetadata.getArtist() + " - " + trackMetadata.getTitle(), audioTrack.getInfo().identifier, startOffset);
        return entries.get(author).add(se) ? (se.isIncomplete() ? AddResult.SUCCESS_INCOMPLETE : AddResult.SUCCESS) : AddResult.ALREADY_ADDED;
    }

    public boolean removeSongRequest(String author, Integer index) {
        LinkedHashSet<SongEntry> entrySet = entries.get(author);
        if (entrySet == null || entrySet.isEmpty() || entrySet.size() < index) return false;
        Iterator<SongEntry> it = entrySet.iterator();
        int i = 1;
        while (it.hasNext()) {
            it.next();
            if (i == index) {
                it.remove();
                return true;
            }
            i++;
        }
        return false;
    }

    public boolean updateArtist(String author, Integer index, String artist) {
        SongEntry e = getEntryByIndex(author, index);
        if (e == null) return false;
        e.setArtist(cleanLight(artist));
        e.recomputeOriginalTitle();
        return true;
    }

    public boolean updateTitle(String author, Integer index, String title) {
        SongEntry e = getEntryByIndex(author, index);
        if (e == null) return false;
        e.setTitle(cleanTitle(title));
        e.recomputeOriginalTitle();
        return true;
    }

    public boolean addExtra(String author, Integer index, String extra) {
        SongEntry e = getEntryByIndex(author, index);
        if (e == null) return false;
        if (e.getExtras().size() >= maximumExtrasNumber) return false;
        e.getExtras().add(extra.toLowerCase());
        e.recomputeOriginalTitle();
        return true;
    }

    public boolean removeExtra(String author, Integer songIndex, Integer extraIndex) {
        SongEntry e = getEntryByIndex(author, songIndex);
        if (e == null) return false;
        if (extraIndex > e.getExtras().size()) return false;
        e.getExtras().remove(extraIndex - 1);
        e.recomputeOriginalTitle();
        return true;
    }

    public boolean setOffset(String author, Integer index, Integer offset) {
        SongEntry e = getEntryByIndex(author, index);
        if (e == null) return false;
        e.setStartOffset(offset);
        return true;
    }

    public boolean everyAnswerFound() {
        if (!trackFound || !artistFound) return false;
        for (int i = 0; i < currentSongEntry.getExtras().size(); i++) {
            if (!extrasFound[i]) return false;
        }
        return true;
    }

    public String onTrackEnd() {
        String reply = "⏳ The track was **[ " + currentSongEntry.getCompleteOriginalTitle() + " ]**";
        if (!trackFound && !artistFound && (currentSongEntry.getExtras().isEmpty() || currentSongEntry.getExtras().size() == extrasYetToFind)) {
            addScore(currentSongEntry.getOwner(), NOTFOUND_SCORE);
            return reply + " and no one found it .. (" + NOTFOUND_SCORE + " for " + currentSongEntry.getOwner() + ")";
        }
        clearCurrentSong();
        return reply;
    }

    public List<String> getSongList(String nick) {
        Set<SongEntry> entrySet = entries.get(nick);
        if (entrySet == null || entrySet.isEmpty()) return Collections.singletonList("No submissions added yet");
        List<String> lists = new ArrayList<>();
        String list = "Submissions (players will have to guess the values in square brackets, please check their correctness) :\n";
        Iterator<SongEntry> it = entrySet.iterator();
        int i = 1;
        while (it.hasNext()) {
            SongEntry e = it.next();
            list += i + " : " + e + "\n";
            i++;
            if (i % 30 == 0) {
                lists.add(list);
                list = "";
            }
        }
        if (entrySet.size() < songsPerPlayer) {
            int diff = songsPerPlayer - entrySet.size();
            list += diff + " submission" + (diff > 1 ? "s" : "") + " left";
        }
        if (!list.isEmpty()) lists.add(list);
        return lists;
    }

    public SongEntry getCurrentSongEntry() {
        return currentSongEntry;
    }

    public boolean getLock() {
        return locked;
    }

    public boolean swapLock() {
        locked = !locked;
        return locked;
    }

    public boolean isKnownNick(String nick) {
        return scores.get(nick) != null;
    }

    public int addScore(String nick, int score) {
        Integer previousScore = scores.get(nick);
        if (previousScore == null) previousScore = 0;
        previousScore += score;
        scores.put(nick, previousScore);
        return previousScore;
    }

    private void writeToFile(String path, String content, boolean checkIfExists) throws IOException {
        Path p = Paths.get(path);
        if (checkIfExists && p.toFile().exists()) throw new IllegalStateException();
        try (BufferedWriter writer = Files.newBufferedWriter(p)) {
            writer.write(content);
        }
    }

    private String readFile(String path) throws IOException {
        Path p = Paths.get(path);
        if (!p.toFile().exists()) throw new IllegalStateException();
        String content;
        try (BufferedReader reader = Files.newBufferedReader(p)) {
            String line;
            content = "";
            while ((line = reader.readLine()) != null) {
                content += line + System.lineSeparator();
            }
        }
        return content;
    }

    private String computeBackFilePath(String name) {
        return backupPath + File.separator + name + ".json";
    }

    public void reset() {
        clearCurrentSong();
        entries.clear();
        scores.clear();
    }

    public String backupState(String name, boolean checkIfExists) {
        String entriesJson = GSON.toJson(entries);
        String scoresJson = GSON.toJson(scores);
        try {
            writeToFile(computeBackFilePath(name + "_entries"), entriesJson, checkIfExists);
            writeToFile(computeBackFilePath(name + "_scores"), scoresJson, checkIfExists);
        } catch (IllegalStateException e) {
            return "A backup already exists with that name..";
        } catch (IOException e) {
            return "Error while backuping the game state..";
        }
        return "Backup successfully completed !";
    }

    public String restoreState(String name) {
        try {
            String entriesJson = readFile(computeBackFilePath(name + "_entries"));
            String scoresJson = readFile(computeBackFilePath(name + "_scores"));
            Type entriesType = new TypeToken<ConcurrentHashMap<String, LinkedHashSet<SongEntry>>>() {}.getType();
            this.entries = GSON.fromJson(entriesJson, entriesType);
            Type scoresType = new TypeToken<HashMap<String, Integer>>() {}.getType();
            this.scores = GSON.fromJson(scoresJson, scoresType);
        } catch (IllegalStateException e) {
            return "No backup found with that name..";
        } catch (JsonParseException | IOException e) {
            return "Error while restoring the game state..";
        }
        return "Backup successfully restored !";
    }

    private SongEntry getEntryByIndex(String author, Integer index) {
        LinkedHashSet<SongEntry> entrySet = entries.get(author);
        if (entrySet == null || entrySet.isEmpty() || entrySet.size() < index) return null;
        Iterator<SongEntry> it = entrySet.iterator();
        int i = 1;
        while (it.hasNext()) {
            SongEntry e = it.next();
            if (i == index) {
                return e;
            }
            i++;
        }
        return null;
    }

    private List<SongEntry> getFlatEntries() {
        List<SongEntry> entryList = new ArrayList<>();
        entries.entrySet().forEach(e -> entryList.addAll(e.getValue()));
        return entryList;
    }

    private String cleanLight(String input) {
        if (input == null) return null;
        return Normalizer.normalize(input.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[,\\!\\?\\:;\\.]", "")
                .trim();
    }

    private String cleanTitle(String title) {
        if (title == null) return null;
        title = cleanLight(title);
        Pattern pattern = Pattern.compile("(.+)(\\(.+\\))", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(title);
        boolean matchFound = matcher.find();
        return matchFound ? matcher.group(1).trim() : title.trim();
    }

    private int calculateDistance(String source, String target) {
        int sourceLength = source.length();
        int targetLength = target.length();
        if (sourceLength == 0) return targetLength;
        if (targetLength == 0) return sourceLength;
        int[][] dist = new int[sourceLength + 1][targetLength + 1];
        for (int i = 0; i < sourceLength + 1; i++) {
            dist[i][0] = i;
        }
        for (int j = 0; j < targetLength + 1; j++) {
            dist[0][j] = j;
        }
        for (int i = 1; i < sourceLength + 1; i++) {
            for (int j = 1; j < targetLength + 1; j++) {
                int cost = source.charAt(i - 1) == target.charAt(j - 1) ? 0 : 1;

                // special cases
                if (source.charAt(i - 1) == ' ' && (target.charAt(j - 1) == '-' || target.charAt(j - 1) == '\'')) cost = 0;

                dist[i][j] = Math.min(Math.min(dist[i - 1][j] + 1, dist[i][j - 1] + 1), dist[i - 1][j - 1] + cost);
                if (i > 1 &&
                    j > 1 &&
                    source.charAt(i - 1) == target.charAt(j - 2) &&
                    source.charAt(i - 2) == target.charAt(j - 1)) {
                    dist[i][j] = Math.min(dist[i][j], dist[i - 2][j - 2] + cost);
                }
            }
        }
        return dist[sourceLength][targetLength];
    }
}
