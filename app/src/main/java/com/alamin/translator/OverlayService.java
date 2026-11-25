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
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);

        TextView t=new TextView(this);
        t.setText("A");
        t.setTextSize(20);
        t.setTextColor(Color.BLACK);
        t.setTypeface(null,Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setBackgroundColor(Color.rgb(212,175,55));
        bubble=t;

        WindowManager.LayoutParams p=new WindowManager.LayoutParams(
            58,58,
            Build.VERSION.SDK_INT>=26 ?
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
            WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);

        p.gravity=Gravity.TOP|Gravity.RIGHT;
        p.x=25;
        p.y=250;

        t.setOnTouchListener(new View.OnTouchListener() {
            int sx,sy;
            float tx,ty;
            boolean moved;

            public boolean onTouch(View v,MotionEvent e) {
                if(e.getAction()==MotionEvent.ACTION_DOWN) {
                    sx=p.x; sy=p.y;
                    tx=e.getRawX(); ty=e.getRawY();
                    moved=false;
                    return true;
                }
                if(e.getAction()==MotionEvent.ACTION_MOVE) {
                    if(Math.abs(e.getRawX()-tx)>8 ||
                       Math.abs(e.getRawY()-ty)>8) moved=true;
                    p.x=sx+(int)(tx-e.getRawX());
                    p.y=sy+(int)(e.getRawY()-ty);
                    wm.updateViewLayout(bubble,p);
                    return true;
                }
                if(e.getAction()==MotionEvent.ACTION_UP) {
                    if(!moved) {
                        Intent i=new Intent(
                            OverlayService.this,CaptureService.class);
                        i.setAction("CAPTURE_NOW");
                        if(Build.VERSION.SDK_INT>=26)
                            startForegroundService(i);
                        else startService(i);
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
        if(bubble!=null) {
            try { wm.removeView(bubble); }
            catch(Exception ignored) {}
        }
    }

    @Override
    public IBinder onBind(Intent i) { return null; }
}
