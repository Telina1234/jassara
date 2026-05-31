package com.jassara.tononkira;

final class Song {
    final long id;
    final String title;
    final String artist;
    final String lyrics;
    final boolean favorite;

    Song(long id, String title, String artist, String lyrics, boolean favorite) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.lyrics = lyrics;
        this.favorite = favorite;
    }
}
