package com.jassara.tononkira;

import android.app.Activity;
import android.content.SharedPreferences;
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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = new TononkiraDatabase(this);
        preferences = getSharedPreferences("tononkira_settings", MODE_PRIVATE);

        Window window = getWindow();
        window.setStatusBarColor(NAVY);
        window.setNavigationBarColor(NAVY);

        root = new FrameLayout(this);
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(SURFACE);
        root.addView(main, match());

        main.addView(createToolbar(), new LinearLayout.LayoutParams(matchWidth(), dp(76)));
        content = new FrameLayout(this);
        main.addView(content, new LinearLayout.LayoutParams(matchWidth(), 0, 1));

        drawerLayer = createDrawer();
        root.addView(drawerLayer, match());
        setContentView(root);

        showHome();
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

        toolbarTitle = label("Tononkira", 24, Color.WHITE, Typeface.NORMAL);
        toolbarTitle.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(matchWidth(), matchHeight());
        titleParams.leftMargin = dp(72);
        titleParams.rightMargin = dp(16);
        toolbar.addView(toolbarTitle, titleParams);
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
        drawer.setBackgroundColor(Color.WHITE);
        drawer.setPadding(0, 0, 0, dp(12));
        GradientDrawable drawerBg = new GradientDrawable();
        drawerBg.setColor(Color.WHITE);
        drawerBg.setCornerRadii(new float[]{0, 0, dp(28), dp(28), dp(28), dp(28), 0, 0});
        drawer.setBackground(drawerBg);
        drawer.setElevation(dp(10));

        FrameLayout header = new FrameLayout(this);
        header.addView(new WaveHeaderView(this), match());
        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);
        headerText.setGravity(Gravity.CENTER_VERTICAL);
        headerText.setPadding(dp(28), dp(12), dp(18), dp(12));
        headerText.addView(label("Tononkira", 34, Color.WHITE, Typeface.BOLD));
        TextView site = label("serasera.org", 18, Color.rgb(204, 233, 255), Typeface.BOLD);
        site.setPadding(0, dp(6), 0, 0);
        headerText.addView(site);
        header.addView(headerText, match());
        drawer.addView(header, new LinearLayout.LayoutParams(matchWidth(), dp(168)));

        addDrawerItem(drawer, "⌂", "Accueil", () -> showHome());
        addDrawerItem(drawer, "⌕", "Recherche", () -> showSearch(""));
        addDrawerItem(drawer, "♪", "Chanteurs", () -> showArtists());
        addDrawerItem(drawer, "★", "Titres préférés", () -> showFavorites());
        addDrawerItem(drawer, "＋", "Mode admin", () -> showAdmin());
        addDrawerItem(drawer, "⚙", "Paramètres", () -> showSettings());
        addDrawerItem(drawer, "i", "A propos", () -> showAbout());

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(314), matchHeight());
        params.gravity = Gravity.START;
        layer.addView(drawer, params);
        return layer;
    }

    private void addDrawerItem(LinearLayout drawer, String icon, String title, Runnable action) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(26), 0, dp(18), 0);
        item.setClickable(true);
        item.setBackgroundColor(Color.WHITE);

        TextView iconView = label(icon, 28, BLUE, Typeface.BOLD);
        iconView.setGravity(Gravity.CENTER);
        item.addView(iconView, new LinearLayout.LayoutParams(dp(44), matchHeight()));

        TextView titleView = label(title, 18, TEXT, Typeface.BOLD);
        titleView.setPadding(dp(16), 0, 0, 0);
        item.addView(titleView, new LinearLayout.LayoutParams(0, matchHeight(), 1));

        item.setOnClickListener(v -> {
            closeDrawer();
            action.run();
        });
        drawer.addView(item, new LinearLayout.LayoutParams(matchWidth(), dp(72)));
    }

    private void showHome() {
        setTitleText("Tononkira");
        LinearLayout body = screen();
        body.addView(hero("Tononkira", "Chants, paroles, favoris et ajout admin dans une seule APK simple."));
        body.addView(sectionTitle("Derniers chants"));
        addSongCards(body, database.allSongs(), false);
        render(body);
    }

    private void showSearch(String initialTerm) {
        setTitleText("Recherche");
        LinearLayout body = screen();
        body.addView(sectionTitle("A rechercher"));

        EditText search = input("Titre, chanteur ou parole");
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
        setTitleText("Chanteurs");
        LinearLayout body = screen();
        body.addView(sectionTitle("Tous les chanteurs"));
        List<String> artists = database.artists();
        if (artists.isEmpty()) {
            body.addView(empty("Aucun chanteur enregistré."));
        } else {
            for (String artistLabel : artists) {
                String artist = artistLabel.replaceFirst(" \\([0-9]+\\)$", "");
                body.addView(actionCard(artistLabel, "Voir les chants", () -> showArtistSongs(artist)));
            }
        }
        render(body);
    }

    private void showArtistSongs(String artist) {
        setTitleText(artist);
        LinearLayout body = screen();
        body.addView(sectionTitle("Chants de " + artist));
        addSongCards(body, database.songsByArtist(artist), false);
        render(body);
    }

    private void showFavorites() {
        setTitleText("Favoris");
        LinearLayout body = screen();
        body.addView(sectionTitle("Titres préférés"));
        List<Song> songs = database.favorites();
        if (songs.isEmpty()) {
            body.addView(empty("Aucun favori pour le moment. Ouvrez un chant et touchez le bouton favori."));
        } else {
            addSongCards(body, songs, false);
        }
        render(body);
    }

    private void showSong(Song song) {
        setTitleText("Paroles");
        LinearLayout body = screen();
        body.addView(button("← Retour", v -> showSearch(song.title), false));

        TextView title = label(song.title, 30, NAVY, Typeface.BOLD);
        title.setPadding(0, dp(18), 0, 0);
        body.addView(title);
        TextView artist = label(song.artist, 18, BLUE, Typeface.BOLD);
        artist.setPadding(0, dp(6), 0, dp(16));
        body.addView(artist);

        final boolean[] favorite = {database.isFavorite(song.id)};
        Button favButton = button(favorite[0] ? "★ Retirer des favoris" : "☆ Ajouter aux favoris", null, true);
        favButton.setOnClickListener(v -> {
            favorite[0] = !favorite[0];
            database.setFavorite(song.id, favorite[0]);
            favButton.setText(favorite[0] ? "★ Retirer des favoris" : "☆ Ajouter aux favoris");
        });
        body.addView(favButton, spaced(matchWidth(), dp(52), 0, 0, 0, dp(18)));

        TextView lyrics = label(song.lyrics, 19, TEXT, Typeface.NORMAL);
        lyrics.setLineSpacing(dp(4), 1.08f);
        lyrics.setPadding(dp(20), dp(20), dp(20), dp(20));
        lyrics.setBackground(cardBackground(Color.WHITE, Color.rgb(210, 225, 255)));
        body.addView(lyrics, spaced(matchWidth(), wrap(), 0, 0, 0, dp(28)));
        render(body);
    }

    private void showAdmin() {
        setTitleText("Mode admin");
        if (!adminUnlocked) {
            showAdminLogin();
            return;
        }

        LinearLayout body = screen();
        body.addView(hero("Ajouter un chant", "Enregistrez un titre, un chanteur et les paroles. Les données restent dans SQLite sur le téléphone."));

        EditText title = input("Titre du chant");
        EditText artist = input("Nom du chanteur");
        EditText lyrics = input("Paroles");
        lyrics.setMinLines(6);
        lyrics.setGravity(Gravity.TOP | Gravity.START);
        lyrics.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        body.addView(title, spaced(matchWidth(), dp(56), 0, dp(14), 0, 0));
        body.addView(artist, spaced(matchWidth(), dp(56), 0, dp(14), 0, 0));
        body.addView(lyrics, spaced(matchWidth(), dp(180), 0, dp(14), 0, 0));
        body.addView(button("Enregistrer le chant", v -> {
            String t = title.getText().toString();
            String a = artist.getText().toString();
            String l = lyrics.getText().toString();
            if (t.trim().isEmpty() || a.trim().isEmpty() || l.trim().isEmpty()) {
                toast("Remplissez le titre, le chanteur et les paroles.");
                return;
            }
            database.addSong(t, a, l);
            title.setText("");
            artist.setText("");
            lyrics.setText("");
            toast("Chant ajouté avec succès.");
            showHome();
        }, true), spaced(matchWidth(), dp(54), 0, dp(18), 0, dp(28)));

        body.addView(button("Verrouiller le mode admin", v -> {
            adminUnlocked = false;
            showHome();
        }, false));
        render(body);
    }

    private void showAdminLogin() {
        LinearLayout body = screen();
        body.addView(hero("Mode admin", "Connectez-vous pour ajouter de nouveaux chants dans la base locale."));
        EditText password = input("Mot de passe admin");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        body.addView(password, spaced(matchWidth(), dp(56), 0, dp(18), 0, 0));
        body.addView(button("Entrer", v -> {
            String expected = preferences.getString("admin_password", "1234");
            if (password.getText().toString().equals(expected)) {
                adminUnlocked = true;
                showAdmin();
            } else {
                toast("Mot de passe incorrect.");
            }
        }, true), spaced(matchWidth(), dp(54), 0, dp(16), 0, 0));
        body.addView(note("Mot de passe par défaut: 1234. Vous pouvez le changer dans Paramètres."));
        render(body);
    }

    private void showSettings() {
        setTitleText("Paramètres");
        LinearLayout body = screen();
        body.addView(sectionTitle("Sécurité admin"));

        EditText oldPass = input("Ancien mot de passe");
        oldPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText newPass = input("Nouveau mot de passe");
        newPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        body.addView(oldPass, spaced(matchWidth(), dp(56), 0, dp(12), 0, 0));
        body.addView(newPass, spaced(matchWidth(), dp(56), 0, dp(12), 0, 0));
        body.addView(button("Changer le mot de passe", v -> {
            String expected = preferences.getString("admin_password", "1234");
            if (!oldPass.getText().toString().equals(expected)) {
                toast("Ancien mot de passe incorrect.");
                return;
            }
            if (newPass.getText().toString().trim().length() < 4) {
                toast("Choisissez au moins 4 caractères.");
                return;
            }
            preferences.edit().putString("admin_password", newPass.getText().toString()).apply();
            oldPass.setText("");
            newPass.setText("");
            adminUnlocked = false;
            toast("Mot de passe admin changé.");
        }, true), spaced(matchWidth(), dp(54), 0, dp(16), 0, dp(28)));

        body.addView(sectionTitle("Base de données"));
        body.addView(note("Les chants ajoutés, favoris et réglages sont stockés localement dans SQLite. L'APK fonctionne sans internet."));
        render(body);
    }

    private void showAbout() {
        setTitleText("A propos");
        LinearLayout body = screen();
        body.addView(hero("Tononkira", "Application APK de chants avec recherche rapide, favoris et mode admin."));
        body.addView(note("Thème inspiré de l'image bleu nuit avec rubans lumineux. Version 1.0."));
        render(body);
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
        card.setBackground(cardBackground(Color.WHITE, Color.rgb(183, 211, 255)));
        card.setClickable(true);
        card.setElevation(dp(2));

        LinearLayout titleLine = new LinearLayout(this);
        titleLine.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label(song.title, 20, TEXT, Typeface.BOLD);
        titleLine.addView(title, new LinearLayout.LayoutParams(0, wrap(), 1));
        TextView star = label(database.isFavorite(song.id) ? "★" : "☆", 24, BLUE, Typeface.BOLD);
        titleLine.addView(star, new LinearLayout.LayoutParams(dp(34), wrap()));
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
        card.setBackground(cardBackground(Color.WHITE, Color.rgb(183, 211, 255)));
        card.setClickable(true);
        card.setElevation(dp(2));
        card.addView(label(title, 20, TEXT, Typeface.BOLD));
        TextView sub = label(subtitle, 15, BLUE, Typeface.BOLD);
        sub.setPadding(0, dp(6), 0, 0);
        card.addView(sub);
        card.setOnClickListener(v -> action.run());
        return withMargin(card, 0, 0, 0, dp(14));
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
        text.addView(label(title, 31, Color.WHITE, Typeface.BOLD));
        TextView sub = label(subtitle, 16, Color.rgb(207, 231, 255), Typeface.NORMAL);
        sub.setPadding(0, dp(10), 0, 0);
        sub.setLineSpacing(dp(2), 1f);
        text.addView(sub);
        hero.addView(text, match());
        return withMargin(hero, 0, 0, 0, dp(22), dp(170));
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
        TextView view = label(text, 22, NAVY, Typeface.BOLD);
        view.setPadding(0, dp(8), 0, dp(14));
        return view;
    }

    private TextView empty(String text) {
        TextView view = label(text, 16, MUTED, Typeface.NORMAL);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(18), dp(30), dp(18), dp(30));
        view.setBackground(cardBackground(Color.WHITE, Color.rgb(220, 230, 245)));
        return view;
    }

    private TextView note(String text) {
        TextView view = label(text, 16, MUTED, Typeface.NORMAL);
        view.setLineSpacing(dp(2), 1f);
        view.setPadding(dp(18), dp(18), dp(18), dp(18));
        view.setBackground(cardBackground(Color.WHITE, Color.rgb(220, 230, 245)));
        return view;
    }

    private EditText input(String hint) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setTextColor(TEXT);
        edit.setHintTextColor(Color.rgb(130, 146, 170));
        edit.setTextSize(16);
        edit.setSingleLine(false);
        edit.setPadding(dp(16), 0, dp(16), 0);
        edit.setBackground(cardBackground(Color.WHITE, Color.rgb(170, 205, 255)));
        return edit;
    }

    private Button button(String text, View.OnClickListener listener, boolean primary) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? Color.WHITE : BLUE);
        button.setBackground(cardBackground(primary ? BLUE : Color.WHITE, BLUE));
        if (listener != null) {
            button.setOnClickListener(listener);
        }
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
