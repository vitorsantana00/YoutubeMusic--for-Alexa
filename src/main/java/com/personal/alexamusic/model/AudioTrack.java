package com.personal.alexamusic.model;

public class AudioTrack {
    private String videoId;
    private String title;
    private String url;

    public AudioTrack(String videoId, String title, String url) {
        this.videoId = videoId;
        this.title = title;
        this.url = url;
    }

    public String getVideoId() { return videoId; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }

    @Override
    public String toString() {
        return "AudioTrack{videoId='" + videoId + "', title='" + title + "'}";
    }
}
