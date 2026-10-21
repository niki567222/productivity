package dev.daylog;
import android.app.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;

public class EntryActivity extends Activity {
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        TrackerService.dismiss(this);
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true); }
        else { getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON); }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        long slot = getIntent().getLongExtra("slot", Alarms.prefs(this).getLong("pending", System.currentTimeMillis()));
        LinearLayout root = Ui.column(this);
        root.addView(Ui.text(this,"МОЙ ДЕНЬ · ЗАПИСЬ",14)); root.addView(Ui.text(this,"Чем ты занимаешься?",30));
        root.addView(Ui.text(this,"Пара слов о том, что ты делаешь. Запись останется на устройстве.",16));
        EditText input = new EditText(this); input.setId(R.id.entry_input); Ui.input(this,input); input.setHint("Например: ем, смотрю TikTok"); input.setGravity(Gravity.TOP); input.setMinLines(5); input.setInputType( android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        root.addView(input,new LinearLayout.LayoutParams(-1,0,1));
        TextView hint = Ui.text(this,"Минимум 5 символов",14); root.addView(hint);
        Button save = Ui.button(this,"Сохранить"); save.setVisibility(View.INVISIBLE); root.addView(save);
        input.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int count,int after) {} public void onTextChanged(CharSequence s,int st,int before,int count) {
                String value=s.toString().trim(); save.setVisibility(value.codePointCount(0,value.length())>=5 ? View.VISIBLE:View.INVISIBLE);
            } public void afterTextChanged(Editable e) {}
        });
        save.setOnClickListener(v -> {
            String value=input.getText().toString().trim(); if(value.codePointCount(0,value.length())<5)return;
            try (Store db = new Store(this)) { db.save(slot,value); }
            Alarms.prefs(this).edit().remove("pending").apply(); getSystemService(NotificationManager.class).cancel(1);
            TrackerService.dismiss(this); finish();
        });
        setContentView(root);
    }
}
