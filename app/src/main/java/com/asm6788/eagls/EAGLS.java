package com.asm6788.eagls;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class EAGLS {
    public static class IDX {
        public String filename;
        public int offset;
        public int size;

        public IDX(String filename, int offset, int size) {
            this.filename = filename;
            this.offset = offset;
            this.size = size;
        }

        public String toString() {
            return filename;
        }
    }

    public static class Todo {
        public enum Kind {
            Ment,
            Talk,
            Assign,
            Function;

            public static final int SIZE = java.lang.Integer.SIZE;

            public int getValue() {
                return this.ordinal();
            }

            public static Kind forValue(int value) {
                return values()[value];
            }
        }

        public Kind kind;
        public String Ment;
        public String Sound;
        public String CG;
        public int Function;
        public String[] Parms;

        public Todo(Kind kind, String ment, String sound, String cG, int function, String[] parms) {
            this.kind = kind;
            Ment = ment;
            Sound = sound;
            CG = cG;
            Function = function;
            Parms = parms;
        }
    }


    public static class SCRIPT {
        public final List<Todo> Parse(String[] script) {
            Log.d("", "스크립트 파싱 중");
            List<Todo> genrated = new ArrayList<Todo>();
            String buffer = "";
            int function = -1;
            String kind = "";
            boolean Is_last_Number = false;
            boolean Is_Talk_Sound = false;
            String[] parm = new String[2];
            for (String line : script) {
                int offset = 0;
                for (int i = 0; i < line.length(); i++) {
                    buffer += line.charAt(i);
                    if (line.charAt(i) == '#' || kind.equals("#")) {
                        if (kind.equals("")) {
                            kind = "#";
                            buffer = "";
                        }
                        if (line.charAt(i) == '=') {
                            Is_Talk_Sound = true;
                            parm[0] = buffer.substring(0, buffer.length() - 1);
                            buffer = "";
                        }
                        if (i == line.length() - 1) {
                            kind = "";
                            if (Is_Talk_Sound) {
                                parm[1] = buffer;
                            }
                            buffer = "";
                            Is_Talk_Sound = false;
                            if (parm[0] != null && parm[1] != null)
                                genrated.add(new Todo(Todo.Kind.Talk, parm[0], parm[1], null, 0, null));
                            parm[0] = null;
                            parm[1] = null;
                        }
                    } else if (line.charAt(i) == '&') {
                        int start = IndexOfNth(line, "\"", offset++);
                        int end = IndexOfNth(line, "\"", offset++);
                        genrated.add(new Todo(Todo.Kind.Ment, line.substring(start + 1, end), null, null, 0, null));
                        buffer = "";
                        i = end;
                        kind = "";
                        continue;
                    } else if (47 < line.charAt(i) && line.charAt(i) < 58) {
                        Is_last_Number = true;
                    } else if (Is_last_Number && buffer.charAt(buffer.length() - 1) == '(') {
                        Is_last_Number = false;
                        if (isNumeric(buffer.substring(0, buffer.length() - 1))) {
                            function = Integer.parseInt(buffer.substring(0, buffer.length() - 1));
                        }
                        buffer = "";
                    } else if (function != -1 && line.charAt(i) == ')') {
                        buffer = buffer.substring(0, buffer.length() - 1);
                        offset += buffer.split("\"", -1).length - 1;
                        String[] parms = buffer.split("[,]", -1);
                        genrated.add(new Todo(Todo.Kind.Function, null, null, null, function, parms));
                        function = -2;
                        buffer = "";
                    } else if (function == -2 && line.charAt(i) == '_' && i != 4 && line.charAt(0) != '$') {
                        line = line.substring(line.lastIndexOf('_'));
                        genrated.add(new Todo(Todo.Kind.Assign, null, null, null, function, line.substring(0, line.length() - 1).split("[=]", -1)));
                        break;
                    }
                }
                buffer = "";
            }

            Log.d("", "스크립트 파싱 완료");
            return genrated;
        }

        public boolean isNumeric(String str) {
            return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
        }

        private int IndexOfNth(String str, String value, int nth) {
            int offset = str.indexOf(value);
            for (int i = 0; i < nth; i++) {
                if (offset == -1) {
                    return -1;
                }
                offset = str.indexOf(value, offset + 1);
            }

            return offset;
        }

    }

    public static class Charcter {
        public Charcter(int _CosSel, int _Face, int _Cheek, int _Parts, String _Target, boolean _CharUP) {
            this._CosSel = _CosSel;
            this._Face = _Face;
            this._Cheek = _Cheek;
            this._Parts = _Parts;
            this._Target = _Target;
            this._CharUP = _CharUP;
        }

        public int _CosSel;
        public int _Face;
        public int _Cheek;
        public int _Parts;
        public String _Target;
        public boolean _CharUP;

        public Bitmap _CosSel_Bitmap;
        public Bitmap _Face_Bitmap;
        public Bitmap _Cheek_Bitmap;
        public Bitmap _Parts_Bitmap;
    }
}
