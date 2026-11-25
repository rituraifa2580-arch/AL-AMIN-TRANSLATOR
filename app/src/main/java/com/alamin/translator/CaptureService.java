package com.alamin.translator;
import com.google.mlkit.common.model.DownloadConditions;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.media.projection.*;
import android.hardware.display.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.nio.ByteBuffer;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.nl.languageid.*;
import com.google.mlkit.nl.translate.*;

public class CaptureService extends Service {
    MediaProjection projection;
    VirtualDisplay display;
    ImageReader reader;
    Handler handler;
    WindowManager wm;
    int width,height,density;
    boolean capturePending=false;
    View resultPanel;

    @Override
    public void onCreate() {
        super.onCreate();
        handler=new Handler(Looper.getMainLooper());
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);

        String channel="translator_channel";
        if(Build.VERSION.SDK_INT>=26) {
            NotificationChannel c=new NotificationChannel(
                channel,"Screen Translator",
                NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class)
                .createNotificationChannel(c);
        }

        Notification n;
        if(Build.VERSION.SDK_INT>=26) {
            n=new Notification.Builder(this,channel)
                .setContentTitle("AL-AMIN TRANSLATOR")
                .setContentText("Screen translator active")
                .setSmallIcon(android.R.drawable.ic_menu_search)
                .build();
        } else {
            n=new Notification.Builder(this)
                .setContentTitle("AL-AMIN TRANSLATOR")
                .setSmallIcon(android.R.drawable.ic_menu_search)
                .build();
        }

        if(Build.VERSION.SDK_INT>=29)
            startForeground(45,n,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        else startForeground(45,n);
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int id) {
        if(intent!=null && "CAPTURE_NOW".equals(intent.getAction())) {
            if(projection==null) {
                show("Screen capture permission not enabled.");
            } else {
                capturePending=true;
                handler.postDelayed(() -> {
                    if(capturePending) show("No screen image received. Try again.");
                    capturePending=false;
                },3000);
            }
            return START_STICKY;
        }

        if(projection==null && intent!=null) {
            int code=intent.getIntExtra("resultCode",0);
            Intent data=intent.getParcelableExtra("data");
            if(data!=null) startProjection(code,data);
        }
        return START_STICKY;
    }

    void startProjection(int code,Intent data) {
        try {
            MediaProjectionManager m=(MediaProjectionManager)
                getSystemService(MEDIA_PROJECTION_SERVICE);
            projection=m.getMediaProjection(code,data);

            projection.registerCallback(new MediaProjection.Callback() {
                @Override public void onStop() { stopSelf(); }
            },handler);

            android.util.DisplayMetrics dm=getResources().getDisplayMetrics();
            width=dm.widthPixels;
            height=dm.heightPixels;
            density=dm.densityDpi;

            reader=ImageReader.newInstance(
                width,height,PixelFormat.RGBA_8888,2);

            reader.setOnImageAvailableListener(r -> {
                Image image=r.acquireLatestImage();
                if(image==null) return;

                if(!capturePending) {
                    image.close();
                    return;
                }
                capturePending=false;

                Bitmap bitmap=toBitmap(image);
                image.close();

                if(bitmap!=null) readText(bitmap);
                else show("Could not capture screen.");
            },handler);

            display=projection.createVirtualDisplay(
                "AL-AMIN-SCREEN",width,height,density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.getSurface(),null,handler);

        } catch(Exception e) {
            show("Capture error: "+e.getMessage());
        }
    }

    Bitmap toBitmap(Image image) {
        try {
            Image.Plane plane=image.getPlanes()[0];
            ByteBuffer buffer=plane.getBuffer();
            int pixel=plane.getPixelStride();
            int row=plane.getRowStride();
            Bitmap full=Bitmap.createBitmap(
                row/pixel,height,Bitmap.Config.ARGB_8888);
            full.copyPixelsFromBuffer(buffer);
            Bitmap result=Bitmap.createBitmap(full,0,0,width,height);
            full.recycle();
            return result;
        } catch(Exception e) {
            return null;
        }
    }

    void readText(Bitmap bitmap) {
        TextRecognizer recognizer=TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(InputImage.fromBitmap(bitmap,0))
            .addOnSuccessListener(result -> {
                bitmap.recycle();
                recognizer.close();
                String source=result.getText().trim();
                if(source.isEmpty()) {
                    show("No readable text found.");
                } else translate(source);
            })
            .addOnFailureListener(e -> {
                bitmap.recycle();
                recognizer.close();
                show("OCR error: "+e.getMessage());
            });
    }

    void translate(String source) {
        LanguageIdentifier id=LanguageIdentification.getClient();
        id.identifyLanguage(source).addOnSuccessListener(lang -> {
            id.close();

            if("und".equals(lang)) {
                show("Language not identified.\n"+source);
                return;
            }
            if("bn".equals(lang)) {
                show(source);
                return;
            }

            String from=TranslateLanguage.fromLanguageTag(lang);
            if(from==null) {
                show("Unsupported language.\n"+source);
                return;
            }

            Translator translator=Translation.getClient(
                new TranslatorOptions.Builder()
                    .setSourceLanguage(from)
                    .setTargetLanguage(TranslateLanguage.BENGALI)
                    .build());

            translator.downloadModelIfNeeded(
                new DownloadConditions.Builder().build())
                .addOnSuccessListener(v ->
                    translator.translate(source)
                    .addOnSuccessListener(out -> {
                        translator.close();
                        show(out);
                    })
                    .addOnFailureListener(e -> {
                        translator.close();
                        show("Translation error: "+e.getMessage());
                    }))
                .addOnFailureListener(e -> {
                    translator.close();
                    show("Model download failed. Internet needed.");
                });
        }).addOnFailureListener(e -> {
            id.close();
            show("Language detection failed.");
        });
    }

    void show(String message) {
        handler.post(() -> {
            try {
                if(resultPanel!=null) wm.removeView(resultPanel);
            } catch(Exception ignored) {}

            LinearLayout box=new LinearLayout(this);
            box.setOrientation(1);
            box.setPadding(18,18,18,18);
            box.setBackgroundColor(Color.rgb(18,18,18));

            TextView title=new TextView(this);
            title.setText("AL-AMIN • বাংলা অনুবাদ");
            title.setTextColor(Color.rgb(212,175,55));
            title.setTextSize(18);
            box.addView(title);

            ScrollView scroll=new ScrollView(this);
            TextView body=new TextView(this);
            body.setText(message);
            body.setTextColor(Color.WHITE);
            body.setTextSize(17);
            body.setPadding(5,15,5,15);
            scroll.addView(body);
            box.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

            Button close=new Button(this);
            close.setText("CLOSE");
            box.addView(close);

            WindowManager.LayoutParams p=new WindowManager.LayoutParams(
                (int)(width*0.88),(int)(height*0.55),
                Build.VERSION.SDK_INT>=26 ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
            p.gravity=Gravity.CENTER;

            resultPanel=box;
            try {
                wm.addView(box,p);
            } catch(Exception e) {
                resultPanel=null;
            }

            close.setOnClickListener(v -> {
                try { wm.removeView(box); } catch(Exception ignored) {}
                resultPanel=null;
            });
        });
    }

    @Override
    public void onDestroy() {
        if(display!=null) display.release();
        if(reader!=null) reader.close();
        if(projection!=null) projection.stop();
        try {
            if(resultPanel!=null) wm.removeView(resultPanel);
        } catch(Exception ignored) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) { return null; }
}
