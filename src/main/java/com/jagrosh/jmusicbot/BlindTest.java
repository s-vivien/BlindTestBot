package com.jagrosh.jmusicbot;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BlindTest {

    static final int SINGLE_SCORE = 1;
    static final int COMBO_SCORE = 3;
    static final int NOTFOUND_SCORE = -1;
    static final int MAX_DIST = 2;

    Map<String, LinkedHashSet<SongEntry>> entries = new HashMap<>();
    Map<String, Integer> scores = new HashMap<>();
    Integer songsPerPlayer;

    // Current song
    SongEntry currentSongEntry = null;
    String trackFound, artistFound;

    public BlindTest(BotConfig cfg) {
        songsPerPlayer = cfg.getSongsPerPlayer();
    }

    private List<SongEntry> getFlatEntries() {
        List<SongEntry> entryList = new ArrayList<>();
        entries.entrySet().forEach(e -> entryList.addAll(e.getValue()));
        return entryList;
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
                return author + " a trouvé l'artiste et le titre ! (+" + COMBO_SCORE + ")";
            } else if (artistFound == null) {
                artistFound = author;
                addScore(author, SINGLE_SCORE);
                return author + " a trouvé l'artiste ! (+" + SINGLE_SCORE + ")";
            } else if (trackFound == null) {
                trackFound = author;
                addScore(author, SINGLE_SCORE);
                return author + " a trouvé le titre ! (+" + SINGLE_SCORE + ")";
            }
        }

        if (artistFound == null) {
            int artistAlone = calculate(proposition, currentSongEntry.artist);
            if (artistAlone <= MAX_DIST) {
                artistFound = author;
                addScore(author, SINGLE_SCORE);
                return author + " a trouvé l'artiste ! (+" + SINGLE_SCORE + ")";
            }
        }

        if (trackFound == null) {
            int trackAlone = calculate(proposition, currentSongEntry.title);
            if (trackAlone <= MAX_DIST) {
                trackFound = author;
                addScore(author, SINGLE_SCORE);
                return author + " a trouvé le titre ! (+" + SINGLE_SCORE + ")";
            }
        }

        return null;
    }

    public boolean pickRandomNextSong() {
        List<SongEntry> entryList = getFlatEntries();
        entryList = entryList.stream().filter(e -> !e.done).collect(Collectors.toList());
        if (entryList.isEmpty()) return false;
        Collections.shuffle(entryList);
        currentSongEntry = entryList.get(0);
        currentSongEntry.done = true;
        trackFound = null;
        artistFound = null;
        return true;
    }

    public String getSongPool() {
        String pool = "\uD83D\uDCBF Pool de chansons\n";

        int total = 0;

        for (Map.Entry<String, LinkedHashSet<SongEntry>> e : entries.entrySet()) {
            pool += e.getKey() + " : " + e.getValue().size() + "\n";
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
        SongEntry se = new SongEntry(url, author, artist.toLowerCase(), cleanTitle(title.toLowerCase()));
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

    public String onTrackEnd() {
        String reply = "⏳ La chanson était **[ " + currentSongEntry.artist + " - " + currentSongEntry.title + " ]**";
        if (trackFound == null && artistFound == null) {
            addScore(currentSongEntry.getOwner(), NOTFOUND_SCORE);
            return reply + " et personne ne l'a trouvée .. (" + NOTFOUND_SCORE + " pour " + currentSongEntry.getOwner() + ")";
        }
        currentSongEntry = null;
        return reply;
    }

    public String getSongList(String nick) {
        Set<SongEntry> entrySet = entries.get(nick);
        if (entrySet == null || entrySet.isEmpty()) return "Aucune chanson ajoutée pour l'instant";
        String list = "Liste des chansons ajoutées :\n";
        Iterator<SongEntry> it = entrySet.iterator();
        int i = 1;
        while (it.hasNext()) {
            SongEntry e = it.next();
            list += i + " : <" + e.url + "> [" + e.artist + "-" + e.title + "]\n";
            i++;
        }
        return list;
    }

    public void addScore(String nick, int score) {
        Integer previousScore = scores.get(nick);
        if (previousScore == null) previousScore = 0;
        previousScore += score;
        scores.put(nick, previousScore);
    }

    private String cleanTitle(String title) {
        Pattern pattern = Pattern.compile("(.+)(\\(.+\\))", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(title);
        boolean matchFound = matcher.find();
        return matchFound ? matcher.group(1).trim() : title.trim();
    }

    public SongEntry getCurrentSongEntry() {
        return currentSongEntry;
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

    public static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    public static int min(int... numbers) {
        return Arrays.stream(numbers)
                .min().orElse(Integer.MAX_VALUE);
    }

    public static class SongEntry {
        String url;
        String owner;
        String artist;
        String title;
        boolean done = false;

        public SongEntry(String url, String owner, String artist, String title) {
            this.url = url;
            this.owner = owner;
            this.artist = artist;
            this.title = title;
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
    }

}
