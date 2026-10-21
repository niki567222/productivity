package dev.daylog;

import android.app.*;
import android.content.*;
import android.os.Build;

final class Alarms {
    static int minutes(Context c) { return prefs(c).getInt("minutes", 15); }
    static int mode(Context c) { return prefs(c).getInt("mode", 2); }
    static String channelId(Context c) { return "checkin_v2_" + mode(c); }
    static android.content.SharedPreferences prefs(Context c) { return c.getSharedPreferences("alarm", Context.MODE_PRIVATE); }
    static PendingIntent pending(Context c) { return PendingIntent.getBroadcast(c, 1, new Intent(c, AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE); }
    static boolean allowed(Context c) { return Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager.class).canScheduleExactAlarms(); }
    static void schedule(Context c) {
        if (!prefs(c).getBoolean("enabled", false) || !allowed(c)) return;
        long next = System.currentTimeMillis() + minutes(c) * 60 * 1000L;
        PendingIntent show = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // Alarm-clock scheduling avoids the idle quota that delays 5-minute reminders.
        c.getSystemService(AlarmManager.class).setAlarmClock(new AlarmManager.AlarmClockInfo(next, show), pending(c));
        prefs(c).edit().putLong("next", next).apply();
    }
    static void ensureScheduled(Context c) {
        if (prefs(c).getLong("next", 0) <= System.currentTimeMillis()) schedule(c);
    }
    static void stop(Context c) {
        prefs(c).edit().putBoolean("enabled", false).remove("pending").remove("next").apply();
        c.getSystemService(AlarmManager.class).cancel(pending(c));
        c.getSystemService(NotificationManager.class).cancel(1);
        c.stopService(new Intent(c, TrackerService.class));
    }
    static void channel(Context c) {
        String[] names = {"Без звука и вибрации", "Только вибрация", "Колокольчик и вибрация"};
        int mode = mode(c);
        NotificationChannel ch = new NotificationChannel(channelId(c), names[mode], NotificationManager.IMPORTANCE_HIGH);
        android.net.Uri sound = android.net.Uri.parse("android.resource://" + c.getPackageName() + "/raw/" + c.getResources().getResourceEntryName(R.raw.bells));
        ch.setSound(mode == 2 ? sound : null, new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ALARM).build());
        ch.enableVibration(mode != 0);
        if (mode != 0) ch.setVibrationPattern(new long[]{0, 400, 200, 400, 200, 600});
        c.getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }
}
