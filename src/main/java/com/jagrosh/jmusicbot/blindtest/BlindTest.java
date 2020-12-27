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
    private static final String SUCCESS_REPLY_TEMPLATE = EMOJI + " %s a trouvé %s `[%s]` ! (+%d) " + EMOJI;
    private static final int COMBO_SCORE = 3;
    private static final int NOTFOUND_SCORE = -1;
    private static final int MAX_DIST_RATIO = 6;
    private static final int MAX_DIST_OFFSET = 3;

    // State
    private ConcurrentHashMap<String, LinkedHashSet<SongEntry>> entries = new ConcurrentHashMap<>();
    private Map<String, Integer> scores = new HashMap<>();
    private boolean locked = false;

    private Integer songsPerPlayer;
    private String backupPath;

    // Current song
    private SongEntry currentSongEntry = null;
    private String trackFound, artistFound;
    private int maxDistCombo, maxDistArtist, maxDistTitle;

    public BlindTest(BotConfig cfg) {
        songsPerPlayer = cfg.getSongsPerPlayer();
        backupPath = cfg.getBackupPath();
    }

    public void clearCurrentSong() {
        trackFound = null;
        artistFound = null;
        currentSongEntry = null;
    }

    public String onProposition(String author, String proposition) {
        if (currentSongEntry == null) return null;
        if (artistFound != null && trackFound != null) return null;

        proposition = proposition.toLowerCase();

        int combo = Math.min(calculateDistance(proposition, currentSongEntry.artist + " " + currentSongEntry.title), calculateDistance(proposition, currentSongEntry.title + " " + currentSongEntry.artist));
        if (combo <= maxDistCombo) {
            if (artistFound == null && trackFound == null) {
                artistFound = author;
                trackFound = author;
                addScore(author, COMBO_SCORE);
                return String.format(SUCCESS_REPLY_TEMPLATE, author, "l'artiste et le titre", currentSongEntry.artist + "][" + currentSongEntry.title, COMBO_SCORE);
            } else if (artistFound == null) {
                artistFound = author;
                addScore(author, SINGLE_SCORE);
                return String.format(SUCCESS_REPLY_TEMPLATE, author, "l'artiste", currentSongEntry.artist, SINGLE_SCORE);
            } else if (trackFound == null) {
                trackFound = author;
                addScore(author, SINGLE_SCORE);
                return String.format(SUCCESS_REPLY_TEMPLATE, author, "le titre", currentSongEntry.title, SINGLE_SCORE);
            }
        }

        if (artistFound == null) {
            int artistAlone = calculateDistance(proposition, currentSongEntry.artist);
            if (artistAlone <= maxDistArtist) {
                artistFound = author;
                addScore(author, SINGLE_SCORE);
                return String.format(SUCCESS_REPLY_TEMPLATE, author, "l'artiste", currentSongEntry.artist, SINGLE_SCORE);
            }
        }

        if (trackFound == null) {
            int trackAlone = calculateDistance(proposition, currentSongEntry.title);
            if (trackAlone <= maxDistTitle) {
                trackFound = author;
                addScore(author, SINGLE_SCORE);
                return String.format(SUCCESS_REPLY_TEMPLATE, author, "le titre", currentSongEntry.title, SINGLE_SCORE);
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
        currentSongEntry.done = true;
        maxDistCombo = Math.max(0, (currentSongEntry.artist.length() + currentSongEntry.title.length() + 1) - MAX_DIST_OFFSET) / MAX_DIST_RATIO;
        maxDistArtist = Math.max(0, currentSongEntry.artist.length() - MAX_DIST_OFFSET) / MAX_DIST_RATIO;
        maxDistTitle = Math.max(0, currentSongEntry.title.length() - MAX_DIST_OFFSET) / MAX_DIST_RATIO;
        return true;
    }

    public String getSongPool() {
        String pool = "\uD83D\uDCBF Pool de chansons\n";

        int total = 0;

        for (Map.Entry<String, LinkedHashSet<SongEntry>> e : entries.entrySet()) {
            int empty = 0;
            for (SongEntry se : e.getValue()) if (se.title.equalsIgnoreCase(DEFAULT) || se.artist.equalsIgnoreCase(DEFAULT)) empty++;
            String warning = (empty > 0 ? " (" + empty + " proposition" + (empty > 1 ? "s" : "") + " incomplète" + (empty > 1 ? "s" : "") + ")" : "");
            pool += e.getKey() + " : " + e.getValue().size() + "/" + songsPerPlayer + warning + "\n";
            total += e.getValue().size();
        }
        pool += "**TOTAL** : " + total;
        return pool;
    }

    public String getScoreBoard() {
        List<SongEntry> entryList = getFlatEntries();
        int totalEntrySize = entryList.size();
        entryList = entryList.stream().filter(e -> e.done).collect(Collectors.toList());
        int doneEntrySize = entryList.size();

        TreeMap<Integer, List<String>> scoreMap = new TreeMap<>(Collections.reverseOrder());
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            scoreMap.computeIfAbsent(e.getValue(), k -> new ArrayList<>());
            scoreMap.get(e.getValue()).add(e.getKey());
        }

        String scoreboard = "⏫ Scores (" + doneEntrySize + " chanson" + (doneEntrySize > 1 ? "s" : "") + " jouée" + (doneEntrySize > 1 ? "s" : "") + " sur " + totalEntrySize + ") :";
        for (Map.Entry<Integer, List<String>> e : scoreMap.entrySet()) {
            scoreboard += "\n" + e.getKey() + " point" + ((e.getKey() > 1 || e.getKey() < -1) ? "s" : "") + " : " + String.join(", ", e.getValue());
        }

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

    public int updateArtist(String author, Integer index, String artist) {
        SongEntry e = getEntryByIndex(author, index);
        if (e == null) return 1;
        e.artist = cleanLight(artist.toLowerCase());
        e.recomputeOriginalTitle();
        return 0;
    }

    public int updateTitle(String author, Integer index, String title) {
        SongEntry e = getEntryByIndex(author, index);
        if (e == null) return 1;
        e.title = cleanTitle(title.toLowerCase());
        e.recomputeOriginalTitle();
        return 0;
    }

    public boolean everyAnswerFound() {
        return currentSongEntry != null && trackFound != null && artistFound != null;
    }

    public String onTrackEnd() {
        String reply = "⏳ La chanson était **[ " + currentSongEntry.completeOriginalTitle + " ]**";
        if (trackFound == null && artistFound == null) {
            addScore(currentSongEntry.getOwner(), NOTFOUND_SCORE);
            return reply + " et personne ne l'a trouvée .. (" + NOTFOUND_SCORE + " pour " + currentSongEntry.getOwner() + ")";
        }
        clearCurrentSong();
        return reply;
    }

    public List<String> getSongList(String nick) {
        Set<SongEntry> entrySet = entries.get(nick);
        if (entrySet == null || entrySet.isEmpty()) return Collections.singletonList("Aucune chanson ajoutée pour l'instant");
        List<String> lists = new ArrayList<>();
        String list = "Liste des chansons ajoutées (les joueurs devront saisir les valeurs entre crochets pour marquer les points, pensez à vérifier qu'elles sont correctes) :\n";
        Iterator<SongEntry> it = entrySet.iterator();
        int i = 1;
        while (it.hasNext()) {
            SongEntry e = it.next();
            list += i + " : <" + e.url + "> [" + e.artist + "] [" + e.title + "]\n";
            i++;
            if (i % 30 == 0) {
                lists.add(list);
                list = "";
            }
        }
        if (entrySet.size() < songsPerPlayer) {
            int diff = songsPerPlayer - entrySet.size();
            list += "Il te manque encore " + diff + " chanson" + (diff > 1 ? "s" : "");
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
            return "Un backup portant le même nom existe déjà..";
        } catch (IOException e) {
            return "Erreur lors du backup..";
        }
        return "Backup réalisé avec succès !";
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
            return "Aucun fichier de backup portant ce nom n'a été trouvé..";
        } catch (JsonParseException | IOException e) {
            return "Erreur lors de la restauration du backup..";
        }
        return "Restauration réalisée avec succès !";
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

    private String cleanLight(String title) {
        return Normalizer.normalize(title.toLowerCase(), Normalizer.Form.NFD)
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
        }
    }
}
