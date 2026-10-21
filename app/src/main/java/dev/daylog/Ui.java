package dev.daylog;
import android.app.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
final class Ui {
    static LinearLayout column(Activity a) {
        LinearLayout l = new LinearLayout(a); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(28, 40, 28, 28); l.setBackgroundColor(Color.rgb(246,247,241));
        l.setOnApplyWindowInsetsListener((v, insets) -> { v.setPadding(28 + insets.getSystemWindowInsetLeft(), 24 + insets.getSystemWindowInsetTop(), 28 + insets.getSystemWindowInsetRight(), 24 + insets.getSystemWindowInsetBottom()); return insets; }); return l;
    }
    static TextView text(Activity a, String s, int size) { TextView t = new TextView(a); t.setText(s); t.setTextSize(size); t.setTextColor(Color.rgb(31,55,47)); t.setPadding(0,12,0,12); return t; }
    static Button button(Activity a, String s) { Button b = new Button(a); b.setText(s); return b; }
}
