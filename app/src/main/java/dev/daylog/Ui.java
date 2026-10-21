package dev.daylog;
import android.app.*;
import android.animation.ValueAnimator;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

final class Ui {
    static final int WHITE = Color.rgb(235,240,255), MUTED = Color.rgb(159,177,215);
    static int dp(Activity a, int n) { return (int)(n * a.getResources().getDisplayMetrics().density); }
    static class NightLayout extends LinearLayout {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final ValueAnimator animator = ValueAnimator.ofFloat(0, (float)(Math.PI * 2));
        float phase;
        Shader base, glow, lower;
        final Matrix matrix = new Matrix();
        public NightLayout(android.content.Context a) {
            super(a); setWillNotDraw(false);
            animator.setDuration(18000); animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new android.view.animation.LinearInterpolator());
            animator.addUpdateListener(v -> { phase = (float)v.getAnimatedValue(); invalidate(); });
        }
        protected void onAttachedToWindow() { super.onAttachedToWindow(); animator.start(); }
        protected void onDetachedFromWindow() { animator.cancel(); super.onDetachedFromWindow(); }
        protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);
            if (animator != null && animator.isStarted()) { if (visibility == VISIBLE) animator.resume(); else animator.pause(); }
        }
        protected void onSizeChanged(int w,int h,int oldw,int oldh) {
            super.onSizeChanged(w,h,oldw,oldh);
            base=new LinearGradient(0,0,Math.max(w,1),Math.max(h,1),new int[]{0xff070c23,0xff101e49,0xff080d25},null,Shader.TileMode.CLAMP);
            glow=new RadialGradient(0,0,Math.max(w,1),new int[]{0x804451a8,0x00233177},null,Shader.TileMode.CLAMP);
            lower=new RadialGradient(0,0,Math.max(w,1),new int[]{0x603175a8,0x00102040},null,Shader.TileMode.CLAMP);
        }
        protected void onDraw(Canvas c) {
            float w=getWidth(), h=getHeight();
            if(base==null)return;
            paint.setShader(base); c.drawRect(0,0,w,h,paint);
            float x=w*(0.5f+0.45f*(float)Math.sin(phase));
            matrix.setTranslate(x,h*0.25f); glow.setLocalMatrix(matrix);
            paint.setShader(glow); c.drawRect(0,0,w,h,paint);
            matrix.setTranslate(w-x,h*0.72f); lower.setLocalMatrix(matrix);
            paint.setShader(lower); c.drawRect(0,0,w,h,paint);
            paint.setShader(null); super.onDraw(c);
        }
    }
    static LinearLayout column(Activity a) {
        a.getWindow().setStatusBarColor(0xff080e27); a.getWindow().setNavigationBarColor(0xff080e27);
        a.getWindow().getDecorView().setSystemUiVisibility(0);
        LinearLayout l = new NightLayout(a); l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(a,24),dp(a,24),dp(a,24),dp(a,24));
        l.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(dp(a,24)+i.getSystemWindowInsetLeft(),dp(a,24)+i.getSystemWindowInsetTop(),dp(a,24)+i.getSystemWindowInsetRight(),dp(a,24)+i.getSystemWindowInsetBottom());return i;}); return l;
    }
    static GradientDrawable panel(int color, Activity a) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(a,18)); d.setStroke(dp(a,1),0x304e79b4); return d;
    }
    static TextView text(Activity a, String s, int size) {
        TextView t=new TextView(a); t.setText(s); t.setTextSize(size); t.setTextColor(size<=16?MUTED:WHITE); t.setPadding(0,dp(a,8),0,dp(a,8));
        if(size>=23)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD); return t;
    }
    static Button button(Activity a, String s) {
        Button b=new Button(a); b.setText(s); b.setTextSize(15); b.setAllCaps(false); b.setTextColor(WHITE);
        b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x445a9fff),panel(0xcc283e78,a),null));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(a,52)); p.setMargins(0,dp(a,6),0,dp(a,6)); b.setLayoutParams(p); return b;
    }
    static void input(Activity a, EditText e) {
        e.setTextColor(WHITE); e.setHintTextColor(MUTED); e.setTextSize(18); e.setPadding(dp(a,18),dp(a,18),dp(a,18),dp(a,18)); e.setBackground(panel(0xb3111d3d,a));
    }
    static LinearLayout choices(Activity a, String[] names, int selected, java.util.function.IntConsumer onSelect) {
        LinearLayout row=new LinearLayout(a);
        for(int i=0;i<names.length;i++) {
            final int index=i; Button b=button(a,names[i]); b.setTextSize(13);
            b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x445a9fff),panel(i==selected?0xff4d61b6:0x99111c3b,a),null));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(a,64),1);p.setMargins(dp(a,3),dp(a,4),dp(a,3),dp(a,8));row.addView(b,p);
            b.setOnClickListener(v->onSelect.accept(index));
        }
        return row;
    }
}
