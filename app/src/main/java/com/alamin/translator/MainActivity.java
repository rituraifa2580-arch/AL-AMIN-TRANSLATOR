package com.alamin.translator;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.Settings;
import android.media.projection.MediaProjectionManager;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    static final int CAPTURE_REQUEST=7001;
    int gold=Color.rgb(212,175,55);

    TextView text(String s,int size) {
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextColor(gold);
        t.setTextSize(size);
        t.setTypeface(null,Typeface.BOLD);
        t.setPadding(12,16,12,16);
        return t;
    }

    Button button(String s) {
        Button b=new Button(this);
        b.setText(s);
        b.setTextColor(Color.BLACK);
        b.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(gold));
        return b;
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(24,35,24,24);
        root.setBackgroundColor(Color.rgb(8,8,8));

        root.addView(text("AL-AMIN TRANSLATOR",27));
        root.addView(text("FLOATING SCREEN TRANSLATOR",15));
        root.addView(text("Translate screen text into Bangla",16));

        Button start=button("START TRANSLATOR");
        Button stop=button("STOP TRANSLATOR");
        root.addView(start);
        root.addView(stop);

        root.addView(text("HOW TO USE",20));
        root.addView(text(
            "1. Start translator and allow permissions.\n"+
            "2. Open another app.\n"+
            "3. Move A bubble over text.\n"+
            "4. Tap bubble to translate into Bangla.",15));

        start.setOnClickListener(v -> {
            if(!Settings.canDrawOverlays(this)) {
                Intent i=new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:"+getPackageName()));
                startActivity(i);
                return;
            }

            MediaProjectionManager m=(MediaProjectionManager)
                getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(
                m.createScreenCaptureIntent(),CAPTURE_REQUEST);
        });

        stop.setOnClickListener(v -> {
            stopService(new Intent(this,CaptureService.class));
            stopService(new Intent(this,OverlayService.class));
            Toast.makeText(this,"Translator stopped",
                Toast.LENGTH_SHORT).show();
        });

        setContentView(root);
    }

    @Override
    protected void onActivityResult(int requestCode,
        int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode==CAPTURE_REQUEST &&
            resultCode==RESULT_OK && data!=null) {

            Intent capture=new Intent(this,CaptureService.class);
            capture.putExtra("resultCode",resultCode);
            capture.putExtra("data",data);

            if(Build.VERSION.SDK_INT>=26)
                startForegroundService(capture);
            else startService(capture);

            startService(new Intent(this,OverlayService.class));

            Toast.makeText(this,"Translator ready!",
                Toast.LENGTH_LONG).show();
        }
    }
}
