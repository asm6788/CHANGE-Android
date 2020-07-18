package com.asm6788.eagls;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.android.vending.expansion.zipfile.APKExpansionSupport;
import com.android.vending.expansion.zipfile.ZipResourceFile;

import java.io.FileInputStream;
import java.io.IOException;

import static com.asm6788.eagls.MainActivity.METADATA_CG;

public class CharacterPos extends AppCompatActivity {
    String[] _GRPNAMES = {"chaa", "chab", "chac", "chad", "chae", "chaf", "chag"};
    Point size = new Point();
    InGame inGame;
    Boolean DoNext = true;
    ZipResourceFile expansionFile = null;
    SharedPreferences.Editor editor;
    int index = 0;
    int touch_x = 0;
    int touch_y = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character_pos_setting);
        SharedPreferences sharedPref = getSharedPreferences("CharacterPos", Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        try {
            expansionFile = APKExpansionSupport.getAPKExpansionZipFile(getApplicationContext(), 1, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        View decorView = getWindow().getDecorView();

        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
        Display display = getWindowManager().getDefaultDisplay();
        display.getSize(size);

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            int temp = size.x;
            size.x = size.y;
            size.y = temp;
        }
        inGame = new InGame();
        SurfaceView ingame_view = findViewById(R.id.character_surface);
        ingame_view.getHolder().addCallback(new CharacterPos.Holder());

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                touch_x = (int) event.getX();
                touch_y = (int) event.getY();
                break;
        }
        return true;
    }


    public void Next(View v) {
        editor.putInt(_GRPNAMES[index - 1] + "x", touch_x);
        editor.putInt(_GRPNAMES[index - 1] + "y", touch_y);
        editor.commit();
        if (index == 7) {
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
            finish();
            return;
        }
        DoNext = true;
    }

    class Holder implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            inGame.start(holder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }

    protected class InGame implements Runnable {
        EAGLS.Charcter charcter = null;
        Paint paint;
        SurfaceHolder surfaceHolder;
        Thread thread;
        long GG_pak_next = 0;
        int finalWidth = 0;

        public void start(SurfaceHolder holder) {
            surfaceHolder = holder;
            thread = new Thread(this);
            thread.start();
        }

        int pak_rand() {
            GG_pak_next = GG_pak_next * 0xbc8f + (GG_pak_next / 0xadc8) * -0x7fffffff;
            if (GG_pak_next < 1) {
                GG_pak_next += 0x7fffffff;
            }
            double st0 = GG_pak_next * 0.00000000046566128752458 * 256;
            return (int) st0; //Fucking Float Point FUCK FUCKKKKK
        }

        byte[] Decode_CG_pak(String filename) {
            EAGLS.IDX who = null;
            for (EAGLS.IDX idx : METADATA_CG) {
                if (idx.filename.equals(filename)) {
                    who = idx;
                    break;
                }
            }
            byte[] DECODED_BUFFER = new byte[who.size];
            try {
                FileInputStream pak = (FileInputStream) expansionFile.getInputStream("CGPACK.pak");
                pak.skip(who.offset);
                pak.read(DECODED_BUFFER, 0, who.size);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String KEY = "EAGLS_SYSTEM";

            GG_pak_next = 0x75BD924 ^ (DECODED_BUFFER[DECODED_BUFFER.length - 1] & 0xff);
            for (int i = 0; i < 0x174b; i++) {
                DECODED_BUFFER[i] ^= (byte) KEY.charAt(pak_rand() % 12);
            }
            return LZSS.decompress_lzss(DECODED_BUFFER);
        }

        void Draw(Canvas canvas) {
            canvas.drawColor(Color.BLACK);
            try {
                if (charcter != null) {
                    if (charcter._CosSel_Bitmap != null) {
                        canvas.drawBitmap(charcter._CosSel_Bitmap, (size.x / 2) - (charcter._CosSel_Bitmap.getWidth() / 2), size.y - charcter._CosSel_Bitmap.getHeight(), paint);
                    }
                    if (charcter._Face_Bitmap != null) {
                        canvas.drawBitmap(charcter._Face_Bitmap, touch_x, touch_y, paint);
                    }
                }
            } catch (Exception e) {
            }

            surfaceHolder.unlockCanvasAndPost(canvas);
        }

        private Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
            if (maxHeight > 0 && maxWidth > 0) {
                int width = image.getWidth();
                int height = image.getHeight();
                float ratioBitmap = (float) width / (float) height;
                float ratioMax = (float) maxWidth / (float) maxHeight;

                finalWidth = maxWidth;
                int finalHeight = maxHeight;
                if (ratioMax > ratioBitmap) {
                    finalWidth = (int) ((float) maxHeight * ratioBitmap);
                } else {
                    finalHeight = (int) ((float) maxWidth / ratioBitmap);
                }
                image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
                return image;
            } else {
                return image;
            }
        }

        @Override
        public void run() {
            while (true) {
                if (surfaceHolder.getSurface().isValid()) {
                    if (DoNext) {
                        DoNext = false;
                        charcter = new EAGLS.Charcter(1, 1, 0, 0, _GRPNAMES[index], false, _GRPNAMES[index]);
                        byte[] RAW_byte = null;
                        RAW_byte = Decode_CG_pak(String.format("%s_b%02d", _GRPNAMES[index], charcter._CosSel) + ".gr");
                        BitmapFactory.Options opt = new BitmapFactory.Options();
                        opt.inMutable = true;
                        opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        Bitmap Temp_Bitmap = BitmapFactory.decodeByteArray(RAW_byte, 0, RAW_byte.length, opt);
                        Temp_Bitmap.setHasAlpha(true);
                        Temp_Bitmap = EAGLS.Charcter.Alpha_bmp(Temp_Bitmap, RAW_byte);
                        charcter._CosSel_Bitmap = resize(Temp_Bitmap, size.x, size.y);
                        float ratio = (float) charcter._CosSel_Bitmap.getHeight() / Temp_Bitmap.getHeight();
                        RAW_byte = Decode_CG_pak(String.format("%s_f%02d", _GRPNAMES[index], charcter._Face) + ".gr");
                        Temp_Bitmap = BitmapFactory.decodeByteArray(RAW_byte, 0, RAW_byte.length, opt);
                        Temp_Bitmap.setHasAlpha(true);
                        Temp_Bitmap = EAGLS.Charcter.Alpha_bmp(Temp_Bitmap, RAW_byte);
                        charcter._Face_Bitmap = resize(Temp_Bitmap, (int) (Temp_Bitmap.getWidth() * ratio), (int) (Temp_Bitmap.getHeight() * ratio));
                        index++;
                    }
                    Canvas canvas = surfaceHolder.lockCanvas();
                    if (canvas != null)
                        Draw(canvas);
                }
            }
        }
    }
}
