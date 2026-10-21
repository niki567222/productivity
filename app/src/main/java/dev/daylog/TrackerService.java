package dev.daylog;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;

/** User-visible ongoing reminder session. The OS alarm remains the scheduling authority. */
public class TrackerService extends Service {
    static final String PROMPT="dev.daylog.PROMPT", DISMISS="dev.daylog.DISMISS", STOP="dev.daylog.STOP";
    private View overlay;
    private long overlaySlot;
    static void dismiss(Context c) {
        if (Alarms.prefs(c).getBoolean("enabled",false)) c.startService(new Intent(c,TrackerService.class).setAction(DISMISS));
    }
    static void start(Context c) { launch(c,null); }
    static boolean launch(Context c,String action) {
        try { c.startForegroundService(new Intent(c,TrackerService.class).setAction(action)); return true; }
        catch (IllegalStateException | SecurityException e) {
            // Restricted OEM/background launches may reject a service. Scheduled OS alarms remain.
            android.util.Log.w("DayLog","Background service launch restricted; OS alarm retained",e);
            return false;
        }
    }
    public int onStartCommand(Intent intent,int flags,int startId) {
        if(intent!=null && DISMISS.equals(intent.getAction())) hideOverlay();
        if(intent!=null && STOP.equals(intent.getAction())) { Alarms.stop(this); stopSelf(); return START_NOT_STICKY; }
        if(!Alarms.prefs(this).getBoolean("enabled",false)) { stopSelf(); return START_NOT_STICKY; }
        NotificationChannel channel=new NotificationChannel("session","Фоновые напоминания",NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null,null); channel.enableVibration(false);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,2,new Intent(this,TrackerService.class).setAction(STOP),PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n=new Notification.Builder(this,"session").setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Мой день · напоминания включены").setContentText("Интервал: "+Alarms.minutes(this)+" мин. Нажми, чтобы настроить.")
            .setContentIntent(open).setOngoing(true).setCategory(Notification.CATEGORY_SERVICE)
            .addAction(new Notification.Action.Builder(null,"Выключить",stop).build()).build();
        if(Build.VERSION.SDK_INT>=34) startForeground(2,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(2,n);
        Alarms.ensureScheduled(this);
        if(intent!=null && PROMPT.equals(intent.getAction())) showOverlay();
        return START_STICKY;
    }
    private void showOverlay() {
        if(!Settings.canDrawOverlays(this) || getSystemService(KeyguardManager.class).isKeyguardLocked())return;
        long slot=Alarms.prefs(this).getLong("pending",0);
        if(slot==0 || (overlay!=null && overlaySlot==slot))return;
        hideOverlay(); overlaySlot=slot;
        LinearLayout root=new Ui.NightLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        int pad=Ui.dp(this,24); root.setPadding(pad,pad,pad,pad);
        root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(pad+i.getSystemWindowInsetLeft(),pad+i.getSystemWindowInsetTop(),pad+i.getSystemWindowInsetRight(),pad+i.getSystemWindowInsetBottom());return i;});
        root.addView(Ui.text(this,"МОЙ ДЕНЬ · ЗАПИСЬ",14));
        root.addView(Ui.text(this,"Чем ты занимаешься?",30));
        root.addView(Ui.text(this,"Запиши момент. История останется на устройстве.",16));
        EditText input=new EditText(this); Ui.input(this,input); input.setHint("Например: ем, смотрю TikTok");
        input.setGravity(Gravity.TOP); input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        root.addView(input,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(Ui.text(this,"Минимум 5 символов",14));
        Button save=Ui.button(this,"Сохранить"); save.setVisibility(View.INVISIBLE); root.addView(save);
        input.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int count,int after) {}
            public void onTextChanged(CharSequence s,int st,int before,int count) {
                String value=s.toString().trim(); save.setVisibility(value.codePointCount(0,value.length())>=5?View.VISIBLE:View.INVISIBLE);
            }
            public void afterTextChanged(Editable e) {}
        });
        save.setOnClickListener(v->{
            String body=input.getText().toString().trim(); if(body.codePointCount(0,body.length())<5)return;
            try(Store db=new Store(this)){db.save(slot,body);}
            Alarms.prefs(this).edit().remove("pending").apply(); getSystemService(NotificationManager.class).cancel(1); hideOverlay();
        });
        Button later=Ui.button(this,"Свернуть"); root.addView(later); later.setOnClickListener(v->hideOverlay());
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,PixelFormat.OPAQUE);
        p.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        try { getSystemService(WindowManager.class).addView(root,p); overlay=root; }
        catch(SecurityException | WindowManager.BadTokenException e) { android.util.Log.w("DayLog","Overlay unavailable; notification retained",e); }
    }
    private void hideOverlay() {
        if(overlay!=null) { getSystemService(WindowManager.class).removeView(overlay); overlay=null; }
    }
    public void onTaskRemoved(Intent rootIntent) { Alarms.ensureScheduled(this); super.onTaskRemoved(rootIntent); }
    public void onDestroy() { hideOverlay(); super.onDestroy(); }
    public IBinder onBind(Intent intent) { return null; }
}
