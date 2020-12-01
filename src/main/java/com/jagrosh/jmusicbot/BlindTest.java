package com.jagrosh.jmusicbot;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BlindTest {

    private Gson GSON = new Gson();
    private static final String EMOJI = ":fire:";
    private static final int SINGLE_SCORE = 1;
    private static final int COMBO_SCORE = 3;
    private static final int NOTFOUND_SCORE = -1;
    private static final int MAX_DIST = 2;

    // State
    Map<String, LinkedHashSet<SongEntry>> entries = new HashMap<>();
    Map<String, Integer> scores = new HashMap<>();
    boolean locked = false;
    Integer songsPerPlayer;
    String backupPath;

    // Current song
    SongEntry currentSongEntry = null;
    String trackFound, artistFound;

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

        int combo = Math.min(calculate(proposition, currentSongEntry.artist + " " + currentSongEntry.title), calculate(proposition, currentSongEntry.title + " " + currentSongEntry.artist));
        if (combo <= MAX_DIST) {
            if (artistFound == null && trackFound == null) {
                artistFound = author;
                trackFound = author;
                addScore(author, COMBO_SCORE);
                return EMOJI + " " + author + " a trouvé l'artiste et le titre ! (+" + COMBO_SCORE + ") " + EMOJI;
            } else if (artistFound == null) {
                artistFound = author;
                addScore(author, SINGLE_SCORE);
                return EMOJI + " " + author + " a trouvé l'artiste ! (+" + SINGLE_SCORE + ") " + EMOJI;
            } else if (trackFound == null) {
                trackFound = author;
                addScore(author, SINGLE_SCORE);
                return EMOJI + " " + author + " a trouvé le titre ! (+" + SINGLE_SCORE + ") " + EMOJI;
            }
        }

        if (artistFound == null) {
            int artistAlone = calculate(proposition, currentSongEntry.artist);
            System.err.println("artistAlone " + artistAlone);
            if (artistAlone <= MAX_DIST) {
                artistFound = author;
                addScore(author, SINGLE_SCORE);
                return EMOJI + " " + author + " a trouvé l'artiste ! (+" + SINGLE_SCORE + ") " + EMOJI;
            }
        }

        if (trackFound == null) {
            int trackAlone = calculate(proposition, currentSongEntry.title);
            if (trackAlone <= MAX_DIST) {
                trackFound = author;
                addScore(author, SINGLE_SCORE);
                return EMOJI + " " + author + " a trouvé le titre ! (+" + SINGLE_SCORE + ") " + EMOJI;
            }
        }

        return null;
    }

    public boolean pickRandomNextSong() {
        List<SongEntry> entryList = getFlatEntries();
        entryList = entryList.stream().filter(e -> !e.done).collect(Collectors.toList());
        if (entryList.isEmpty()) return false;
        Collections.shuffle(entryList);
        clearCurrentSong();
        currentSongEntry = entryList.get(0);
        currentSongEntry.done = true;
        return true;
    }

    public String getSongPool() {
        String pool = "\uD83D\uDCBF Pool de chansons\n";

        int total = 0;

        for (Map.Entry<String, LinkedHashSet<SongEntry>> e : entries.entrySet()) {
            pool += e.getKey() + " : " + e.getValue().size() + "/" + songsPerPlayer + "\n";
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
        LinkedHashSet<SongEntry> entrySet = entries.get(author);
        if (entrySet == null || entrySet.isEmpty() || entrySet.size() < index) return 1;
        Iterator<SongEntry> it = entrySet.iterator();
        int i = 1;
        while (it.hasNext()) {
            SongEntry e = it.next();
            if (i == index) {
                e.artist = cleanLight(artist.toLowerCase());
                e.recomputeOriginalTitle();
                return 0;
            }
            i++;
        }
        return 1;
    }

    public int updateTitle(String author, Integer index, String title) {
        LinkedHashSet<SongEntry> entrySet = entries.get(author);
        if (entrySet == null || entrySet.isEmpty() || entrySet.size() < index) return 1;
        Iterator<SongEntry> it = entrySet.iterator();
        int i = 1;
        while (it.hasNext()) {
            SongEntry e = it.next();
            if (i == index) {
                e.title = cleanTitle(title.toLowerCase());
                e.recomputeOriginalTitle();
                return 0;
            }
            i++;
        }
        return 1;
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

    public String getSongList(String nick) {
        Set<SongEntry> entrySet = entries.get(nick);
        if (entrySet == null || entrySet.isEmpty()) return "Aucune chanson ajoutée pour l'instant";
        String list = "Liste des chansons ajoutées (les joueurs devront saisir les valeurs entre crochets pour marquer les points; pensez à vérifier qu'elles sont correctes) :\n";
        Iterator<SongEntry> it = entrySet.iterator();
        int i = 1;
        while (it.hasNext()) {
            SongEntry e = it.next();
            list += i + " : <" + e.url + "> [" + e.artist + "] [" + e.title + "]\n";
            i++;
        }
        if (entrySet.size() < songsPerPlayer) {
            int diff = songsPerPlayer - entrySet.size();
            list += "Il te manque encore " + diff + " chanson" + (diff > 1 ? "s" : "");
        }
        return list;
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

    private void addScore(String nick, int score) {
        Integer previousScore = scores.get(nick);
        if (previousScore == null) previousScore = 0;
        previousScore += score;
        scores.put(nick, previousScore);
    }

    private int writeToFile(String path, String content) {
        Path p = Paths.get(path);
        if (p.toFile().exists()) return 2;
        try (BufferedWriter writer = Files.newBufferedWriter(p)) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
        return 0;
    }

    private String readFile(String path) {
        Path p = Paths.get(path);
        String content = "";
        try (BufferedReader reader = Files.newBufferedReader(p)) {
            String line;
            while ((line = reader.readLine()) != null) {
                content += line + System.lineSeparator();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    private String computeBackFilePath(String name) {
        return backupPath + File.separator + name + ".json";
    }

    public String backupState(String name) {
        String entriesJson = GSON.toJson(entries);
        int er = writeToFile(computeBackFilePath("entries_" + name), entriesJson);
        if (er == 1) return "Erreur lors du backup..";
        else if (er == 2) return "Un backup portant le même nom existe déjà..";
        String scoresJson = GSON.toJson(scores);
        er = writeToFile(computeBackFilePath("scores_" + name), scoresJson);
        if (er == 1) return "Erreur lors du backup..";
        else if (er == 2) return "Un backup portant le même nom existe déjà..";
        return "Backup réalisé avec succès !";
    }

    public String restoreState(String name) {
        String entriesJson = readFile(computeBackFilePath("entries_" + name));
        if (entriesJson.isEmpty()) return "Erreur lors de la restauration du backup..";
        String scoresJson = readFile(computeBackFilePath("scores_" + name));
        if (scoresJson.isEmpty()) return "Erreur lors de la restauration du backup..";
        try {
            Type entriesType = new TypeToken<HashMap<String, LinkedHashSet<SongEntry>>>() {}.getType();
            this.entries = GSON.fromJson(entriesJson, entriesType);
            Type scoresType = new TypeToken<HashMap<String, Integer>>() {}.getType();
            this.scores = GSON.fromJson(scoresJson, scoresType);
        } catch (Exception e) {
            return "Erreur lors de la restauration du backup.."; // lol duplicated strings go brr brr
        }
        return "Restauration réalisée avec succès !";
    }

    private List<SongEntry> getFlatEntries() {
        List<SongEntry> entryList = new ArrayList<>();
        entries.entrySet().forEach(e -> entryList.addAll(e.getValue()));
        return entryList;
    }

    private String cleanLight(String title) {
        return title.replaceAll(",", "");
    }

    private String cleanTitle(String title) {
        title = cleanLight(title);
        Pattern pattern = Pattern.compile("(.+)(\\(.+\\))", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(title);
        boolean matchFound = matcher.find();
        return matchFound ? matcher.group(1).trim() : title.trim();
    }

    private int calculate(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = min(dp[i - 1][j - 1]
                                   + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1);
                }
            }
        }

        return dp[x.length()][y.length()];
    }

    private static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    private static int min(int... numbers) {
        return Arrays.stream(numbers)
                .min().orElse(Integer.MAX_VALUE);
    }

    public static class SongEntry {
        String url;
        String owner;
        String artist;
        String title;
        String completeOriginalTitle;
        boolean done = false;

        public SongEntry(String url, String owner) {
            this.url = url;
            this.owner = owner;
        }

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
