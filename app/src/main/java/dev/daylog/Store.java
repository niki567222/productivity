package dev.daylog;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

final class Store extends SQLiteOpenHelper {
    Store(Context c) { super(c, "daylog.db", null, 1); }
    public void onCreate(SQLiteDatabase db) { db.execSQL("CREATE TABLE entries(slot INTEGER PRIMARY KEY, day TEXT NOT NULL, time TEXT NOT NULL, body TEXT NOT NULL)"); }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
    void save(long slot, String body) {
        ZonedDateTime date = Instant.ofEpochMilli(slot).atZone(ZoneId.systemDefault());
        ContentValues values = new ContentValues();
        values.put("slot", slot); values.put("day", date.toLocalDate().toString());
        values.put("time", date.format(DateTimeFormatter.ofPattern("HH:mm"))); values.put("body", body);
        getWritableDatabase().insertWithOnConflict("entries", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
    String export() {
        StringBuilder text = new StringBuilder(); String lastDay = "";
        try (Cursor c = getReadableDatabase().rawQuery("SELECT day,time,body FROM entries ORDER BY slot", null)) {
            while (c.moveToNext()) {
                String day = c.getString(0);
                if (!day.equals(lastDay)) { if (text.length() > 0) text.append('\n'); text.append(day).append('\n'); lastDay = day; }
                text.append(c.getString(1)).append(" — ").append(c.getString(2)).append('\n');
            }
        }
        return text.toString();
    }
}
