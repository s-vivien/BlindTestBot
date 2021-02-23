package com.jagrosh.jmusicbot.blindtest;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.jagrosh.jmusicbot.BotConfig;

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

    private Gson GSON = new Gson();
    public static final String DEFAULT = "N/A";
    private static final String EMOJI = ":fire:";
    private static final int SINGLE_SCORE = 1;
    private static final String SUCCESS_REPLY_TEMPLATE = EMOJI + " %s found %s `[%s]` ! (+%d) " + EMOJI;
    private static final int COMBO_SCORE = 3;
    private static final int NOTFOUND_SCORE = -1;
    private static final int MAX_DIST_RATIO = 6;
    private static final int MAX_DIST_OFFSET = 3;
    private static final double INCLUDE_TOLERANCE = 1.6;

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

        int combo = Math.min(calculateDistance(proposition, currentSongEntry.artist + " " + currentSongEntry.title), calculateDistance(proposition, currentSongEntry.title + " " + currentSongEntry.artist));
        if (combo <= maxDistCombo) {
            if (!artistFound && !trackFound) {
                artistFound = true;
                trackFound = true;
                addScore(author, COMBO_SCORE);
                return String.format(SUCCESS_REPLY_TEMPLATE, author, "the artist and the title", currentSongEntry.artist + "][" + currentSongEntry.title, COMBO_SCORE);
            } else if (!artistFound) {
                artistFound = true;
                addScore(author, SINGLE_SCORE);
                return String.format(SUCCESS_REPLY_TEMPLATE, author, "the artist", currentSongEntry.artist, SINGLE_SCORE);
            } else if (!trackFound) {
                trackFound = true;
                addScore(author, SINGLE_SCORE);
                return String.format(SUCCESS_REPLY_TEMPLATE, author, "the title", currentSongEntry.title, SINGLE_SCORE);
            }
        }

        if (!artistFound) {
            int artistAlone = calculateDistance(proposition, currentSongEntry.artist);
            if (artistAlone <= maxDistArtist || (proposition.contains(currentSongEntry.artist) && proposition.length() <= INCLUDE_TOLERANCE * currentSongEntry.artist.length())) {
                artistFound = true;
                addScore(author, SINGLE_SCORE);
                return String.format(SUCCESS_REPLY_TEMPLATE, author, "the artist", currentSongEntry.artist, SINGLE_SCORE);
            }
        }

        if (!trackFound) {
            int trackAlone = calculateDistance(proposition, currentSongEntry.title);
            if (trackAlone <= maxDistTitle || (proposition.contains(currentSongEntry.title) && proposition.length() <= INCLUDE_TOLERANCE * currentSongEntry.title.length())) {
                trackFound = true;
                addScore(author, SINGLE_SCORE);
                return String.format(SUCCESS_REPLY_TEMPLATE, author, "the title", currentSongEntry.title, SINGLE_SCORE);
            }
        }

        if (extrasYetToFind > 0) {
            for (int i = 0; i < currentSongEntry.extras.size(); i++) {
                if (extrasFound[i]) continue;
                String extra = currentSongEntry.extras.get(i);
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
        List<SongEntry> entryList = getFlatEntries();
        entryList = entryList.stream().filter(e -> !e.done).collect(Collectors.toList());
        if (entryList.isEmpty()) return false;
        Collections.shuffle(entryList);
        clearCurrentSong();
        currentSongEntry = entryList.get(0);
        extrasYetToFind = currentSongEntry.extras.size();
        currentSongEntry.done = true;
        maxDistCombo = Math.max(0, (currentSongEntry.artist.length() + currentSongEntry.title.length() + 1) - MAX_DIST_OFFSET) / MAX_DIST_RATIO;
        maxDistArtist = Math.max(0, currentSongEntry.artist.length() - MAX_DIST_OFFSET) / MAX_DIST_RATIO;
        maxDistTitle = Math.max(0, currentSongEntry.title.length() - MAX_DIST_OFFSET) / MAX_DIST_RATIO;
        for (int i = 0; i < currentSongEntry.extras.size(); i++) {
            maxDistExtras[i] = Math.max(0, currentSongEntry.extras.get(i).length() - MAX_DIST_OFFSET) / MAX_DIST_RATIO;
        }
        return true;
    }

    public String getSongPool() {
        String pool = "\uD83D\uDCBF Submission pool\n";

        int total = 0;

        if (!entries.isEmpty()) {
            pool += "```";
            for (Map.Entry<String, LinkedHashSet<SongEntry>> e : entries.entrySet()) {
                int empty = 0;
                for (SongEntry se : e.getValue()) if (se.title.equalsIgnoreCase(DEFAULT) || se.artist.equalsIgnoreCase(DEFAULT)) empty++;
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
        return entries.entrySet().stream().mapToInt(e->e.getValue().size()).sum();
    }

    public int getDoneEntriesSize() {
        return entries.entrySet().stream().mapToInt(e-> (int) e.getValue().stream().filter(s->s.done).count()).sum();
    }

    public String getScoreBoard() {
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

    public int addSongRequest(String author, String url, String artist, String title) {
        entries.computeIfAbsent(author, k -> new LinkedHashSet<>());
        scores.putIfAbsent(author, 0);
        if (entries.get(author).size() >= songsPerPlayer) return 2;
        SongEntry se = new SongEntry(url, author, cleanLight(artist.toLowerCase()), cleanTitle(title.toLowerCase()), artist + " - " + title);
        return entries.get(author).add(se) ? 0 : 1;
    }

    public int removeSongRequest(String author, Integer index) {
        LinkedHashSet<SongEntry> entrySet = entries.get(author);
        if (entrySet == null || entrySet.isEmpty() || entrySet.size() < index) return 1;
        Iterator<SongEntry> it = entrySet.iterator();
        int i = 1;
        while (it.hasNext()) {
            it.next();
            if (i == index) {
                it.remove();
                return 0;
            }
            i++;
        }
        return 1;
    }

    public boolean updateArtist(String author, Integer index, String artist) {
        SongEntry e = getEntryByIndex(author, index);
        if (e == null) return false;
        e.artist = cleanLight(artist.toLowerCase());
        e.recomputeOriginalTitle();
        return true;
    }

    public boolean updateTitle(String author, Integer index, String title) {
        SongEntry e = getEntryByIndex(author, index);
        if (e == null) return false;
        e.title = cleanTitle(title.toLowerCase());
        e.recomputeOriginalTitle();
        return true;
    }

    public boolean addExtra(String author, Integer index, String extra) {
        SongEntry e = getEntryByIndex(author, index);
        if (e == null) return false;
        if (e.extras.size() >= maximumExtrasNumber) return false;
        e.extras.add(extra.toLowerCase());
        e.recomputeOriginalTitle();
        return true;
    }

    public boolean removeExtra(String author, Integer songIndex, Integer extraIndex) {
        SongEntry e = getEntryByIndex(author, songIndex);
        if (e == null) return false;
        if (extraIndex > e.extras.size()) return false;
        e.extras.remove(extraIndex - 1);
        e.recomputeOriginalTitle();
        return true;
    }

    public boolean everyAnswerFound() {
        if (!trackFound || !artistFound) return false;
        for (int i = 0; i < currentSongEntry.extras.size(); i++) {
            if (!extrasFound[i]) return false;
        }
        return true;
    }

    public String onTrackEnd() {
        String reply = "⏳ The track was **[ " + currentSongEntry.completeOriginalTitle + " ]**";
        if (!trackFound && !artistFound && (currentSongEntry.extras.isEmpty() || currentSongEntry.extras.size() == extrasYetToFind)) {
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
            list += i + " : <" + e.url + "> artist=[" + e.artist + "] title=[" + e.title + "]";
            if (!e.extras.isEmpty()) {
                for (int j = 0; j < e.extras.size(); j++) list += " extra" + (j + 1) + "=[" + e.extras.get(j) + "]";
            }
            list += "\n";
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

    private void writeToFile(String path, String content) throws IOException {
        Path p = Paths.get(path);
        if (p.toFile().exists()) throw new IllegalStateException();
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

    public String backupState(String name) {
        String entriesJson = GSON.toJson(entries);
        String scoresJson = GSON.toJson(scores);
        try {
            writeToFile(computeBackFilePath(name + "_entries"), entriesJson);
            writeToFile(computeBackFilePath(name + "_scores"), scoresJson);
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
        return Normalizer.normalize(input.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[,\\!\\?\\:;\\.]", "")
                .trim();
    }

    private String cleanTitle(String title) {
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

    public static class SongEntry {
        String url;
        String owner;
        String artist;
        String title;
        List<String> extras = new ArrayList<>();
        String completeOriginalTitle;
        boolean done = false;

        public SongEntry(String url, String owner, String artist, String title, String completeOriginalTitle) {
            this.url = url;
            this.owner = owner;
            this.artist = artist;
            this.title = title;
            this.completeOriginalTitle = completeOriginalTitle;
        }

        @Override
        public int hashCode() {
            return (url + owner).hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return url.equals(((SongEntry) obj).url) && owner.equals(((SongEntry) obj).owner);
        }

        public String getUrl() {
            return url;
        }

        public String getOwner() {
            return owner;
        }

        public void recomputeOriginalTitle() {
            completeOriginalTitle = artist + " - " + title;
            for (String extra : extras) {
                completeOriginalTitle += " - " + extra;
            }
        }
    }
}
