package fr.svivien.btbot.blindtest.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SongEntry {

    public static final String DEFAULT = "N/A";

    private String url;
    private String owner;
    private List<Guessable> guessables;
    private String completeOriginalTitle;
    private String ytId;
    private int startOffset;
    private boolean done = false;

    public SongEntry(String url, String owner, String artist, String title, String completeOriginalTitle, String ytId, int startOffset) {
        this.url = url;
        this.owner = owner;
        this.guessables = new ArrayList<>();
        this.guessables.add(new Guessable("artist", artist != null ? artist : DEFAULT));
        this.guessables.add(new Guessable("title", title != null ? title : DEFAULT));
        this.completeOriginalTitle = completeOriginalTitle;
        this.ytId = ytId;
        this.startOffset = startOffset;
    }

    @Override
    public int hashCode() {
        return (url + owner).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return url.equals(((SongEntry) obj).url) && owner.equals(((SongEntry) obj).owner);
    }

    @Override
    public String toString() {
        return "<" + url + (startOffset > 0 ? "&t=" + startOffset : "") + "> "
                + guessables.stream().map(g -> g.getName() + "=[`" + g.getValue() + "`]")
                .collect(Collectors.joining(", "));
    }

    public Guessable getGuessable(String name, int skip) {
        return guessables.stream().skip(skip).filter(g -> g.getName().equals(name)).findFirst().orElse(null);
    }

    public boolean isIncomplete() {
        return guessables.get(0).getValue().equals(DEFAULT) || guessables.get(1).getValue().equals(DEFAULT);
    }

    public String getUrl() {
        return url;
    }

    public String getOwner() {
        return owner;
    }

    public void recomputeOriginalTitle() {
        completeOriginalTitle = guessables.stream().map(Guessable::getValue).collect(Collectors.joining(" - "));
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<Guessable> getGuessables() {
        return guessables;
    }

    public String getCompleteOriginalTitle() {
        return completeOriginalTitle;
    }

    public String getYtId() {
        return ytId;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
