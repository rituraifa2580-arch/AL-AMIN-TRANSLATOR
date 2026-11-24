package com.alamin.translator;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;

public class OverlayService extends Service {

    WindowManager wm;
    View bubble;

    @Override
    public void onCreate() {
        super.onCreate();

        wm = (WindowManager)getSystemService(WINDOW_SERVICE);

        TextView t = new TextView(this);
        t.setText("A");
        t.setTextSize(20);
        t.setTextColor(Color.BLACK);
        t.setTypeface(null,Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setBackgroundColor(Color.rgb(212,175,55));
        bubble = t;

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            58,58,
            Build.VERSION.SDK_INT >= 26 ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );

        p.gravity = Gravity.TOP | Gravity.RIGHT;
        p.x = 25;
        p.y = 250;

        t.setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });

        t.setOnTouchListener(new View.OnTouchListener() {
            int sx,sy;
            float tx,ty;

            public boolean onTouch(View v, android.view.MotionEvent e) {
                if(e.getAction()==MotionEvent.ACTION_DOWN) {
                    sx=p.x; sy=p.y;
                    tx=e.getRawX(); ty=e.getRawY();
                    return true;
                }
                if(e.getAction()==MotionEvent.ACTION_MOVE) {
                    p.x=sx+(int)(tx-e.getRawX());
                    p.y=sy+(int)(e.getRawY()-ty);
                    wm.updateViewLayout(bubble,p);
                    return true;
                }
                if(e.getAction()==MotionEvent.ACTION_UP) {
                    if(Math.abs(e.getRawX()-tx)<10 &&
                       Math.abs(e.getRawY()-ty)<10) {
                        Intent i=new Intent(OverlayService.this,MainActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    }
                    return true;
                }
                return true;
            }
        });

        wm.addView(bubble,p);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(bubble!=null) wm.removeView(bubble);
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }
}
