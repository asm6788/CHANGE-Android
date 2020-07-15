package com.asm6788.eagls;

import android.graphics.drawable.GradientDrawable;
import android.util.Log;

import java.util.Arrays;

public class LZSS {
    public static byte[] decompress_lzss(byte[] Orginial) {
        int index = 0;
        byte[] Slice_buf = new byte[Orginial.length];
        System.arraycopy(Orginial, 0xfef, Slice_buf, 0xfef, Slice_buf.length - 0xfef);
        byte[] Decode = new byte[10000000];
        int i, k, r, c;
        int flags;
        r = 0xFEE;
        flags = 0;
        int f = 0;
        while (index < Orginial.length) {
            flags = flags >> 1;
            if ((flags & 256) == 0) {
                c = Orginial[index++] & 0xff;
                flags = (int) c | 0xff00;
            }
            if ((flags & 1) == 0) {
                if (index == Orginial.length)
                    i = 0;
                else
                    i = Orginial[index++] & 0xff;
                int j;
                if (index == Orginial.length)
                    j = 0;
                else
                    j = Orginial[index++] & 0xff;
                for (k = 0; k <= (j & 0xf) + 2; k++) {
                    c = Slice_buf[k + (i | (j & 0xf0) << 4) & 0xfff] & 0xff;
                    Slice_buf[r] = (byte) c;
                    if (f < Decode.length)
                        Decode[f++] = (byte) c;
                    r = (r + 1) & 0xfff;
                }
            } else {
                if (index == Orginial.length)
                    c = 0;
                else
                    c = Orginial[index++] & 0xff;
                Slice_buf[r] = (byte) c;
                if (f < Decode.length)
                    Decode[f++] = (byte) c;
                r = (r + 1) & 0xfff;
            }
        }
        return TrimEnd(Decode);
    }

    static byte[] TrimEnd(byte[] bytes) {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0) {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }
}
