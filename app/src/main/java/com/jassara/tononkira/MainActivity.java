package com.jassara.tononkira;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQUEST_EXPORT = 9001;
    private static final int REQUEST_IMPORT = 9002;
    private static final int NAVY = Color.rgb(2, 11, 36);
    private static final int NAVY_SOFT = Color.rgb(7, 24, 65);
    private static final int BLUE = Color.rgb(29, 109, 255);
    private static final int CYAN = Color.rgb(84, 217, 255);
    private static final int TEXT = Color.rgb(31, 41, 55);
    private static final int MUTED = Color.rgb(99, 116, 139);
    private static final int SURFACE = Color.rgb(247, 250, 255);

    private TononkiraDatabase database;
    private SharedPreferences preferences;
    private FrameLayout root;
    private FrameLayout content;
    private FrameLayout drawerLayer;
    private TextView toolbarTitle;
    private boolean adminUnlocked;
    private Long editingSongId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = new TononkiraDatabase(this);
        preferences = getSharedPreferences("tononkira_settings", MODE_PRIVATE);

        Window window = getWindow();
        window.setStatusBarColor(NAVY);
        window.setNavigationBarColor(NAVY);

        root = new FrameLayout(this);
        root.setBackgroundColor(darkMode() ? Color.rgb(7, 18, 38) : SURFACE);
        ImageView watermark = new ImageView(this);
        watermark.setImageResource(getResources().getIdentifier("victory_voice_logo", "drawable", getPackageName()));
        watermark.setAlpha(darkMode() ? 0.08f : 0.11f);
        watermark.setScaleType(ImageView.ScaleType.CENTER_CROP);
        watermark.setPadding(0, dp(70), 0, 0);
        root.addView(watermark, match());

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(Color.TRANSPARENT);
        root.addView(main, match());

        main.addView(createToolbar(), new LinearLayout.LayoutParams(matchWidth(), dp(76)));
        content = new FrameLayout(this);
        main.addView(content, new LinearLayout.LayoutParams(matchWidth(), 0, 1));

        drawerLayer = createDrawer();
        root.addView(drawerLayer, match());
        setContentView(root);

        showHome();
    }

    @Override
    public void onBackPressed() {
        if (toolbarTitle != null && !tr("app").contentEquals(toolbarTitle.getText())) {
            showHome();
            return;
        }
        super.onBackPressed();
    }

    private View createToolbar() {
        FrameLayout toolbar = new FrameLayout(this);
        toolbar.setBackgroundColor(NAVY);
        toolbar.addView(new WaveToolbarView(this), match());

        TextView menu = toolbarButton("☰", 30);
        FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(dp(64), matchHeight());
        menuParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        toolbar.addView(menu, menuParams);
        menu.setOnClickListener(v -> openDrawer());

        toolbarTitle = label(tr("app"), 24, Color.WHITE, Typeface.NORMAL);
        toolbarTitle.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(matchWidth(), matchHeight());
        titleParams.leftMargin = dp(72);
        titleParams.rightMargin = dp(132);
        toolbar.addView(toolbarTitle, titleParams);

        TextView search = toolbarButton("⌕", 28);
        FrameLayout.LayoutParams searchParams = new FrameLayout.LayoutParams(dp(58), matchHeight());
        searchParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        searchParams.rightMargin = dp(58);
        toolbar.addView(search, searchParams);
        search.setOnClickListener(v -> showSearch(""));

        TextView theme = toolbarButton(darkMode() ? "☼" : "◐", 24);
        FrameLayout.LayoutParams themeParams = new FrameLayout.LayoutParams(dp(58), matchHeight());
        themeParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        toolbar.addView(theme, themeParams);
        theme.setOnClickListener(v -> {
            preferences.edit().putBoolean("dark_mode", !darkMode()).apply();
            recreate();
        });
        return toolbar;
    }

    private FrameLayout createDrawer() {
        FrameLayout layer = new FrameLayout(this);
        layer.setVisibility(View.GONE);

        View dim = new View(this);
        dim.setBackgroundColor(Color.argb(148, 0, 0, 0));
        dim.setOnClickListener(v -> closeDrawer());
        layer.addView(dim, match());

        LinearLayout drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setBackgroundColor(cardFill());
        drawer.setPadding(0, 0, 0, dp(12));
        GradientDrawable drawerBg = new GradientDrawable();
        drawerBg.setColor(cardFill());
        drawerBg.setCornerRadii(new float[]{0, 0, dp(28), dp(28), dp(28), dp(28), 0, 0});
        drawer.setBackground(drawerBg);
        drawer.setElevation(dp(10));

        FrameLayout header = new FrameLayout(this);
        header.addView(new WaveHeaderView(this), match());
        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);
        headerText.setGravity(Gravity.CENTER_VERTICAL);
        headerText.setPadding(dp(28), dp(12), dp(18), dp(12));
        headerText.addView(label(tr("app"), 34, Color.WHITE, Typeface.BOLD));
        TextView verse = label("'Ezaho ny fiantsonao, fa aza atao malemy, Asandrato ny feonao ho tahaka ny an'ny anjomara.'\nIsaia 58:1", 15, Color.rgb(217, 236, 255), Typeface.NORMAL);
        try {
            verse.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/BRADHITC.TTF"));
        } catch (RuntimeException ignored) {
            verse.setTypeface(Typeface.create(Typeface.SERIF, Typeface.ITALIC));
        }
        verse.setGravity(Gravity.END);
        verse.setLineSpacing(dp(2), 1f);
        verse.setPadding(0, dp(8), dp(8), 0);
        headerText.addView(verse, new LinearLayout.LayoutParams(matchWidth(), wrap()));
        header.addView(headerText, match());
        drawer.addView(header, new LinearLayout.LayoutParams(matchWidth(), dp(168)));
        Space navTop = new Space(this);
        drawer.addView(navTop, new LinearLayout.LayoutParams(matchWidth(), dp(8)));

        addDrawerItem(drawer, "⌂", tr("home"), () -> showHome());
        addDrawerItem(drawer, "⌕", tr("search"), () -> showSearch(""));
        addDrawerItem(drawer, "♪", tr("artists"), () -> showArtists());
        addDrawerItem(drawer, "★", tr("favorites"), () -> showFavorites());
        addDrawerItem(drawer, "＋", tr("admin"), () -> showAdmin());
        addDrawerItem(drawer, "⚙", tr("settings"), () -> showSettings());
        addDrawerItem(drawer, "⇅", tr("import_export"), () -> showImportExport());
        addDrawerItem(drawer, "i", tr("about"), () -> showAbout());

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(314), matchHeight());
        params.gravity = Gravity.START;
        layer.addView(drawer, params);
        return layer;
    }

    private void addDrawerItem(LinearLayout drawer, String icon, String title, Runnable action) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(14), 0, dp(14), 0);
        item.setClickable(true);
        GradientDrawable itemBg = new GradientDrawable();
        itemBg.setColor(Color.TRANSPARENT);
        itemBg.setCornerRadius(dp(18));
        item.setBackground(itemBg);

        TextView iconView = label(icon, 28, BLUE, Typeface.BOLD);
        iconView.setGravity(Gravity.CENTER);
        iconView.setIncludeFontPadding(false);
        iconView.setBackground(circleBg(Color.argb(darkMode() ? 48 : 26, 29, 109, 255)));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        item.addView(iconView, iconParams);

        TextView titleView = label(title, 18, primaryText(), Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        titleView.setIncludeFontPadding(false);
        titleView.setPadding(dp(14), 0, 0, 0);
        item.addView(titleView, new LinearLayout.LayoutParams(0, matchHeight(), 1));

        item.setOnClickListener(v -> {
            closeDrawer();
            action.run();
        });
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(matchWidth(), dp(64));
        itemParams.setMargins(dp(14), dp(3), dp(14), dp(3));
        drawer.addView(item, itemParams);
    }

    private void showHome() {
        setTitleText(tr("app"));
        LinearLayout body = screen();
        body.addView(sectionTitle(tr("song_list")));
        addSongCards(body, database.allSongs(), false);
        render(body);
    }

    private void showSearch(String initialTerm) {
        setTitleText(tr("search"));
        LinearLayout body = screen();

        EditText search = input(tr("search_hint"));
        search.setSingleLine(true);
        search.setText(initialTerm);
        body.addView(search, spaced(matchWidth(), dp(56), 0, 0, 0, dp(14)));

        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        body.addView(results, new LinearLayout.LayoutParams(matchWidth(), wrap()));

        Runnable refresh = () -> {
            results.removeAllViews();
            String term = search.getText().toString();
            List<Song> songs = term.trim().isEmpty() ? database.allSongs() : database.search(term);
            addSongCards(results, songs, true);
        };
        search.addTextChangedListener(new SimpleWatcher(refresh));
        refresh.run();
        render(body);
    }

    private void showArtists() {
        setTitleText(tr("artists"));
        LinearLayout body = screen();
        body.addView(sectionTitle(tr("all_artists")));
        List<String> artists = database.artists();
        if (artists.isEmpty()) {
            body.addView(empty(tr("no_artist")));
        } else {
            for (String artistLabel : artists) {
                String artist = artistLabel.replaceFirst(" \\([0-9]+\\)$", "");
                body.addView(actionCard(artistLabel, tr("view_songs"), () -> showArtistSongs(artist)));
            }
        }
        render(body);
    }

    private void showArtistSongs(String artist) {
        setTitleText(artist);
        LinearLayout body = screen();
        body.addView(sectionTitle(tr("songs_by") + " " + artist));
        addSongCards(body, database.songsByArtist(artist), false);
        render(body);
    }

    private void showFavorites() {
        setTitleText(tr("favorites"));
        LinearLayout body = screen();
        body.addView(sectionTitle(tr("favorite_titles")));
        List<Song> songs = database.favorites();
        if (songs.isEmpty()) {
            body.addView(empty(tr("no_favorite")));
        } else {
            addSongCards(body, songs, false);
        }
        render(body);
    }

    private void showSong(Song song) {
        setTitleText(tr("lyrics"));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(8), dp(16), dp(10));

        TextView title = label(song.title, 30, primaryText(), Typeface.BOLD);
        title.setPadding(0, 0, 0, 0);
        body.addView(title);
        TextView artist = label(song.artist, 18, BLUE, Typeface.BOLD);
        artist.setPadding(0, dp(2), 0, dp(8));
        body.addView(artist);

        final boolean[] favorite = {database.isFavorite(song.id)};
        Button favButton = smallButton(favorite[0] ? tr("remove_fav") : tr("add_fav"), null, true);
        favButton.setOnClickListener(v -> {
            favorite[0] = !favorite[0];
            database.setFavorite(song.id, favorite[0]);
            favButton.setText(favorite[0] ? tr("remove_fav") : tr("add_fav"));
        });
        LinearLayout detailActions = new LinearLayout(this);
        detailActions.setOrientation(LinearLayout.HORIZONTAL);
        detailActions.setGravity(Gravity.CENTER_VERTICAL);
        detailActions.addView(favButton, new LinearLayout.LayoutParams(wrap(), dp(40)));
        Space alignSpace = new Space(this);
        detailActions.addView(alignSpace, new LinearLayout.LayoutParams(0, dp(40), 1));
        Button left = translucentButton("≡", null);
        Button center = translucentButton("≡", null);
        Button right = translucentButton("≡", null);
        detailActions.addView(left, new LinearLayout.LayoutParams(dp(38), dp(38)));
        detailActions.addView(center, new LinearLayout.LayoutParams(dp(38), dp(38)));
        detailActions.addView(right, new LinearLayout.LayoutParams(dp(38), dp(38)));
        body.addView(detailActions, spaced(matchWidth(), dp(42), 0, 0, 0, dp(8)));

        int savedSize = preferences.getInt("lyrics_size", 19);
        TextView lyrics = label(song.lyrics, savedSize, primaryText(), Typeface.NORMAL);
        String align = preferences.getString("lyrics_align", "center");
        lyrics.setGravity(gravityForAlign(align));
        lyrics.setLineSpacing(dp(4), 1.08f);
        lyrics.setPadding(dp(20), dp(20), dp(20), dp(20));
        ScrollView lyricsScroll = new ScrollView(this);
        lyricsScroll.setFillViewport(true);
        lyricsScroll.setBackgroundColor(Color.TRANSPARENT);
        lyricsScroll.addView(lyrics, new ScrollView.LayoutParams(matchWidth(), wrap()));
        body.addView(lyricsScroll, new LinearLayout.LayoutParams(matchWidth(), 0, 1));

        LinearLayout zoom = new LinearLayout(this);
        zoom.setOrientation(LinearLayout.HORIZONTAL);
        zoom.setGravity(Gravity.CENTER_VERTICAL);
        Button minus = translucentButton("-", null);
        Button plus = translucentButton("+", null);
        Space gap = new Space(this);
        zoom.addView(minus, new LinearLayout.LayoutParams(dp(36), dp(36)));
        zoom.addView(gap, new LinearLayout.LayoutParams(0, dp(42), 1));
        zoom.addView(plus, new LinearLayout.LayoutParams(dp(36), dp(36)));
        final int[] size = {savedSize};
        minus.setOnClickListener(v -> {
            size[0] = Math.max(14, size[0] - 2);
            lyrics.setTextSize(size[0]);
            preferences.edit().putInt("lyrics_size", size[0]).apply();
        });
        plus.setOnClickListener(v -> {
            size[0] = Math.min(34, size[0] + 2);
            lyrics.setTextSize(size[0]);
            preferences.edit().putInt("lyrics_size", size[0]).apply();
        });
        left.setOnClickListener(v -> {
            preferences.edit().putString("lyrics_align", "left").apply();
            lyrics.setGravity(Gravity.START);
        });
        center.setOnClickListener(v -> {
            preferences.edit().putString("lyrics_align", "center").apply();
            lyrics.setGravity(Gravity.CENTER_HORIZONTAL);
        });
        right.setOnClickListener(v -> {
            preferences.edit().putString("lyrics_align", "right").apply();
            lyrics.setGravity(Gravity.END);
        });
        body.addView(zoom, spaced(matchWidth(), dp(42), 0, dp(8), 0, 0));
        content.removeAllViews();
        content.addView(body, match());
    }

    private void showAdmin() {
        setTitleText(tr("admin"));
        if (!adminUnlocked) {
            showAdminLogin();
            return;
        }

        LinearLayout body = screen();
        Song editing = findSong(editingSongId);
        EditText title = input(tr("song_title"));
        EditText artist = input(tr("artist_name"));
        EditText lyrics = input(tr("lyrics"));
        if (editing != null) {
            title.setText(editing.title);
            artist.setText(editing.artist);
            lyrics.setText(editing.lyrics);
        }
        lyrics.setMinLines(6);
        lyrics.setGravity(Gravity.TOP | Gravity.START);
        lyrics.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        body.addView(title, spaced(matchWidth(), dp(56), 0, dp(14), 0, 0));
        body.addView(artist, spaced(matchWidth(), dp(56), 0, dp(14), 0, 0));
        body.addView(lyrics, spaced(matchWidth(), dp(320), 0, dp(14), 0, 0));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        Button save = smallButton(editing == null ? tr("save") : tr("update"), v -> {
            String t = title.getText().toString();
            String a = artist.getText().toString();
            String l = lyrics.getText().toString();
            if (t.trim().isEmpty() || a.trim().isEmpty() || l.trim().isEmpty()) {
                toast(tr("fill_all"));
                return;
            }
            if (editingSongId == null) {
                database.addSong(t, a, l);
            } else {
                database.updateSong(editingSongId, t, a, l);
                editingSongId = null;
            }
            title.setText("");
            artist.setText("");
            lyrics.setText("");
            toast(tr("song_added"));
            showAdmin();
        }, true);
        Button lock = smallButton(tr("lock"), v -> {
            adminUnlocked = false;
            editingSongId = null;
            showHome();
        }, false);
        actions.addView(save, new LinearLayout.LayoutParams(wrap(), dp(40)));
        Space actionGap = new Space(this);
        actions.addView(actionGap, new LinearLayout.LayoutParams(dp(10), dp(40)));
        if (editing != null) {
            Button cancel = smallButton(tr("cancel"), v -> {
                editingSongId = null;
                showAdmin();
            }, false);
            actions.addView(cancel, new LinearLayout.LayoutParams(wrap(), dp(40)));
            Space cancelGap = new Space(this);
            actions.addView(cancelGap, new LinearLayout.LayoutParams(dp(10), dp(40)));
        }
        actions.addView(lock, new LinearLayout.LayoutParams(wrap(), dp(40)));
        body.addView(actions, spaced(matchWidth(), dp(44), 0, dp(18), 0, dp(28)));

        body.addView(smallButton(tr("admin_list"), v -> showAdminListDialog(), false), spaced(wrap(), dp(40), 0, 0, 0, dp(18)));
        render(body);
    }

    private void showAdminLogin() {
        LinearLayout body = screen();
        EditText password = input(tr("admin_password"));
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        body.addView(password, spaced(matchWidth(), dp(56), 0, dp(18), 0, 0));
        body.addView(smallButton(tr("enter"), v -> {
            String expected = preferences.getString("admin_password", "1234");
            if (password.getText().toString().equals(expected)) {
                adminUnlocked = true;
                showAdmin();
            } else {
                toast(tr("wrong_password"));
            }
        }, true), spaced(wrap(), dp(40), 0, dp(16), 0, 0));
        render(body);
    }

    private void showSettings() {
        setTitleText(tr("settings"));
        LinearLayout body = screen();
        body.addView(sectionTitle(tr("settings")));
        body.addView(languageSettingCard());
        body.addView(settingActionCard("🔒", tr("admin_security"), tr("admin_security_sub"), () -> showPasswordDialog()));
        render(body);
    }

    private void showImportExport() {
        setTitleText(tr("import_export"));
        LinearLayout body = screen();
        body.addView(sectionTitle(tr("import_export")));
        body.addView(settingActionCard("⬇", tr("import_data"), tr("import_data_sub"), () -> startImport()));
        body.addView(settingActionCard("⬆", tr("export_data"), tr("export_data_sub"), () -> startExport()));
        render(body);
    }

    private void showAbout() {
        setTitleText(tr("about"));
        LinearLayout body = screen();
        String[] lines = tr("about_text").split("\\n", 2);
        TextView version = note(lines[0]);
        body.addView(version, spaced(matchWidth(), wrap(), 0, 0, 0, 0));
        Space spacer = new Space(this);
        body.addView(spacer, new LinearLayout.LayoutParams(matchWidth(), dp(420)));
        body.addView(note(lines.length > 1 ? lines[1] : "Copyright by Telina Randrenanja"));
        render(body);
    }

    private void showPasswordDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(8), dp(18), 0);

        EditText oldPass = input(tr("old_password"));
        oldPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText newPass = input(tr("new_password"));
        newPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(oldPass, spaced(matchWidth(), dp(56), 0, dp(8), 0, 0));
        form.addView(newPass, spaced(matchWidth(), dp(56), 0, dp(12), 0, 0));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(tr("admin_security"))
                .setView(form)
                .setNegativeButton(tr("close"), null)
                .setPositiveButton(tr("change"), null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String expected = preferences.getString("admin_password", "1234");
            if (!oldPass.getText().toString().equals(expected)) {
                toast(tr("old_wrong"));
                return;
            }
            if (newPass.getText().toString().trim().length() < 4) {
                toast(tr("pass_short"));
                return;
            }
            preferences.edit().putString("admin_password", newPass.getText().toString()).apply();
            adminUnlocked = false;
            toast(tr("pass_changed"));
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void startExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "victory-voice-data.json");
        startActivityForResult(intent, REQUEST_EXPORT);
    }

    private void startImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        try {
            if (requestCode == REQUEST_EXPORT && data.getData() != null) {
                writeExportFile(data.getData());
                toast(tr("export_done"));
            } else if (requestCode == REQUEST_IMPORT) {
                int imported = importSelectedFiles(data);
                toast(tr("import_done") + " " + imported);
                showHome();
            }
        } catch (Exception e) {
            toast(tr("transfer_error"));
        }
    }

    private void writeExportFile(Uri uri) throws Exception {
        JSONObject root = new JSONObject();
        root.put("app", "Victory Voice");
        root.put("version", 1);
        JSONObject settings = new JSONObject();
        settings.put("language", preferences.getString("language", "fr"));
        settings.put("dark_mode", preferences.getBoolean("dark_mode", false));
        settings.put("lyrics_size", preferences.getInt("lyrics_size", 19));
        settings.put("lyrics_align", preferences.getString("lyrics_align", "center"));
        root.put("settings", settings);

        JSONArray songs = new JSONArray();
        for (Song song : database.allSongs()) {
            JSONObject item = new JSONObject();
            item.put("title", song.title);
            item.put("artist", song.artist);
            item.put("lyrics", song.lyrics);
            item.put("favorite", database.isFavorite(song.id));
            songs.put(item);
        }
        root.put("songs", songs);

        OutputStream stream = getContentResolver().openOutputStream(uri);
        if (stream == null) {
            throw new IllegalStateException("No output stream");
        }
        stream.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        stream.close();
    }

    private int importSelectedFiles(Intent data) throws Exception {
        int imported = 0;
        ClipData clipData = data.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                imported += importFile(clipData.getItemAt(i).getUri());
            }
            return imported;
        }
        Uri uri = data.getData();
        return uri == null ? 0 : importFile(uri);
    }

    private int importFile(Uri uri) throws Exception {
        InputStream stream = getContentResolver().openInputStream(uri);
        if (stream == null) {
            return 0;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = stream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        stream.close();

        JSONObject root = new JSONObject(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        JSONObject settings = root.optJSONObject("settings");
        if (settings != null) {
            preferences.edit()
                    .putString("language", settings.optString("language", preferences.getString("language", "fr")))
                    .putBoolean("dark_mode", settings.optBoolean("dark_mode", darkMode()))
                    .putInt("lyrics_size", settings.optInt("lyrics_size", preferences.getInt("lyrics_size", 19)))
                    .putString("lyrics_align", settings.optString("lyrics_align", preferences.getString("lyrics_align", "center")))
                    .apply();
        }

        int imported = 0;
        JSONArray songs = root.optJSONArray("songs");
        if (songs == null) {
            return 0;
        }
        for (int i = 0; i < songs.length(); i++) {
            JSONObject item = songs.getJSONObject(i);
            String title = item.optString("title").trim();
            String artist = item.optString("artist").trim();
            String lyrics = item.optString("lyrics").trim();
            if (title.isEmpty() || artist.isEmpty() || lyrics.isEmpty()) {
                continue;
            }
            long id = database.upsertSong(title, artist, lyrics);
            if (item.optBoolean("favorite", false)) {
                database.setFavorite(id, true);
            }
            imported++;
        }
        return imported;
    }

    private void showAdminListDialog() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(12), dp(8), dp(12), dp(8));
        for (Song song : database.allSongs()) {
            list.addView(adminSongCard(song));
        }
        scroll.addView(list, new ScrollView.LayoutParams(matchWidth(), wrap()));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(tr("admin_list"))
                .setView(scroll)
                .setNegativeButton(tr("close"), null)
                .create();
        dialog.setOnShowListener(d -> {
            ViewGroup parent = (ViewGroup) list.getParent();
            parent.setTag(dialog);
        });
        dialog.show();
    }

    private void addSongCards(LinearLayout parent, List<Song> songs, boolean showEmpty) {
        if (songs.isEmpty()) {
            if (showEmpty) {
                parent.addView(empty("Aucun chant trouvé."));
            }
            return;
        }
        for (Song song : songs) {
            parent.addView(songCard(song));
        }
    }

    private View songCard(Song song) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackground(cardBackground(cardFill(), Color.rgb(183, 211, 255)));
        card.setClickable(true);
        card.setElevation(dp(2));

        LinearLayout titleLine = new LinearLayout(this);
        titleLine.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label(song.title, 20, primaryText(), Typeface.BOLD);
        titleLine.addView(title, new LinearLayout.LayoutParams(0, wrap(), 1));
        TextView star = label(database.isFavorite(song.id) ? "★" : "☆", 24, BLUE, Typeface.BOLD);
        star.setGravity(Gravity.CENTER);
        star.setClickable(true);
        star.setOnClickListener(v -> {
            boolean next = !database.isFavorite(song.id);
            database.setFavorite(song.id, next);
            star.setText(next ? "★" : "☆");
        });
        titleLine.addView(star, new LinearLayout.LayoutParams(dp(48), dp(48)));
        card.addView(titleLine);

        TextView artist = label(song.artist, 16, BLUE, Typeface.BOLD);
        artist.setPadding(0, dp(6), 0, 0);
        card.addView(artist);

        card.setOnClickListener(v -> showSong(song));
        return withMargin(card, 0, 0, 0, dp(14));
    }

    private View actionCard(String title, String subtitle, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackground(cardBackground(cardFill(), Color.rgb(183, 211, 255)));
        card.setClickable(true);
        card.setElevation(dp(2));
        card.addView(label(title, 20, primaryText(), Typeface.BOLD));
        TextView sub = label(subtitle, 15, BLUE, Typeface.BOLD);
        sub.setPadding(0, dp(6), 0, 0);
        card.addView(sub);
        card.setOnClickListener(v -> action.run());
        return withMargin(card, 0, 0, 0, dp(14));
    }

    private View adminSongCard(Song song) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackground(cardBackground(darkMode() ? Color.rgb(13, 27, 51) : Color.WHITE, Color.rgb(183, 211, 255)));
        card.addView(label(song.title, 20, darkMode() ? Color.WHITE : TEXT, Typeface.BOLD));
        TextView artist = label(song.artist, 15, BLUE, Typeface.BOLD);
        artist.setPadding(0, dp(4), 0, dp(10));
        card.addView(artist);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button edit = translucentButton("✎", v -> {
            editingSongId = song.id;
            dismissAncestorDialog((View) v);
            showAdmin();
        });
        Button delete = translucentButton("🗑", v -> {
            database.deleteSong(song.id);
            if (editingSongId != null && editingSongId == song.id) {
                editingSongId = null;
            }
            dismissAncestorDialog((View) v);
            showAdmin();
        });
        row.addView(edit, new LinearLayout.LayoutParams(wrap(), dp(40)));
        Space gap = new Space(this);
        row.addView(gap, new LinearLayout.LayoutParams(dp(10), dp(40)));
        row.addView(delete, new LinearLayout.LayoutParams(wrap(), dp(40)));
        card.addView(row);
        return withMargin(card, 0, 0, 0, dp(14));
    }

    private void dismissAncestorDialog(View view) {
        View current = view;
        while (current != null) {
            Object tag = current.getTag();
            if (tag instanceof AlertDialog) {
                ((AlertDialog) tag).dismiss();
                return;
            }
            if (!(current.getParent() instanceof View)) {
                return;
            }
            current = (View) current.getParent();
        }
    }

    private Song findSong(Long id) {
        if (id == null) {
            return null;
        }
        for (Song song : database.allSongs()) {
            if (song.id == id) {
                return song;
            }
        }
        return null;
    }

    private View hero(String title, String subtitle) {
        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(cardBackground(NAVY, BLUE));
        hero.setClipToOutline(false);
        hero.addView(new WaveHeroView(this), match());

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setPadding(dp(22), dp(16), dp(22), dp(16));
        if (!title.trim().isEmpty()) {
            text.addView(label(title, 31, Color.WHITE, Typeface.BOLD));
        }
        if (!subtitle.trim().isEmpty()) {
            TextView sub = label(subtitle, 16, Color.rgb(207, 231, 255), Typeface.NORMAL);
            sub.setPadding(0, dp(10), 0, 0);
            sub.setLineSpacing(dp(2), 1f);
            text.addView(sub);
        }
        hero.addView(text, match());
        return withMargin(hero, 0, 0, 0, dp(16), dp(132));
    }

    private LinearLayout screen() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(18), dp(16), dp(24));
        return body;
    }

    private void render(LinearLayout body) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(body, new ScrollView.LayoutParams(matchWidth(), wrap()));
        content.removeAllViews();
        content.addView(scroll, match());
    }

    private TextView sectionTitle(String text) {
        TextView view = label(text, 22, primaryText(), Typeface.BOLD);
        view.setPadding(0, dp(8), 0, dp(14));
        return view;
    }

    private TextView empty(String text) {
        TextView view = label(text, 16, mutedText(), Typeface.NORMAL);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(18), dp(30), dp(18), dp(30));
        view.setBackground(cardBackground(cardFill(), Color.rgb(82, 117, 169)));
        return view;
    }

    private TextView note(String text) {
        TextView view = label(text, 16, mutedText(), Typeface.NORMAL);
        view.setLineSpacing(dp(2), 1f);
        view.setPadding(dp(18), dp(18), dp(18), dp(18));
        view.setBackground(cardBackground(cardFill(), Color.rgb(82, 117, 169)));
        return view;
    }

    private TextView settingRow(String title, String subtitle) {
        TextView view = label(title + "\n" + subtitle, 16, primaryText(), Typeface.BOLD);
        view.setLineSpacing(dp(4), 1f);
        view.setPadding(dp(18), dp(16), dp(18), dp(16));
        view.setBackground(cardBackground(cardFill(), Color.rgb(82, 117, 169)));
        return view;
    }

    private View languageSettingCard() {
        LinearLayout card = settingCardBase("Aa", tr("language"), tr("language_sub"));
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        boolean en = "en".equals(preferences.getString("language", "fr"));
        Button french = smallButton("FR", v -> {
            preferences.edit().putString("language", "fr").apply();
            recreate();
        }, !en);
        Button english = smallButton("EN", v -> {
            preferences.edit().putString("language", "en").apply();
            recreate();
        }, en);
        buttons.addView(french, new LinearLayout.LayoutParams(dp(58), dp(40)));
        Space gap = new Space(this);
        buttons.addView(gap, new LinearLayout.LayoutParams(dp(8), dp(40)));
        buttons.addView(english, new LinearLayout.LayoutParams(dp(58), dp(40)));
        card.addView(buttons, new LinearLayout.LayoutParams(wrap(), dp(44)));
        return withMargin(card, 0, 0, 0, dp(12));
    }

    private View settingActionCard(String icon, String title, String subtitle, Runnable action) {
        LinearLayout card = settingCardBase(icon, title, subtitle);
        card.setClickable(true);
        card.setOnClickListener(v -> action.run());
        TextView arrow = label("›", 30, mutedText(), Typeface.BOLD);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(28), matchHeight()));
        return withMargin(card, 0, 0, 0, dp(12));
    }

    private LinearLayout settingCardBase(String icon, String title, String subtitle) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(cardBackground(cardFill(), Color.rgb(82, 117, 169)));

        TextView iconView = label(icon, 18, BLUE, Typeface.BOLD);
        iconView.setGravity(Gravity.CENTER);
        iconView.setBackground(circleBg(Color.argb(darkMode() ? 70 : 34, 29, 109, 255)));
        card.addView(iconView, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setPadding(dp(14), 0, dp(10), 0);
        text.addView(label(title, 17, primaryText(), Typeface.BOLD));
        TextView subtitleView = label(subtitle, 14, mutedText(), Typeface.NORMAL);
        subtitleView.setPadding(0, dp(3), 0, 0);
        text.addView(subtitleView);
        card.addView(text, new LinearLayout.LayoutParams(0, wrap(), 1));
        return card;
    }

    private EditText input(String hint) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setTextColor(primaryText());
        edit.setHintTextColor(darkMode() ? Color.rgb(168, 181, 200) : Color.rgb(130, 146, 170));
        edit.setTextSize(16);
        edit.setSingleLine(false);
        edit.setPadding(dp(16), 0, dp(16), 0);
        edit.setBackground(cardBackground(cardFill(), Color.rgb(82, 117, 169)));
        return edit;
    }

    private Button button(String text, View.OnClickListener listener, boolean primary) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? Color.WHITE : BLUE);
        button.setBackground(cardBackground(primary ? BLUE : cardFill(), BLUE));
        if (listener != null) {
            button.setOnClickListener(listener);
        }
        return button;
    }

    private Button smallButton(String text, View.OnClickListener listener, boolean primary) {
        Button button = button(text, listener, primary);
        button.setTextSize(14);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setMinimumHeight(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(12), 0, dp(12), 0);
        return button;
    }

    private Button translucentButton(String text, View.OnClickListener listener) {
        Button button = smallButton(text, listener, true);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(88, 29, 109, 255));
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), Color.argb(150, 255, 255, 255));
        button.setBackground(bg);
        button.setTextColor(Color.WHITE);
        return button;
    }

    private TextView label(String text, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setIncludeFontPadding(true);
        return view;
    }

    private TextView toolbarButton(String text, int sp) {
        TextView button = label(text, sp, Color.WHITE, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        return button;
    }

    private GradientDrawable cardBackground(int fill, int stroke) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), stroke);
        return bg;
    }

    private GradientDrawable circleBg(int fill) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(fill);
        return bg;
    }

    private View withMargin(View view, int left, int top, int right, int bottom) {
        return withMargin(view, left, top, right, bottom, wrap());
    }

    private View withMargin(View view, int left, int top, int right, int bottom, int height) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(view, new LinearLayout.LayoutParams(matchWidth(), height));
        wrapper.setPadding(left, top, right, bottom);
        return wrapper;
    }

    private LinearLayout.LayoutParams spaced(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(left, top, right, bottom);
        return params;
    }

    private void openDrawer() {
        drawerLayer.setVisibility(View.VISIBLE);
    }

    private void closeDrawer() {
        drawerLayer.setVisibility(View.GONE);
    }

    private void setTitleText(String text) {
        toolbarTitle.setText(text);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean darkMode() {
        return preferences.getBoolean("dark_mode", false);
    }

    private int primaryText() {
        return darkMode() ? Color.rgb(248, 250, 252) : TEXT;
    }

    private int mutedText() {
        return darkMode() ? Color.rgb(168, 181, 200) : MUTED;
    }

    private int cardFill() {
        return darkMode() ? Color.rgb(13, 27, 51) : Color.WHITE;
    }

    private int gravityForAlign(String align) {
        if ("left".equals(align)) {
            return Gravity.START;
        }
        if ("right".equals(align)) {
            return Gravity.END;
        }
        return Gravity.CENTER_HORIZONTAL;
    }

    private String tr(String key) {
        boolean en = "en".equals(preferences.getString("language", "fr"));
        switch (key) {
            case "app":
                return "Victory Voice";
            case "home":
                return en ? "Home" : "Accueil";
            case "search":
                return en ? "Search" : "Recherche";
            case "artists":
                return en ? "Singers" : "Chanteurs";
            case "favorites":
                return en ? "Favorites" : "Titres préférés";
            case "admin":
                return en ? "Admin mode" : "Mode admin";
            case "settings":
                return en ? "Settings" : "Paramètres";
            case "import_export":
                return en ? "Import / Export" : "Importer / Exporter";
            case "about":
                return en ? "About" : "A propos";
            case "song_list":
                return en ? "Song list" : "Liste des chants";
            case "search_hint":
                return en ? "Title, singer or lyrics" : "Titre, chanteur ou parole";
            case "all_artists":
                return en ? "All singers" : "Tous les chanteurs";
            case "no_artist":
                return en ? "No singer saved." : "Aucun chanteur enregistré.";
            case "view_songs":
                return en ? "View songs" : "Voir les chants";
            case "songs_by":
                return en ? "Songs by" : "Chants de";
            case "favorite_titles":
                return en ? "Favorite titles" : "Titres préférés";
            case "no_favorite":
                return en ? "No favorite yet." : "Aucun favori pour le moment.";
            case "lyrics":
                return en ? "Lyrics" : "Paroles";
            case "remove_fav":
                return en ? "★ Remove" : "★ Retirer";
            case "add_fav":
                return en ? "☆ Favorite" : "☆ Favori";
            case "song_title":
                return en ? "Song title" : "Titre du chant";
            case "artist_name":
                return en ? "Singer name" : "Nom du chanteur";
            case "save":
                return en ? "Save" : "Enregistrer";
            case "lock":
                return en ? "Lock" : "Verrouiller";
            case "fill_all":
                return en ? "Fill in the title, singer and lyrics." : "Remplissez le titre, le chanteur et les paroles.";
            case "song_added":
                return en ? "Song saved." : "Chant ajouté avec succès.";
            case "admin_password":
                return en ? "Admin password" : "Mot de passe admin";
            case "enter":
                return en ? "Enter" : "Entrer";
            case "wrong_password":
                return en ? "Wrong password." : "Mot de passe incorrect.";
            case "language":
                return en ? "Language" : "Langue";
            case "language_sub":
                return en ? "Choose the display language." : "Choisissez la langue d'affichage.";
            case "import_data":
                return en ? "Import data" : "Importer les données";
            case "import_data_sub":
                return en ? "Choose one or more Victory Voice JSON files." : "Choisissez un ou plusieurs fichiers Victory Voice JSON.";
            case "export_data":
                return en ? "Export data" : "Exporter les données";
            case "export_data_sub":
                return en ? "Create one file containing all songs and favorites." : "Créer un seul fichier avec tous les chants et favoris.";
            case "export_done":
                return en ? "Export finished." : "Export terminé.";
            case "import_done":
                return en ? "Imported songs:" : "Chants importés:";
            case "transfer_error":
                return en ? "Transfer failed." : "Transfert échoué.";
            case "admin_security":
                return en ? "Admin security" : "Sécurité admin";
            case "admin_security_sub":
                return en ? "Click to change the admin password." : "Cliquez pour modifier le mot de passe du mode admin.";
            case "old_password":
                return en ? "Old password" : "Ancien mot de passe";
            case "new_password":
                return en ? "New password" : "Nouveau mot de passe";
            case "change":
                return en ? "Change" : "Changer";
            case "close":
                return en ? "Close" : "Fermer";
            case "old_wrong":
                return en ? "Old password is wrong." : "Ancien mot de passe incorrect.";
            case "pass_short":
                return en ? "Use at least 4 characters." : "Choisissez au moins 4 caractères.";
            case "pass_changed":
                return en ? "Password changed." : "Mot de passe admin changé.";
            case "update":
                return en ? "Update" : "Mettre à jour";
            case "cancel":
                return en ? "Cancel" : "Annuler";
            case "edit":
                return en ? "Edit" : "Modifier";
            case "delete":
                return en ? "Delete" : "Supprimer";
            case "admin_list":
                return en ? "Edit or delete a song" : "Modifier ou supprimer un chant";
            case "about_text":
                return en ? "APK version 1.0\nCopyright by Telina Randrenanja" : "Version APK 1.0\nCopyright by Telina Randrenanja";
            default:
                return key;
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int matchWidth() {
        return ViewGroup.LayoutParams.MATCH_PARENT;
    }

    private int matchHeight() {
        return ViewGroup.LayoutParams.MATCH_PARENT;
    }

    private int wrap() {
        return ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    private FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(matchWidth(), matchHeight());
    }

    private static final class SimpleWatcher implements TextWatcher {
        private final Runnable after;

        SimpleWatcher(Runnable after) {
            this.after = after;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            after.run();
        }
    }

    private final class WaveToolbarView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        WaveToolbarView(Activity context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            paint.setShader(new LinearGradient(0, 0, w, h, NAVY, NAVY_SOFT, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.argb(120, 84, 217, 255));
            path.reset();
            path.moveTo(w * 0.45f, h + dp(12));
            path.cubicTo(w * 0.62f, h * 0.2f, w * 0.75f, h * 1.05f, w + dp(24), h * 0.15f);
            canvas.drawPath(path, paint);
        }
    }

    private class WaveHeaderView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        WaveHeaderView(Activity context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            drawWaves(canvas, getWidth(), getHeight(), true);
        }

        private void drawWaves(Canvas canvas, int w, int h, boolean particles) {
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(0, 0, w, h, NAVY, Color.rgb(4, 34, 91), Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            for (int i = 0; i < 3; i++) {
                paint.setStrokeWidth(dp(8 - i * 2));
                paint.setColor(Color.argb(78 - i * 12, 29, 109, 255));
                path.reset();
                path.moveTo(-dp(30), h * (0.74f - i * 0.1f));
                path.cubicTo(w * 0.28f, h * (0.25f + i * 0.04f), w * 0.62f, h * (0.95f - i * 0.08f), w + dp(36), h * (0.18f + i * 0.05f));
                canvas.drawPath(path, paint);
            }
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.argb(170, 84, 217, 255));
            path.reset();
            path.moveTo(-dp(18), h * 0.70f);
            path.cubicTo(w * 0.26f, h * 0.22f, w * 0.64f, h * 0.88f, w + dp(20), h * 0.24f);
            canvas.drawPath(path, paint);

            if (particles) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(170, 84, 217, 255));
                for (int i = 0; i < 18; i++) {
                    float x = (i * 37 % Math.max(w, 1));
                    float y = dp(22) + (i * 29 % Math.max(h - dp(44), 1));
                    canvas.drawCircle(x, y, dp(i % 3 == 0 ? 2 : 1), paint);
                }
            }
        }
    }

    private final class WaveHeroView extends WaveHeaderView {
        WaveHeroView(Activity context) {
            super(context);
        }
    }
}
