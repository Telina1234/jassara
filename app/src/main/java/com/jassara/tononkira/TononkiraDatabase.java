package com.jassara.tononkira;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

final class TononkiraDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "tononkira.db";
    private static final int DB_VERSION = 1;

    TononkiraDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE songs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "artist TEXT NOT NULL," +
                "lyrics TEXT NOT NULL," +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE favorites (" +
                "song_id INTEGER PRIMARY KEY," +
                "created_at INTEGER NOT NULL," +
                "FOREIGN KEY(song_id) REFERENCES songs(id) ON DELETE CASCADE)");

        seed(db, "Hidera Anao", "Mahefa Nirina Rabemananjara",
                "Hidera Anao izahay, ry Tompo avo indrindra.\n" +
                        "Ny fo sy feo rehetra no asandratray am-pifaliana.\n\n" +
                        "Aoka ny hira hiakatra ho voninahitrao,\n" +
                        "ary ny fiainanay ho fanompoana Anao.");
        seed(db, "AmpiadaniKo", "Dadi Love",
                "AmpiadaniKo ny foko, omeo hazavana.\n" +
                        "Rehefa maizina ny lalana dia Ianao no fanantenana.");
        seed(db, "Manantena", "Sphynx",
                "Manantena Anao aho, tsy miova ny fitiavanao.\n" +
                        "Na sarotra aza ny andro, eo anilako Ianao.");
        seed(db, "Anao aho", "Antoko Mpihira Akon'ny Paradisa Zandriny",
                "Anao aho, Tompo ô, raiso ny fiainako.\n" +
                        "Ataovy fitaovana hitondra ny fiadananao.");
        seed(db, "Mila ho fefika", "KTLM Tsanta Fitoriana Ambohimena",
                "Mila ho fefika ny fanahiko, mila ny teninao.\n" +
                        "Tahio ny dianay, tariho amin'ny fahamarinana.");
        seed(db, "Mbola hotsaraina", "Vetson'ny Fanantenana Bemasoandro",
                "Mbola hotsaraina ny asa vita ety.\n" +
                        "Koa mifalia amin'ny tsara, mitandrema amin'ny fo.");
        seed(db, "Ramaria", "Tarika TARY",
                "Ramaria, hira entinay mampahery.\n" +
                        "Ny fanantenana tsy levona raha mitoetra ny finoana.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS favorites");
        db.execSQL("DROP TABLE IF EXISTS songs");
        onCreate(db);
    }

    long addSong(String title, String artist, String lyrics) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title.trim());
        values.put("artist", artist.trim());
        values.put("lyrics", lyrics.trim());
        values.put("created_at", System.currentTimeMillis());
        return db.insert("songs", null, values);
    }

    void updateSong(long id, String title, String artist, String lyrics) {
        ContentValues values = new ContentValues();
        values.put("title", title.trim());
        values.put("artist", artist.trim());
        values.put("lyrics", lyrics.trim());
        getWritableDatabase().update("songs", values, "id = ?", new String[]{String.valueOf(id)});
    }

    void deleteSong(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("favorites", "song_id = ?", new String[]{String.valueOf(id)});
        db.delete("songs", "id = ?", new String[]{String.valueOf(id)});
    }

    long upsertSong(String title, String artist, String lyrics) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id FROM songs WHERE title = ? COLLATE NOCASE AND artist = ? COLLATE NOCASE",
                new String[]{title.trim(), artist.trim()});
        try {
            if (cursor.moveToFirst()) {
                long id = cursor.getLong(0);
                updateSong(id, title, artist, lyrics);
                return id;
            }
        } finally {
            cursor.close();
        }
        return addSong(title, artist, lyrics);
    }

    List<Song> allSongs() {
        return querySongs("SELECT s.id, s.title, s.artist, s.lyrics, f.song_id IS NOT NULL AS favorite " +
                "FROM songs s LEFT JOIN favorites f ON f.song_id = s.id ORDER BY s.title COLLATE NOCASE", null);
    }

    List<Song> favorites() {
        return querySongs("SELECT s.id, s.title, s.artist, s.lyrics, 1 AS favorite " +
                "FROM songs s INNER JOIN favorites f ON f.song_id = s.id ORDER BY f.created_at DESC", null);
    }

    List<Song> search(String term) {
        String like = "%" + term.trim() + "%";
        return querySongs("SELECT s.id, s.title, s.artist, s.lyrics, f.song_id IS NOT NULL AS favorite " +
                "FROM songs s LEFT JOIN favorites f ON f.song_id = s.id " +
                "WHERE s.title LIKE ? OR s.artist LIKE ? OR s.lyrics LIKE ? " +
                "ORDER BY s.title COLLATE NOCASE", new String[]{like, like, like});
    }

    List<Song> songsByArtist(String artist) {
        return querySongs("SELECT s.id, s.title, s.artist, s.lyrics, f.song_id IS NOT NULL AS favorite " +
                "FROM songs s LEFT JOIN favorites f ON f.song_id = s.id " +
                "WHERE s.artist = ? ORDER BY s.title COLLATE NOCASE", new String[]{artist});
    }

    List<String> artists() {
        ArrayList<String> artists = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT artist, COUNT(*) AS total FROM songs GROUP BY artist ORDER BY artist COLLATE NOCASE", null);
        try {
            while (cursor.moveToNext()) {
                artists.add(cursor.getString(0) + " (" + cursor.getInt(1) + ")");
            }
        } finally {
            cursor.close();
        }
        return artists;
    }

    boolean isFavorite(long songId) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT song_id FROM favorites WHERE song_id = ?", new String[]{String.valueOf(songId)});
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    void setFavorite(long songId, boolean favorite) {
        SQLiteDatabase db = getWritableDatabase();
        if (favorite) {
            ContentValues values = new ContentValues();
            values.put("song_id", songId);
            values.put("created_at", System.currentTimeMillis());
            db.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } else {
            db.delete("favorites", "song_id = ?", new String[]{String.valueOf(songId)});
        }
    }

    private List<Song> querySongs(String sql, String[] args) {
        ArrayList<Song> songs = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(sql, args);
        try {
            while (cursor.moveToNext()) {
                songs.add(new Song(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getInt(4) == 1));
            }
        } finally {
            cursor.close();
        }
        return songs;
    }

    private void seed(SQLiteDatabase db, String title, String artist, String lyrics) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("artist", artist);
        values.put("lyrics", lyrics);
        values.put("created_at", System.currentTimeMillis());
        db.insert("songs", null, values);
    }
}
