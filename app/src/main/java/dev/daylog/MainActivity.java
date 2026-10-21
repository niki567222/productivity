package dev.daylog;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    TextView status;
    protected void onCreate(Bundle state) { super.onCreate(state); Alarms.channel(this); }
    protected void onResume() {
        super.onResume();
        if (Alarms.prefs(this).getBoolean("enabled",false)) { Alarms.ensureScheduled(this); TrackerService.start(this); }
        render();
    }
    boolean notificationsAllowed() { return getSystemService(NotificationManager.class).areNotificationsEnabled(); }
    void render() {
        LinearLayout root=Ui.column(this);
        root.addView(Ui.text(this,"ЛИЧНЫЙ ТРЕКЕР",14)); root.addView(Ui.text(this,"Мой день",34));
        root.addView(Ui.text(this,"Остановись на минуту. Заметь, куда уходит время.",17));
        boolean enabled=Alarms.prefs(this).getBoolean("enabled",false);
        status=Ui.text(this,enabled ? "● Напоминания включены · каждые " + Alarms.minutes(this) + " мин.":"○ Напоминания выключены",17); root.addView(status);
        root.addView(Ui.text(this,"ИНТЕРВАЛ ПРОВЕРКИ",13));
        int[] intervals = {5, 10, 15};
        int selected = Alarms.minutes(this)==5 ? 0 : Alarms.minutes(this)==10 ? 1 : 2;
        root.addView(Ui.choices(this,new String[]{"5 минут","10 минут","15 минут"},selected,index->{
            Alarms.prefs(this).edit().putInt("minutes",intervals[index]).apply();
            if(enabled) { Alarms.schedule(this); TrackerService.start(this); } render();
        }));
        root.addView(Ui.text(this,"РЕЖИМ СИГНАЛА",13));
        root.addView(Ui.choices(this,new String[]{"Тихо\nбез вибрации", "Вибрация", "Вибрация\n+ звук"},Alarms.mode(this),index->{
            Alarms.prefs(this).edit().putInt("mode",index).apply(); Alarms.channel(this); render();
        }));
        Button toggle=Ui.button(this,enabled?"Выключить":"Включить напоминания"); root.addView(toggle);
        toggle.setOnClickListener(v->{
            if(enabled) { Alarms.stop(this); render(); return; }
            if(!notificationsAllowed()) {
                if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},1);
                else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
                return;
            }
            if(!Alarms.allowed(this)) { startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName()))); return; }
            Alarms.prefs(this).edit().putBoolean("enabled",true).apply(); Alarms.schedule(this); TrackerService.start(this); render();
        });
        if(Build.VERSION.SDK_INT>=34 && !getSystemService(NotificationManager.class).canUseFullScreenIntent()) {
            Button permission=Ui.button(this,"Разрешить полноэкранные сигналы"); root.addView(permission);
            permission.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:"+getPackageName()))));
        }
        root.addView(Ui.text(this,"РАБОТА В ФОНЕ",13));
        boolean overlay=Settings.canDrawOverlays(this);
        Button overlayButton=Ui.button(this,overlay?"✓ Форма поверх приложений разрешена":"Разрешить форму поверх приложений"); root.addView(overlayButton);
        overlayButton.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
        boolean unrestricted=getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName());
        Button battery=Ui.button(this,unrestricted?"✓ Ограничения батареи сняты":"Снять ограничения батареи"); root.addView(battery);
        battery.setOnClickListener(v->{
            Intent settings=new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            Toast.makeText(this,"Выбери «Все приложения», найди «Мой день» и отключи оптимизацию",Toast.LENGTH_LONG).show();
            startActivity(settings);
        });
        root.addView(Ui.text(this,"Для формы на экране блокировки разреши полноэкранные сигналы. Для формы поверх открытых приложений — показ поверх окон. Смахивание из недавних не выключает напоминания. На некоторых телефонах также нужно разрешить автозапуск в настройках производителя.",13));
        Button manual=Ui.button(this,"Записать сейчас"); root.addView(manual);
        manual.setOnClickListener(v->startActivity(new Intent(this,EntryActivity.class)));
        Button export=Ui.button(this,"Экспортировать всё в .txt"); root.addView(export);
        export.setOnClickListener(v->startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain").putExtra(Intent.EXTRA_TITLE,"my-days.txt"),2));
        root.addView(Ui.text(this,"История по дням",23));
        ScrollView scroll=new ScrollView(this); String history; try(Store db=new Store(this)){history=db.export();}
        TextView entries=Ui.text(this,history.isEmpty()?"Здесь появится твоя первая запись.":history,16);
        entries.setTextColor(Ui.WHITE); entries.setTextIsSelectable(true); entries.setPadding(Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16)); entries.setBackground(Ui.panel(0x99111c3b,this)); root.addView(entries);
        scroll.setFillViewport(true); scroll.addView(root); setContentView(scroll);
    }
    protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==2 && result==RESULT_OK && data!=null && data.getData()!=null) {
            try(Store db=new Store(this); java.io.OutputStream out=getContentResolver().openOutputStream(data.getData())) {
                if(out==null) throw new java.io.IOException(); out.write(db.export().getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this,"Экспорт сохранён",Toast.LENGTH_SHORT).show();
            } catch(Exception e) { Toast.makeText(this,"Не удалось сохранить файл. Выбери другое место.",Toast.LENGTH_LONG).show(); }
        }
    }
}
