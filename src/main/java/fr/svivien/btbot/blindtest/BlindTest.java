package fr.svivien.btbot.blindtest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fr.svivien.btbot.BotConfig;
import fr.svivien.btbot.audio.AudioHandler;
import fr.svivien.btbot.blindtest.model.Guessable;
import fr.svivien.btbot.blindtest.model.SongEntry;
import fr.svivien.btbot.blindtest.model.TrackMetadata;
import fr.svivien.btbot.blindtest.model.operation.EntryOperationResult;
import fr.svivien.btbot.blindtest.model.operation.ValueOperationResult;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.stream.IntStream;

public class BlindTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlindTest.class);
    public static final String ARTIST = "artist";
    public static final String TITLE = "title";
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String AUTOBACKUP_NAME = "AUTO";
    private static final String EMOJI = ":fire:";
    private static final String SUCCESS_REPLY_TEMPLATE2 = EMOJI + " %s `[%s]` found by %s ! (+%d) " + EMOJI;
    private static final int NOTFOUND_SCORE = -1;
    private static final OkHttpClient client = new OkHttpClient();

    // State
    private ConcurrentHashMap<String, LinkedHashSet<SongEntry>> entries = new ConcurrentHashMap<>();
    private Map<String, Integer> scores = new HashMap<>();
    private final Set<String> trackGuessers = new HashSet<>();
    private boolean locked = false;
    private TextChannel btChannel;

    private Integer songsPerPlayer;
    private final Integer maximumExtrasNumber;
    private final String backupPath;

    // Current song
    private final Set<String> skipRequests = new HashSet<>();
    private SongEntry currentSongEntry = null;
    private final boolean[] isGuessed;
    private int yetToGuess;

    public BlindTest(BotConfig cfg) {
        songsPerPlayer = cfg.getSongsPerPlayer();
        backupPath = cfg.getBackupPath();
        maximumExtrasNumber = cfg.getMaximumExtrasNumber();
        isGuessed = new boolean[2 + maximumExtrasNumber];
    }

    public void clearCurrentSong() {
        Arrays.fill(isGuessed, false);
        currentSongEntry = null;
        skipRequests.clear();
    }

    public String whatsLeftToFind() {
        if (yetToGuess == 0) return "Everything has been found";
        return "Left to find : " + IntStream.range(0, currentSongEntry.getGuessables().size())
                .filter(i -> !isGuessed[i])
                .mapToObj(i -> currentSongEntry.getGuessables().get(i).getName())
                .collect(Collectors.joining(", "));
    }

    public void onSkip(MessageReceivedEvent event) {
        if (skipRequests.add(event.getAuthor().getName())) {
            String msg = skipRequests.size() + " player(s) requested a *skip*.";
            var vc = event.getGuild().getAudioManager().getConnectedChannel();
            assert vc != null;
            int requiredSkipNumber = 1 + (vc.getMembers().size() - 1) / 3;
            boolean skip = false;
            if (skipRequests.size() >= requiredSkipNumber) {
                skip = true;
                msg += " **Skipping current song !**";
            } else {
                msg += " " + (requiredSkipNumber - skipRequests.size()) + " more needed.";
            }
            btChannel.sendMessage(msg).queue();

            if (skip) {
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                assert handler != null;
                handler.stopAndClear();
                event.getGuild().getAudioManager().closeAudioConnection();
                clearCurrentSong();
            }
        }
    }

    public void onProposition(MessageReceivedEvent event) {
        if (event.getChannel() != this.btChannel) return;
        if (currentSongEntry == null) return;
        if (yetToGuess == 0) return;

        String author = event.getAuthor().getName();
        if (author.equals(currentSongEntry.getOwner())) return;
        String proposition = event.getMessage().getContentRaw();

        if (event.getMessage().getContentDisplay().equals(":skip:")) {
            onSkip(event);
            return;
        }

        proposition = cleanLight(proposition);

        int i = -1;
        for (Guessable guessable : currentSongEntry.getGuessables()) {
            if (isGuessed[++i]) continue;
            var score = sorensenDiceScore(guessable.getValue(), proposition);
            if (score >= 0.8) {
                isGuessed[i] = true;
                yetToGuess--;
                int delta = 1 + (trackGuessers.contains(author) ? 1 : 0);
                addScore(author, delta);
                trackGuessers.add(author);
                btChannel.sendMessage(String.format(SUCCESS_REPLY_TEMPLATE2, guessable.getName(), guessable.getValue(), author, delta) + " " + whatsLeftToFind()).queue();
            }
        }
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
        trackGuessers.clear();
        currentSongEntry = entryList.get(0);
        yetToGuess = currentSongEntry.getGuessables().size();
        currentSongEntry.setDone(true);
        return true;
    }

    public int getEntriesSize() {
        return entries.values().stream().mapToInt(HashSet::size).sum();
    }

    public int getDoneEntriesSize() {
        return entries.values().stream().mapToInt(songEntries -> (int) songEntries.stream().filter(SongEntry::isDone).count()).sum();
    }

    public List<SongEntry> getAllEntries() {
        return entries.entrySet().stream().flatMap(e -> e.getValue().stream()).collect(Collectors.toList());
    }

    public void printSongPool() {
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
        btChannel.sendMessage(pool).queue();
    }

    public void printPoolPlaylists() {
        List<String> data = new ArrayList<>();
        List<SongEntry> doneEntries = getFlatEntries().stream().filter(SongEntry::isDone).collect(Collectors.toList());
        if (!doneEntries.isEmpty()) {
            List<String> ytIds = doneEntries.stream().map(SongEntry::getYtId).collect(Collectors.toList());
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
            for (String part : data) {
                btChannel.sendMessage(part).queue();
            }
        } else {
            btChannel.sendMessage("No song have been played so far :confused:").queue();
        }
    }

    public void printScoreBoard() {
        backupState(AUTOBACKUP_NAME, false);
        int doneEntrySize = getDoneEntriesSize();

        TreeMap<Integer, List<String>> scoreMap = new TreeMap<>(Collections.reverseOrder());
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            scoreMap.computeIfAbsent(e.getValue(), k -> new ArrayList<>());
            scoreMap.get(e.getValue()).add(e.getKey());
        }

        if (scoreMap.isEmpty()) {
            btChannel.sendMessage("No one scored yet ..").queue();
        } else {
            String scoreboard = "⏫ Scores (" + doneEntrySize + " track" + (doneEntrySize > 1 ? "s" : "") + " played out of " + getEntriesSize() + ") :";
            scoreboard += "```";
            for (Map.Entry<Integer, List<String>> e : scoreMap.entrySet()) {
                scoreboard += "\n" + String.format("%1$-11s", e.getKey() + " point" + ((e.getKey() > 1 || e.getKey() < -1) ? "s" : "")) + "   " + String.join(", ", e.getValue());
            }
            scoreboard += "```";

            btChannel.sendMessage(scoreboard).queue();
        }
    }

    public EntryOperationResult addSongRequest(String author, AudioTrack audioTrack, TrackMetadata trackMetadata, int startOffset) {
        LOGGER.info(author + " added a track to the pool");
        entries.computeIfAbsent(author, k -> new LinkedHashSet<>());
        scores.putIfAbsent(author, 0);
        if (entries.get(author).size() >= songsPerPlayer) return EntryOperationResult.FULL_LIST;
        SongEntry se = new SongEntry(audioTrack.getInfo().uri, author, cleanLight(trackMetadata.getArtist()), cleanTitle(trackMetadata.getTitle()),
                trackMetadata.getArtist() + " - " + trackMetadata.getTitle(), audioTrack.getInfo().identifier, startOffset);
        return entries.get(author).add(se) ? (se.isIncomplete() ? EntryOperationResult.SUCCESS_INCOMPLETE : EntryOperationResult.SUCCESS) : EntryOperationResult.ALREADY_ADDED;
    }

    public boolean removeSongRequest(String author, Integer index) {
        LOGGER.info(author + " removed a track from the pool");
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

    public ValueOperationResult setValue(String author, Integer index, String name, String value) {
        SongEntry e = getEntryByIndex(author, index);
        if (e == null) return ValueOperationResult.ENTRY_NOT_FOUND;

        Guessable g = e.getGuessable(name, 0);
        ValueOperationResult result;
        if (g == null) {
            if (e.getGuessables().size() >= 2 + maximumExtrasNumber) return ValueOperationResult.FULL;
            g = new Guessable(name, null);
            e.getGuessables().add(g);
            result = ValueOperationResult.ADDED;
        } else {
            result = ValueOperationResult.UPDATED;
        }

        if (name.equals(ARTIST)) {
            value = cleanLight(value);
        } else if (name.equals(TITLE)) {
            value = cleanTitle(value);
        } else {
            value = value.toLowerCase();
        }
        g.setValue(value);

        e.recomputeOriginalTitle();
        return result;
    }

    public ValueOperationResult unsetValue(String author, Integer index, String name) {
        SongEntry e = getEntryByIndex(author, index);
        if (e == null) return ValueOperationResult.ENTRY_NOT_FOUND;

        if (name.equals(ARTIST) || name.equals(TITLE)) return ValueOperationResult.FORBIDDEN;

        Guessable g = e.getGuessable(name, 2);
        if (g == null) return ValueOperationResult.VALUE_NOT_FOUND;

        e.getGuessables().remove(g);
        e.recomputeOriginalTitle();
        return ValueOperationResult.UPDATED;
    }

    public boolean setStartOffset(String author, Integer index, Integer offset) {
        SongEntry e = getEntryByIndex(author, index);
        if (e == null) return false;
        e.setStartOffset(offset);
        return true;
    }

    public void onTrackEnd() {
        String reply = "⏳ The track was **[ " + currentSongEntry.getCompleteOriginalTitle() + " ]**";
        if (yetToGuess == currentSongEntry.getGuessables().size()) {
            addScore(currentSongEntry.getOwner(), NOTFOUND_SCORE);
            reply += " and no one found it .. (" + NOTFOUND_SCORE + " for " + currentSongEntry.getOwner() + ")";
        }
        clearCurrentSong();
        btChannel.sendMessage(reply).queue();
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

    public String checkNick(String nick) {
        nick = nick.toLowerCase();
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getKey().toLowerCase().equals(nick)) return entry.getKey();
        }
        return null;
    }

    public int addScore(String nick, int delta) {
        Integer score = scores.get(nick);
        if (score == null) score = 0;
        score += delta;
        scores.put(nick, score);
        return score;
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
            Type entriesType = new TypeToken<ConcurrentHashMap<String, LinkedHashSet<SongEntry>>>() {
            }.getType();
            this.entries = GSON.fromJson(entriesJson, entriesType);
            Type scoresType = new TypeToken<HashMap<String, Integer>>() {
            }.getType();
            this.scores = GSON.fromJson(scoresJson, scoresType);
        } catch (IllegalStateException e) {
            return "No backup found with that name..";
        } catch (JsonParseException | IOException e) {
            return "Error while restoring the game state..";
        }
        return "Backup successfully restored !";
    }

    public void setBtChannel(TextChannel btChannel) {
        this.btChannel = btChannel;
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

    public String getLocalFilePath() {
        return backupPath + File.separator + "localFiles";
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
        entries.forEach((key, value) -> entryList.addAll(value));
        return entryList;
    }

    private String cleanLight(String input) {
        if (input == null) return null;
        return Normalizer.normalize(input.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[!?]+$", "")
                .replaceAll("^[!?]+", "")
                .replaceAll(" [!?]+", " ")
                .replaceAll("[!?]+ ", " ")
                .replaceAll("[¿¡*,.’':\\/-]", "")
                .replaceAll("œ", "oe")
                .replaceAll("[$]", "s")
                .replaceAll("[ø]", "o")
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

    private double sorensenDiceScore(String first, String second) {
        first = first.replace("\\s+", "");
        second = second.replace("\\s+", "");

        if (first.equals(second)) return 1;
        if (first.length() < 2 && second.length() >= 2) return 0;

        var firstBigrams = new HashMap<String, Integer>();
        var firstReverseBigrams = new HashMap<String, Integer>();
        var firstAltBigrams = new HashMap<String, Integer>();
        for (int i = 0; i < first.length() - 1; i++) {
            var bigram = String.valueOf(first.charAt(i)) + first.charAt(i + 1);
            var count = firstBigrams.getOrDefault(bigram, 0);
            firstBigrams.put(bigram, count + 1);

            var reverseBigram = String.valueOf(first.charAt(i + 1)) + first.charAt(i);
            var reverseCount = firstReverseBigrams.getOrDefault(reverseBigram, 0);
            firstReverseBigrams.put(reverseBigram, reverseCount + 1);

            if (i + 2 < first.length()) {
                var altBigram = String.valueOf(first.charAt(i)) + first.charAt(i + 2);
                var altCount = firstAltBigrams.getOrDefault(altBigram, 0);
                firstAltBigrams.put(altBigram, altCount + 1);
            }
        }

        double intersectionSize = 0;
        double altRatio = Math.max(0.2, 1.0 - 0.05 * first.length());

        for (int i = 0; i < second.length() - 1; i++) {
            var bigram = String.valueOf(second.charAt(i)) + second.charAt(i + 1);

            if (firstBigrams.get(bigram) != null) {
                var count = firstBigrams.get(bigram);
                if (count > 0) {
                    firstBigrams.put(bigram, count - 1);
                    intersectionSize++;
                    continue;
                }
            }
            if (firstReverseBigrams.get(bigram) != null) {
                var count = firstReverseBigrams.get(bigram);
                if (count > 0) {
                    firstReverseBigrams.put(bigram, count - 1);
                    intersectionSize += altRatio;
                    continue;
                }
            }
            if (firstAltBigrams.get(bigram) != null) {
                var count = firstAltBigrams.get(bigram);
                if (count > 0) {
                    firstAltBigrams.put(bigram, count - 1);
                    intersectionSize += altRatio;
                }
            }
        }

        return (2.0 * intersectionSize) / (first.length() + second.length() - 2);
    }
}
