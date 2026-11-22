package org.telegram.ui.nebula;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.text.TextWatcher;
import android.text.Editable;
import java.util.Locale;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Manages HTTPS downloads started from this Nebula screen, not Telegram media transfers. */
public final class NebulaDownloadsActivity extends NebulaProtectedActivity {
    private static final String PREF = "nebula_downloads_v1";
    private static final String KEY = "download_ids";
    private static final String METERED = "wifi_only";
    private static final String FILTER = "status_filter";
    private LinearLayout items;
    private TextView summary;
    private int activeFilter;
    private String searchQuery = "";
    private DownloadManager manager;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        LinearLayout root = NebulaToolUi.page(this, "Nebula · Завантаження",
            "Окремі HTTPS-завантаження через системний Android DownloadManager. " +
            "Файли Telegram тут не відображаються. Посилання передаються системному менеджеру завантажень.");
        SharedPreferences prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        activeFilter = prefs.getInt(FILTER, 0);
        CheckBox wifi = new CheckBox(this);
        wifi.setText("Нові завантаження: лише через Wi-Fi / без тарифікованого трафіку");
        wifi.setTextColor(0xffffffff);
        wifi.setChecked(prefs.getBoolean(METERED, false));
        wifi.setOnCheckedChangeListener((button, enabled) -> prefs.edit().putBoolean(METERED, enabled).apply());
        root.addView(wifi);
        NebulaToolUi.button(this, root, "Завантажити за HTTPS-посиланням", this::promptDownload);
        NebulaToolUi.button(this, root, "Фільтр: усі / активні / завершені / помилки", () -> {
            final String[] names = {"Усі", "Активні", "Завершені", "Помилки"};
            new AlertDialog.Builder(this).setTitle("Показувати завантаження")
                .setSingleChoiceItems(names, activeFilter, (dialog, which) -> {
                    activeFilter = which;
                    prefs.edit().putInt(FILTER, which).apply();
                    dialog.dismiss(); refresh();
                }).setNegativeButton("Закрити", null).show();
        });
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Пошук за назвою файлу, адресою або ID");
        search.setTextColor(0xffffffff);
        search.setHintTextColor(0xffb8abc9);
        root.addView(search);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                searchQuery = text.toString().trim().toLowerCase(Locale.ROOT);
                refresh();
            }
            @Override public void afterTextChanged(Editable text) {}
        });
        NebulaToolUi.button(this, root, "Очистити завершені записи (файли залишаться)", () -> confirmCleanup(false));
        NebulaToolUi.button(this, root, "Прибрати помилки та відсутні записи", () -> confirmCleanup(true));
        summary = new TextView(this);
        summary.setTextColor(0xffc6b6db);
        summary.setTextSize(14);
        root.addView(summary);
        NebulaToolUi.button(this, root, "Оновити список", this::refresh);
        items = new LinearLayout(this); items.setOrientation(LinearLayout.VERTICAL);
        root.addView(items);
        refresh();
    }
    @Override protected void onResume() { super.onResume(); if (items != null) refresh(); }

    private Set<Long> ids() {
        Set<Long> result = new HashSet<>();
        for (String raw : getSharedPreferences(PREF, MODE_PRIVATE).getStringSet(KEY, Collections.emptySet())) {
            try { long id = Long.parseLong(raw); if (id > 0) result.add(id); }
            catch (NumberFormatException ignored) {}
        }
        return result;
    }
    private boolean persist(Set<Long> values) {
        Set<String> raw = new HashSet<>();
        for (long id : values) raw.add(Long.toString(id));
        return getSharedPreferences(PREF, MODE_PRIVATE).edit().putStringSet(KEY, raw).commit();
    }
    private void promptDownload() {
        EditText url = new EditText(this);
        url.setSingleLine(true); url.setHint("https://example.org/file.pdf");
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        new AlertDialog.Builder(this).setTitle("Нове завантаження")
            .setMessage("Лише HTTPS. Не вводьте приватні посилання з токенами, якщо не довіряєте джерелу.")
            .setView(url).setNegativeButton("Скасувати", null)
            .setPositiveButton("Завантажити", (dialog, which) -> enqueue(url.getText().toString().trim())).show();
    }
    private void enqueue(String value) {
        try {
            Uri uri = Uri.parse(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getHost().isEmpty()
                    || uri.getUserInfo() != null || value.length() > 4096) {
                throw new IllegalArgumentException("Only HTTPS URLs without embedded credentials are supported");
            }
            if (manager == null) throw new IllegalStateException("DownloadManager unavailable");
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setAllowedOverMetered(!getSharedPreferences(PREF, MODE_PRIVATE).getBoolean(METERED, false));
            request.setTitle("Nebula download");
            request.setDescription(uri.getHost());
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            // Scoped to the app's external files: no storage permission required on modern Android.
            String fileName = "nebula_" + System.currentTimeMillis() + ".download";
            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, fileName);
            long id = manager.enqueue(request);
            Set<Long> stored = ids(); stored.add(id);
            if (!persist(stored)) {
                manager.remove(id);
                throw new IllegalStateException("Could not persist download ID");
            }
            Toast.makeText(this, "Завантаження додано", Toast.LENGTH_SHORT).show();
            refresh();
        } catch (Exception e) {
            Toast.makeText(this, "Не вдалося почати завантаження: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private void refresh() {
        if (items == null) return;
        items.removeAllViews();
        if (manager == null) {
            summary.setText("Системний DownloadManager недоступний");
            return;
        }
        ArrayList<Long> ordered = new ArrayList<>(ids());
        Collections.sort(ordered, Collections.reverseOrder());
        int active = 0, complete = 0, failed = 0, missing = 0, visible = 0;
        for (long id : ordered) {
            int status = -1;
            try (Cursor cursor = manager.query(new DownloadManager.Query().setFilterById(id))) {
                if (cursor != null && cursor.moveToFirst())
                    status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            } catch (Exception ignored) { /* display unavailable entries below */ }
            if (status == DownloadManager.STATUS_SUCCESSFUL) complete++;
            else if (status == DownloadManager.STATUS_FAILED) failed++;
            else if (status == -1) missing++;
            else active++;
            boolean show = activeFilter == 0 ||
                (activeFilter == 1 && status != -1 && status != DownloadManager.STATUS_FAILED && status != DownloadManager.STATUS_SUCCESSFUL) ||
                (activeFilter == 2 && status == DownloadManager.STATUS_SUCCESSFUL) ||
                (activeFilter == 3 && (status == DownloadManager.STATUS_FAILED || status == -1));
            if (show && matchesSearch(id)) { renderOne(id); visible++; }
        }
        summary.setText("Усього: " + ordered.size() + " · активні: " + active +
            " · готові: " + complete + " · помилки: " + failed + " · відсутні: " + missing);
        if (visible == 0) NebulaToolUi.text(this, items, "Немає завантажень для вибраного фільтра.", 14, 0xffb8abc9);
    }
    private void renderOne(long id) {
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);
        try (Cursor cursor = manager.query(query)) {
            if (cursor == null || !cursor.moveToFirst()) {
                NebulaToolUi.text(this, items, "#" + id + " · запис відсутній у системі", 14, 0xffb8abc9);
                NebulaToolUi.button(this, items, "Прибрати запис #" + id, () -> forget(id));
                return;
            }
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            String displayName = safeColumn(cursor, DownloadManager.COLUMN_TITLE);
            if (!displayName.isEmpty()) NebulaToolUi.text(this, items, displayName, 14, 0xffcdb7fa);
            long total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            long done = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            String state = status == DownloadManager.STATUS_SUCCESSFUL ? "Завершено" :
                status == DownloadManager.STATUS_FAILED ? "Помилка" :
                status == DownloadManager.STATUS_RUNNING ? "Завантажується" :
                status == DownloadManager.STATUS_PAUSED ? "Призупинено" : "Очікує";
            NebulaToolUi.text(this, items, "#" + id + " · " + state + " · " +
                (done / 1024) + " / " + (total < 0 ? "?" : Long.toString(total / 1024)) + " KiB", 14, 0xffffffff);
            if (total > 0 && status != DownloadManager.STATUS_SUCCESSFUL) {
                ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                bar.setMax(1000);
                bar.setProgress((int) Math.min(1000L, Math.max(0L, done) * 1000L / total));
                items.addView(bar);
            }
            if (status == DownloadManager.STATUS_FAILED) {
                int reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                NebulaToolUi.text(this, items, "Код помилки Android DownloadManager: " + reason, 12, 0xffffb16d);
            }
            if (status == DownloadManager.STATUS_PAUSED) {
                int reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                NebulaToolUi.text(this, items, "Причина очікування (код Android): " + reason, 12, 0xffb8abc9);
            }
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                NebulaToolUi.button(this, items, "Відкрити завантаження #" + id, () -> {
                    try {
                        Uri local = manager.getUriForDownloadedFile(id);
                        if (local == null) throw new IllegalStateException("Файл недоступний");
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                        intent.setDataAndType(local, "application/octet-stream");
                        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(android.content.Intent.createChooser(intent, "Відкрити файл"));
                    } catch (Exception e) { Toast.makeText(this, "Не вдалося відкрити файл", Toast.LENGTH_SHORT).show(); }
                });
            }
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                NebulaToolUi.button(this, items, "Поділитися файлом #" + id, () -> {
                    try {
                        Uri local = manager.getUriForDownloadedFile(id);
                        if (local == null) throw new IllegalStateException("File unavailable");
                        Intent send = new Intent(Intent.ACTION_SEND);
                        send.setType("application/octet-stream");
                        send.putExtra(Intent.EXTRA_STREAM, local);
                        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(send, "Поділитися файлом"));
                    } catch (Exception e) {
                        Toast.makeText(this, "Файл недоступний для надсилання", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            NebulaToolUi.button(this, items, "Скасувати / видалити #" + id, () ->
                new AlertDialog.Builder(this).setTitle("Видалити завантаження?")
                    .setMessage("Системний DownloadManager скасує завдання і може видалити його файл.")
                    .setNegativeButton("Ні", null).setPositiveButton("Видалити", (d, w) -> {
                        try { manager.remove(id); forget(id); }
                        catch (Exception e) { Toast.makeText(this, "Не вдалося видалити", Toast.LENGTH_SHORT).show(); }
                    }).show());
        } catch (Exception e) {
            NebulaToolUi.text(this, items, "#" + id + " · помилка читання стану", 14, 0xffffb16d);
        }
    }
    private static String safeColumn(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        if (index < 0 || cursor.isNull(index)) return "";
        String value = cursor.getString(index);
        return value == null ? "" : value;
    }

    private boolean matchesSearch(long id) {
        if (searchQuery.isEmpty() || Long.toString(id).contains(searchQuery)) return true;
        try (Cursor cursor = manager.query(new DownloadManager.Query().setFilterById(id))) {
            if (cursor == null || !cursor.moveToFirst()) return false;
            String title = safeColumn(cursor, DownloadManager.COLUMN_TITLE);
            String url = safeColumn(cursor, DownloadManager.COLUMN_URI);
            return title.toLowerCase(Locale.ROOT).contains(searchQuery) ||
                url.toLowerCase(Locale.ROOT).contains(searchQuery);
        } catch (Exception ignored) { return false; }
    }

    /** Only forget tracked IDs; never delete completed downloads in a bulk cleanup. */
    private void confirmCleanup(boolean errors) {
        new AlertDialog.Builder(this)
            .setTitle(errors ? "Прибрати невдалі завантаження?" : "Очистити історію завершених?")
            .setMessage(errors ? "Прибрати записи про помилки та відсутні завдання? Файли не видаляються." :
                "Завершені завантаження залишаться у системі та на диску. Буде очищено лише список Nebula.")
            .setNegativeButton("Скасувати", null)
            .setPositiveButton("Очистити", (dialog, which) -> cleanup(errors)).show();
    }

    private void cleanup(boolean errors) {
        Set<Long> remaining = ids();
        int removed = 0;
        for (long id : new HashSet<>(remaining)) {
            int status = -1;
            try (Cursor cursor = manager.query(new DownloadManager.Query().setFilterById(id))) {
                if (cursor != null && cursor.moveToFirst())
                    status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            } catch (Exception ignored) { continue; } // Unknown error: keep the record.
            boolean eligible = errors ? (status == -1 || status == DownloadManager.STATUS_FAILED)
                : status == DownloadManager.STATUS_SUCCESSFUL;
            if (eligible) { remaining.remove(id); removed++; }
        }
        if (!persist(remaining)) {
            Toast.makeText(this, "Не вдалося зберегти зміни", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Прибрано записів: " + removed, Toast.LENGTH_SHORT).show();
        }
        refresh();
    }

    private void forget(long id) {
        Set<Long> stored = ids(); stored.remove(id);
        if (!persist(stored)) Toast.makeText(this, "Помилка збереження", Toast.LENGTH_SHORT).show();
        refresh();
    }
}
