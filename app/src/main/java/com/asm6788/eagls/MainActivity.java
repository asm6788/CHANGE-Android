package com.asm6788.eagls;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.vending.expansion.zipfile.APKExpansionSupport;
import com.android.vending.expansion.zipfile.ZipResourceFile;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class MainActivity extends Activity {

    static List<EAGLS.IDX> METADATA_Script = null;
    static List<EAGLS.IDX> METADATA_CG = null;
    static List<EAGLS.IDX> METADATA_Wave = null;
    static int finalWidth;
    static InGame inGame;
    static VideoPlayer video;
    static Point size = new Point();
    static boolean BlockTouch = false;
    ZipResourceFile expansionFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        try {
            expansionFile = APKExpansionSupport.getAPKExpansionZipFile(getApplicationContext(), 1, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
            }

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    101);
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            }

            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    101);
        }
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
        METADATA_Script = Decode_idx("SCPACK.idx");
        METADATA_CG = Decode_idx("CGPACK.idx");
        METADATA_Wave = Decode_idx("WAVEPACK.idx");

        final ListView listview = findViewById(R.id.Script_list);
        ArrayAdapter<EAGLS.IDX> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, METADATA_Script);

        inGame = new InGame();
        final SurfaceView ingame_view = findViewById(R.id.InGame_view);
        ingame_view.getHolder().addCallback(new Holder());
        SurfaceView video_view = findViewById(R.id.Video_view);
        video_view.getHolder().addCallback(new Holder());
        video = new VideoPlayer();

        listview.setAdapter(adapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                inGame.Script = ((EAGLS.IDX) adapterView.getItemAtPosition(position)).filename;
                listview.setVisibility(View.GONE);
                inGame.start(ingame_view.getHolder());
            }
        });
    }

    @Override
    public void onBackPressed() {
        inGame.stop();
        final SurfaceView ingame_view = findViewById(R.id.InGame_view);
        ingame_view.getHolder().addCallback(new Holder());

        final ListView listview = findViewById(R.id.Script_list);
        ArrayAdapter<EAGLS.IDX> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, METADATA_Script);

        listview.setAdapter(adapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                inGame.Script = ((EAGLS.IDX) adapterView.getItemAtPosition(position)).filename;
                listview.setVisibility(View.GONE);
                BlockTouch = false;
                inGame.charcter = null;
                inGame.resume();
            }
        });
        listview.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        View decorView = getWindow().getDecorView();

        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!BlockTouch) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP)
                inGame.Touched = true;
        }
        return true;
    }

    public void Del_Force_Char(View v) {
        inGame.charcter = null;
    }

    long next = 0L;

    int idx_rand() {
        next = next * 0x343fd + 0x269ec3;
        return (int) (next >> 16) & 0x7fff;
    }

    long GG_pak_next = 0;

    int pak_rand() {
        GG_pak_next = GG_pak_next * 0xbc8f + (GG_pak_next / 0xadc8) * -0x7fffffff;
        if (GG_pak_next < 1) {
            GG_pak_next += 0x7fffffff;
        }
        double st0 = GG_pak_next * 0.00000000046566128752458 * 256;
        return (int) st0; //Fucking Float Point FUCK FUCKKKKK
    }

    public int toInt32_2(byte[] bytes, int index) {
        int a = (int) ((int) (0xff & bytes[index]) << 32 | (int) (0xff & bytes[index + 1]) << 40 | (int) (0xff & bytes[index + 2]) << 48 | (int) (0xff & bytes[index + 3]) << 56);
        return a;
    }

    private static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
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

    private static Point resize(int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = 1024;
            int height = 768;
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float) maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float) maxWidth / ratioBitmap);
            }
            return new Point(finalWidth, finalHeight);
        } else {
            return new Point(1024, 768);
        }
    }

    List<EAGLS.IDX> Decode_idx(String input) {
        byte[] IDX = new byte[60];
        FileInputStream idx = null;
        try {
            idx = (FileInputStream) expansionFile.getInputStream(input);
            IDX = null;
            IDX = IOUtils.toByteArray(idx);
            idx.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String KEY = "1qaz2wsx3edc4rfv5tgb6yhn7ujm8ik,9ol.0p;/-@:^[]";
        next = toInt32_2(new byte[]{IDX[IDX.length - 4], IDX[IDX.length - 3], IDX[IDX.length - 2], IDX[IDX.length - 1]}, 0);
        for (int i = 0; i < 400000; i++) {
            IDX[i] ^= (byte) KEY.charAt(idx_rand() % 46);
        }

        List<EAGLS.IDX> Parsed = new ArrayList<EAGLS.IDX>();
        ByteArrayInputStream reader = new ByteArrayInputStream(IDX);
        while (true) {
            byte[] readed = new byte[40];
            byte[] filename_buffer = new byte[24];
            reader.read(readed, 0, 40);
            filename_buffer = Arrays.copyOfRange(readed, 0, 24);
            String filename = null;
            filename = new String(filename_buffer).trim();
            if (filename.equals(""))
                break;
            if (input.equals("SCPACK.idx") && !(filename.startsWith("sc") || filename.startsWith("sm"))) {
                continue;
            }
            byte[] offset_buffer = new byte[8];
            byte[] size_buffer = new byte[8];
            offset_buffer = Arrays.copyOfRange(readed, 24, 32);
            size_buffer = Arrays.copyOfRange(readed, 32, 40);
            Parsed.add(new EAGLS.IDX(filename, toInt32_2(offset_buffer, 0) - 0x174B, toInt32_2(size_buffer, 0)));
        }

        return Parsed;
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

    byte[] Decode_WAVE_pak(String filename) {
        EAGLS.IDX who = null;
        for (EAGLS.IDX idx : METADATA_Wave) {
            if (idx.filename.equals(filename)) {
                who = idx;
                break;
            }
        }
        //시발 filter 지원 안되는거 개실화냐
        //언어의 부적절성(JAVA)
        byte[] DECODED_BUFFER = new byte[who.size];
        try {
            FileInputStream pak = (FileInputStream) expansionFile.getInputStream("WAVEPACK.pak");
            pak.skip(who.offset);
            pak.read(DECODED_BUFFER, 0, who.size);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return DECODED_BUFFER;
    }

    byte[] Decode_SCRIPT_pak(String filename) {
        EAGLS.IDX who = null;
        for (EAGLS.IDX idx : METADATA_Script) {
            if (idx.filename.equals(filename)) {
                who = idx;
                break;
            }
        }
        if (who == null) {
            return null;
        }
        byte[] DECODED_BUFFER = new byte[who.size - 0xe10];
        try {
            FileInputStream pak = (FileInputStream) expansionFile.getInputStream("SCPACK.pak");
            pak.skip(who.offset + 0xe10);
            pak.read(DECODED_BUFFER, 0, who.size - 0xe10);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String KEY = "EAGLS_SYSTEM";

        next = DECODED_BUFFER[DECODED_BUFFER.length - 1] & 0xff;
        if (((DECODED_BUFFER[DECODED_BUFFER.length - 1] & 0xff) & 128) == 128) //첫비트가 1이면 실행 (movsx)
        {
            next = next | (0xFFFFFFF << 8);
        }
        for (int i = 0; i < DECODED_BUFFER.length; i += 2) {
            DECODED_BUFFER[i] ^= (byte) KEY.charAt(idx_rand() % 12);
        }
        return DECODED_BUFFER;
    }

    class Holder implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            video.SetDisplay(holder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }

    protected class InGame implements Runnable {
        public void start(SurfaceHolder holder) {
            running = true;
            thread = new Thread(this);
            thread.start();
            surfaceHolder = holder;
        }

        public String Script = "";
        //그림을 그릴 쓰레드를 지정합니다.
        Thread thread = null;
        SurfaceHolder surfaceHolder;
        boolean running = false;
        boolean Touched = false;
        boolean _MovieLoopFlg = false;
        boolean Online;
        EAGLS.Charcter charcter = null;
        TextView subtitle = findViewById(R.id.subtitle);
        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Point image = resize(size.x, size.y);

        void Draw(Bitmap CG, final String MENT, Canvas canvas) {
            canvas.drawColor(Color.BLACK);
            try {
                if (CG != null) {
                    canvas.drawBitmap(CG, (size.x / 2) - (CG.getWidth() / 2), 0, paint);
                }
                //Not working properly
                if (charcter != null) {
                    if (charcter._CosSel_Bitmap != null) {
                        canvas.drawBitmap(charcter._CosSel_Bitmap, (size.x / 2) - (charcter._CosSel_Bitmap.getWidth() / 2), size.y - charcter._CosSel_Bitmap.getHeight(), paint);
                    }
                    if (charcter._Face_Bitmap != null) {
                        canvas.drawBitmap(charcter._Face_Bitmap, (size.x / 2) - (charcter._Face_Bitmap.getWidth() / 2) - 5, size.y - 930, paint);
                    }
                }
            } catch (Exception e) {
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    subtitle.setText(MENT);
                }
            });

            surfaceHolder.unlockCanvasAndPost(canvas);
        }


        void stop() {
            running = false;
            Touched = false;
            _MovieLoopFlg = false;
            thread.interrupted();
        }

        void resume() {
            running = true;
        }

        void PlayAudio(MediaPlayer player, byte[] bytes) {
            // create temp file that will hold byte array
            try {
                File tempMp3 = File.createTempFile("EAGLS_", "audio", getCacheDir());
                tempMp3.deleteOnExit();
                FileOutputStream fos = new FileOutputStream(tempMp3);
                fos.write(bytes);
                fos.close();

                FileInputStream fis = new FileInputStream(tempMp3);
                player.setDataSource(fis.getFD());

                player.prepare();
                player.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void run() {
            while (true) {
                if (!running) {
                    continue;
                }
                EAGLS.SCRIPT sci = new EAGLS.SCRIPT();
                List<EAGLS.Todo> genrated = null;
                Bitmap CG = null;
                subtitle = findViewById(R.id.subtitle);
                String MENT = "";
                int _CosSel = 0;
                int _Face = 0;
                int _Cheek = 0;
                int _Parts = 0;
                int _WaitTime = 0;
                String _Target = null;
                boolean _CharUP = false;
                boolean Char_Parsed = false;
                MediaPlayer Talk_Player = new MediaPlayer();
                MediaPlayer BGM_Player = new MediaPlayer();
                MediaPlayer Effect_Player = new MediaPlayer();
                StringBuilder text = new StringBuilder();
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader((FileInputStream) expansionFile.getInputStream("Script/" + Script)));
                    String line;
                    while ((line = br.readLine()) != null) {
                        text.append(line);
                        text.append('\n');
                    }
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (Locale.getDefault().getLanguage().equals("ja")) {
                    try {
                        genrated = sci.Parse(new String(Decode_SCRIPT_pak(Script), "Shift-jis").split("\\r?\\n"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else {
                    genrated = sci.Parse(text.toString().split("\\r?\\n"));
                }
                if (genrated == null) {
                    Canvas canvas = surfaceHolder.lockCanvas();
                    Draw(null, "스크립트 암호화 해독 실패", canvas);
                }
                mainloop:
                while (running) {
                    if (surfaceHolder.getSurface().isValid()) {
                        String _GRPName = null;
                        String _BGMName = "";
                        String _MovieFileName = "";
                        String who = "주인공";
                        for (int i = 0; i < genrated.size(); i++) {
                            if (!running) {
                                break;
                            }
                            Touched = false;
                            boolean talked = false;
                            boolean Excuted_Function = false;

                            EAGLS.Todo what = genrated.get(i);
                            Canvas canvas = surfaceHolder.lockCanvas();
                            paint.setColor(Color.WHITE);
                            paint.setTextSize(50);
                            paint.setStyle(Paint.Style.STROKE);
                            paint.setStrokeWidth(4);
                            if (what.kind == EAGLS.Todo.Kind.Ment) {
                                MENT = who + ": " + what.Ment;
                                who = "주인공";
                            }
                            if (what.kind == EAGLS.Todo.Kind.Talk) {
                                who = what.Ment;
                                talked = true;
                                String sound = what.Sound;
                                what = genrated.get(++i);
                                MENT = who + ": " + what.Ment;
                                Draw(CG, MENT, canvas);
                                if (sound != null) {
                                    Talk_Player.reset();
                                    PlayAudio(Talk_Player, Decode_WAVE_pak(sound + ".ogg"));
                                    while (true) {
                                        if (Touched) {
                                            Talk_Player.stop();
                                            break;
                                        }
                                        if (!Talk_Player.isPlaying()) {
                                            break;
                                        }
                                    }
                                }
                            }
                            if (what.kind == EAGLS.Todo.Kind.Function) {
                                Excuted_Function = true;
                                if (what.Function == 52) {
                                    if (what.Parms[0].equals("\"_GRPName\"") || what.Parms[0].equals("\"_grpname\"")) {
                                        _GRPName = what.Parms[1].replace("\"", "");
                                        if (Char_Parsed) {
                                            charcter = new EAGLS.Charcter(_CosSel, _Face, _Cheek, _Parts, _Target, _CharUP);
                                            Char_Parsed = false;
                                        }
                                    } else if (what.Parms[0].equals("\"_BGMName\"")) {
                                        _BGMName = what.Parms[1].replace("\"", "") + ".WAV";
                                    } else if (what.Parms[0].equals("\"_MovieFileName\"")) {
                                        _MovieFileName = what.Parms[1].replace("\"", "") + ".mp4";
                                    }
                                } else if (what.Function == 42) {
                                    if (what.Parms[0].equals("\"BGM\"")) {
                                        try {
                                            if (_BGMName.equals(".WAV")) {
                                                surfaceHolder.unlockCanvasAndPost(canvas);
                                                continue;
                                            }
                                            BGM_Player.reset();
                                            BGM_Player.setVolume(0.3f, 0.3f);
                                            BGM_Player.setDataSource(expansionFile.getAssetFileDescriptor("WAVE/" + _BGMName.toUpperCase()));
                                            BGM_Player.prepare();
                                            BGM_Player.setLooping(true);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        BGM_Player.start();
                                    } else if (what.Parms[1].equals("\"02MiniMovPlay.dat\"")) {
                                        BlockTouch = true;
                                        if (_MovieFileName.charAt(0) == 'i') {
                                            Effect_Player.reset();
                                            Effect_Player.setVolume(0.7f, 0.7f);
                                            PlayAudio(Effect_Player, Decode_WAVE_pak("se201.wav"));
                                            Effect_Player.start();
                                        }
                                        video.run(_MovieFileName, true, _MovieLoopFlg, image);
                                    } else if (what.Parms[1].equals("\"00HMovPlay.dat\"")) {
                                        video.run(_MovieFileName, false, _MovieLoopFlg, image);
                                    } else if (what.Parms[1].equals("\"00Draw.dat\"") || what.Parms[1].equals("\"00draw.dat\"")) {
                                        int orientation = getResources().getConfiguration().orientation;
                                        if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
                                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                                            int temp = size.x;
                                            size.x = size.y;
                                            size.y = temp;
                                        }
                                        if (what.Parms[0].equals("\"DrawBG\"") || what.Parms[0].equals("\"DrawCG\"")) {
                                            try {
                                                byte[] RAW_byte = Decode_CG_pak(_GRPName + ".gr");
                                                Bitmap bitmap = BitmapFactory.decodeByteArray(RAW_byte, 0, RAW_byte.length);
                                                CG = resize(bitmap, size.x, size.y);
                                            } catch (Exception e) {
                                            }
                                        } else if (what.Parms[0].equals("\"DrawChar\"") && charcter != null) {
                                            charcter._CosSel = charcter._CosSel != 0 ? charcter._CosSel : 1;
                                            byte[] RAW_byte = null;
                                            try {
                                                RAW_byte = Decode_CG_pak(String.format("%s_b%02d", _GRPName, charcter._CosSel) + ".gr");
                                            } catch (Exception e) {
                                                charcter = null;
                                                Draw(CG, MENT, canvas);
                                                continue;
                                            }
                                            BitmapFactory.Options opt = new BitmapFactory.Options();
                                            opt.inMutable = true;
                                            opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
                                            Bitmap Temp_Bitmap = BitmapFactory.decodeByteArray(RAW_byte, 0, RAW_byte.length, opt);
                                            Temp_Bitmap.setHasAlpha(true);
                                            Temp_Bitmap = EAGLS.Charcter.Alpha_bmp(Temp_Bitmap, RAW_byte);
                                            charcter._CosSel_Bitmap = resize(Temp_Bitmap, size.x, size.y);
                                            float ratio = (float) charcter._CosSel_Bitmap.getHeight() / Temp_Bitmap.getHeight();
                                            try {
                                                RAW_byte = Decode_CG_pak(String.format("%s_f%02d", _GRPName, charcter._Face) + ".gr");
                                            } catch (Exception e) {
                                                charcter = null;
                                                Draw(CG, MENT, canvas);
                                                continue;
                                            }
                                            Temp_Bitmap = BitmapFactory.decodeByteArray(RAW_byte, 0, RAW_byte.length, opt);
                                            Temp_Bitmap.setHasAlpha(true);
                                            Temp_Bitmap = EAGLS.Charcter.Alpha_bmp(Temp_Bitmap, RAW_byte);
                                            charcter._Face_Bitmap = resize(Temp_Bitmap, (int) (Temp_Bitmap.getWidth() * ratio), (int) (Temp_Bitmap.getHeight() * ratio));
/*
                                        RAW_byte = Decode_CG_pak(String.format("%s_h%02d", _GRPName, charcter._Cheek) + ".gr");
                                        Temp_Bitmap = BitmapFactory.decodeByteArray(RAW_byte, 0, RAW_byte.length, opt);
                                        allpixels = new int[Temp_Bitmap.getHeight() * Temp_Bitmap.getWidth()];
                                        Temp_Bitmap.getPixels(allpixels, 0, Temp_Bitmap.getWidth(), 0, 0, Temp_Bitmap.getWidth(), Temp_Bitmap.getHeight());
                                        for (int k = 0; k < allpixels.length; k++) {
                                            if (allpixels[k] >>> 24 == 255) {
                                                allpixels[k] = Color.TRANSPARENT;
                                            }
                                        }
                                        Temp_Bitmap.setPixels(allpixels, 0, Temp_Bitmap.getWidth(), 0, 0, Temp_Bitmap.getWidth(), Temp_Bitmap.getHeight());
                                        Temp_Bitmap.setHasAlpha(true);
                                        charcter._Cheek_Bitmap = Temp_Bitmap;


                                        RAW_byte = Decode_CG_pak(String.format("%s_z%02d", _GRPName, charcter._Parts) + ".gr");
                                        Temp_Bitmap = BitmapFactory.decodeByteArray(RAW_byte, 0, RAW_byte.length, opt);
                                        allpixels = new int[Temp_Bitmap.getHeight() * Temp_Bitmap.getWidth()];
                                        Temp_Bitmap.getPixels(allpixels, 0, Temp_Bitmap.getWidth(), 0, 0, Temp_Bitmap.getWidth(), Temp_Bitmap.getHeight());
                                        for (int k = 0; k < allpixels.length; k++) {
                                            if (allpixels[k] >>> 24 == 255) {
                                                allpixels[k] = Color.TRANSPARENT;
                                            }
                                        }
                                        Temp_Bitmap.setPixels(allpixels, 0, Temp_Bitmap.getWidth(), 0, 0, Temp_Bitmap.getWidth(), Temp_Bitmap.getHeight());
                                        Temp_Bitmap.setHasAlpha(true);
                                        charcter._Parts_Bitmap = Temp_Bitmap;*/
                                        }
                                    } else if (what.Parms[1].equals("\"01Draw.dat\"")) {
                                        if (what.Parms[0].equals("\"DrawCGEX\"")) {
                                            int orientation = getResources().getConfiguration().orientation;
                                            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                                                int temp = size.x;
                                                size.x = size.y;
                                                size.y = temp;
                                            }
                                            byte[] RAW_byte = Decode_CG_pak(_GRPName + ".gr");
                                            Bitmap bitmap = BitmapFactory.decodeByteArray(RAW_byte, 0, RAW_byte.length);
                                            CG = resize(bitmap, size.x, size.y);
                                        }
                                    } else if (what.Parms[1].equals("\"00Wait.dat\"")) {
                                        try {
                                            Thread.sleep(_WaitTime);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                } else if (what.Function == 100) {
                                    if (!what.Parms[0].equals("\"\"") && !what.Parms[0].equals("\"_GRPName.wav\"")) {
                                        Effect_Player.reset();
                                        Effect_Player.setVolume(0.7f, 0.7f);
                                        PlayAudio(Effect_Player, Decode_WAVE_pak(what.Parms[0].replace("\"", "") + ".wav"));
                                        Effect_Player.start();
                                    }
                                } else if (what.Function == 41) {
                                    if (!what.Parms[1].contains("MAP")) {
                                        if (what.Parms[1].equals("\"" + what.Parms[0].replace("\"", "") + ".dat" + "\"")) {
                                            Script = what.Parms[1].replace("\"", "");
                                            surfaceHolder.unlockCanvasAndPost(canvas);
                                            break mainloop;
                                        }
                                    }
                                }
                            }
                            if (what.kind == EAGLS.Todo.Kind.Assign) {
                                Excuted_Function = true;
                                if (what.Parms[0].equals("_MovieLoopFlg")) {
                                    _MovieLoopFlg = what.Parms[1].equals("0") ? false : true;
                                } else if (what.Parms[0].equals("_Target")) {
                                    _Target = what.Parms[1];
                                } else if (what.Parms[0].equals("_CosSel")) {
                                    _CosSel = Integer.parseInt(what.Parms[1].split(",")[0]);
                                } else if (what.Parms[0].equals("_Face")) {
                                    _Face = Integer.parseInt(what.Parms[1]);
                                } else if (what.Parms[0].equals("_Cheek")) {
                                    _Cheek = Integer.parseInt(what.Parms[1]);
                                } else if (what.Parms[0].equals("_Parts")) {
                                    _Parts = Integer.parseInt(what.Parms[1]);
                                } else if (what.Parms[0].equals("_CharUP")) {
                                    _CharUP = what.Parms[1].equals("1");
                                    Char_Parsed = true;
                                } else if (what.Parms[0].equals("_WaitTime")) {
                                    _WaitTime = Integer.parseInt(what.Parms[1]);
                                }
                            }

                            if (!talked)
                                Draw(CG, MENT, canvas);
                            if (!Excuted_Function) {
                                while (!Touched) {
                                    if (!running) {
                                        break;
                                    }
                                }
                            }
                            if (!running) {
                                break;
                            }
                        }
                        running = false;
                        Canvas canvas = surfaceHolder.lockCanvas();
                        Draw(CG, "스크립트 끝", canvas);
                        break;
                    }
                }
            }
        }
    }

    protected class VideoPlayer {
        MediaPlayer Player = new MediaPlayer();
        SurfaceView videoPlayer = findViewById(R.id.Video_view);
        SurfaceHolder sh = null;

        public void SetDisplay(SurfaceHolder sh) {
            this.sh = sh;
        }

        private Point resize(int imageWidth, int imageHeight, int maxWidth, int maxHeight) {
            if (maxHeight > 0 && maxWidth > 0) {
                int width = imageWidth;
                int height = imageHeight;
                float ratioBitmap = (float) width / (float) height;
                float ratioMax = (float) maxWidth / (float) maxHeight;

                finalWidth = maxWidth;
                int finalHeight = maxHeight;
                if (ratioMax > ratioBitmap) {
                    finalWidth = (int) ((float) maxHeight * ratioBitmap);
                } else {
                    finalHeight = (int) ((float) maxWidth / ratioBitmap);
                }
                return new Point(finalWidth, finalHeight);
            } else {
                return new Point(imageWidth, imageHeight);
            }
        }

        public void run(String Path, final boolean IsMini, final boolean _MovieLoopFlg, final Point image) {
            final MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            AssetFileDescriptor fd = expansionFile.getAssetFileDescriptor("MOVIE/" + Path);
            metaRetriever.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            runOnUiThread(new Runnable() {
                public void run() {
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) videoPlayer.getLayoutParams();
                    if (IsMini) {
                        params.gravity = Gravity.CENTER_VERTICAL;
                    } else {
                        params.width = Gravity.NO_GRAVITY;
                    }
                    params.width = image.x;
                    params.height = resize(Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)), Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)), image.x, image.y).y;
                    videoPlayer.setLayoutParams(params);
                    videoPlayer.setX((size.x / 2) - (image.x / 2));
                    videoPlayer.setVisibility(View.VISIBLE);
                }
            });

            try {
                Player.reset();
                Player.setDisplay(sh);
                Player.setDataSource(fd);
                Player.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (_MovieLoopFlg) {
                Player.setLooping(true);
            }
            Player.start();
            Player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    if (IsMini)
                        BlockTouch = false;

                    if (!_MovieLoopFlg) {
                        videoPlayer.setVisibility(View.GONE);
                        BlockTouch = false;
                    }
                }
            });
        }
    }
}
