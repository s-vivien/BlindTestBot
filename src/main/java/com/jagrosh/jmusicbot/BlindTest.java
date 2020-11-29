package com.jagrosh.jmusicbot;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlindTest {

    Map<String, LinkedHashSet<SongEntry>> entries = new HashMap<>();
    Map<String, Integer> scores = new HashMap<>();
    Integer songsPerPlayer;

    SongEntry currentSongEntry = null;

    public BlindTest(BotConfig cfg) {
        songsPerPlayer = cfg.getSongsPerPlayer();
    }

    public boolean pickRandomNextSong() {
        List<SongEntry> entryList = new ArrayList<>();
        entries.entrySet().forEach(e -> {
            for (SongEntry se : e.getValue()) {
                if (!se.done) entryList.add(se);
            }
        });
        if (entryList.isEmpty()) return false;
        Collections.shuffle(entryList);
        currentSongEntry = entryList.get(0);
        currentSongEntry.done = true;
        return true;
    }

    public int addSongRequest(String author, String url, String artist, String title) {
        if (entries.get(author) == null) entries.put(author, new LinkedHashSet<>());
        if (entries.get(author).size() >= songsPerPlayer) return 2;
        SongEntry se = new SongEntry(url, author, artist, cleanTitle(title));
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
