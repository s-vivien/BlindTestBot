package com.jagrosh.jmusicbot.blindtest.model;

import java.util.ArrayList;
import java.util.List;

public class SongEntry {

    public static final String DEFAULT = "N/A";

    private String url;
    private String owner;
    private String artist;
    private String title;
    private List<String> extras = new ArrayList<>();
    private String completeOriginalTitle;
    private String ytId;
    private int startOffset = 0;
    private boolean done = false;

    public SongEntry(String url, String owner, String artist, String title, String completeOriginalTitle, String ytId, int startOffset) {
        this.url = url;
        this.owner = owner;
        this.artist = artist != null ? artist : DEFAULT;
        this.title = title != null ? title : DEFAULT;
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
        String str = "<" + url + "> artist=[" + artist + "] title=[" + title + "]";
        if (!extras.isEmpty()) {
            for (int j = 0; j < extras.size(); j++) str += " extra" + (j + 1) + "=[" + extras.get(j) + "]";
        }
        return str;
    }

    public boolean isIncomplete() {
        return artist.equals(DEFAULT) || title.equals(DEFAULT);
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

    public void setUrl(String url) {
        this.url = url;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getExtras() {
        return extras;
    }

    public void setExtras(List<String> extras) {
        this.extras = extras;
    }

    public String getCompleteOriginalTitle() {
        return completeOriginalTitle;
    }

    public void setCompleteOriginalTitle(String completeOriginalTitle) {
        this.completeOriginalTitle = completeOriginalTitle;
    }

    public String getYtId() {
        return ytId;
    }

    public void setYtId(String ytId) {
        this.ytId = ytId;
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
