package com.jagrosh.jmusicbot.blindtest.model;

public class TrackMetadata {

    private String artist;
    private String title;

    public TrackMetadata(String artist, String title) {
        this.artist = artist;
        this.title = title;
    }

    public TrackMetadata() {
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public boolean isIncomplete() {
        return artist == null || title == null;
    }
}
