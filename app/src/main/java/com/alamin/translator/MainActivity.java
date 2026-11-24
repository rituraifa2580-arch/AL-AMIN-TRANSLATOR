package com.alamin.translator;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {

    LinearLayout root;

    int gold = Color.rgb(212,175,55);

    TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(gold);
        t.setTextSize(size);
        t.setTypeface(null, Typeface.BOLD);
        t.setPadding(12,16,12,16);
        return t;
    }

    Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(Color.BLACK);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold));
        return b;
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(24,35,24,24);
        root.setBackgroundColor(Color.rgb(8,8,8));

        root.addView(text("AL-AMIN TRANSLATOR",27));
        root.addView(text("FLOATING SCREEN TRANSLATOR",14));
        root.addView(text("Translate screen text into Bangla",16));

        Button start = button("START FLOATING BUBBLE");
        root.addView(start);

        Button stop = button("STOP FLOATING BUBBLE");
        root.addView(stop);

        root.addView(text("HOW TO USE",20));
        root.addView(text(
            "1. Start the floating bubble.\\n" +
            "2. Open any app.\\n" +
            "3. Tap the bubble to open translator.\\n" +
            "4. Screen capture and OCR features are coming next.",
            15));

        start.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent i = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } else {
                startService(new Intent(this, OverlayService.class));
                Toast.makeText(this,"Floating bubble started",Toast.LENGTH_SHORT).show();
            }
        });

        stop.setOnClickListener(v -> {
            stopService(new Intent(this, OverlayService.class));
            Toast.makeText(this,"Bubble stopped",Toast.LENGTH_SHORT).show();
        });

        setContentView(root);
    }
}
