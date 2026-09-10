package core;

import core.shell.ShellEntity;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

public class Encoding {
    private String charsetString;

    public static final String AUTO = "Auto";

    /**
     * Auto 模式下最近一次含 CJK 探测命中的字符集。
     * 编码方向据此回推：目录列表按 GBK 解码出中文后，进入该目录时也必须按 GBK 编码回发，
     * 否则目标端按自身字符集解析路径会「不存在」。纯 ASCII 响应不改变该状态。
     */
    private volatile String lastDecodedCharset;

    /**
     * 目标端**实际**用来解释我们发去字节的字符集，由知道答案的 payload 类填入：
     * Java payload 从目标的 {@code file.encoding} 读（模块解码用的就是
     * {@code Charset.defaultCharset()}）；.NET Core payload 恒为 UTF-8。
     *
     * 一旦设置就**优先于**用户选择：目标端的解码方式不可配置，选别的必然乱码。
     * 控制台代码页（chcp）推断只对 C#/ASP 这类用 {@code Encoding.Default}/
     * 会话代码页的目标成立；JDK 18+ 起 {@code file.encoding} 固定为 UTF-8
     * （JEP 400），与 chcp 分叉，所以不能只靠 chcp。
     *
     * 只影响**编码方向**。响应方向仍交给 autoDecoding 逐响应判断——响应里混着
     * 命令输出（控制台码页）和 payload 自己拼的文本（JVM 字符集），不能一刀切。
     */
    private volatile String remoteCharset;

    private static final String[] ENCODING_TYPES = new String[]{"Auto", "UTF-8", "GBK", "GB2312", "BIG5", "GB18030", "ISO-8859-1", "latin1", "UTF16", "ascii", "cp850"};

    private Encoding(String charsetString) {
        this.charsetString = charsetString;
    }

    public static String[] getAllEncodingTypes() {
        return ENCODING_TYPES;
    }

    public byte[] Encoding(String text) {
        String charset = this.encodeCharset();
        try {
            if ("UTF-16LE".equals(charset)) {
                byte[] body = text.getBytes("UTF-16LE");
                byte[] out = new byte[body.length + 2];
                out[0] = (byte)0xFF;
                out[1] = (byte)0xFE;
                System.arraycopy(body, 0, out, 2, body.length);
                return out;
            }
            if ("UTF-16BE".equals(charset)) {
                byte[] body = text.getBytes("UTF-16BE");
                byte[] out = new byte[body.length + 2];
                out[0] = (byte)0xFE;
                out[1] = (byte)0xFF;
                System.arraycopy(body, 0, out, 2, body.length);
                return out;
            }
            return charset != null ? text.getBytes(charset) : text.getBytes();
        } catch (UnsupportedEncodingException e) {
            util.Log.error(e);
            return text.getBytes();
        }
    }

    // Auto: use the charset that won the last CJK decode; before any detection fall back to
    // a real Java charset (never the "Auto" label, never null).
    private String encodeCharset() {
        // What the target decodes with beats every guess, including an explicit
        // pick: target-side decoding is not configurable, so anything else is
        // guaranteed mojibake. Only Java/NetCore payloads set this.
        if (this.remoteCharset != null && this.remoteCharset.trim().length() > 0) {
            String cs = this.remoteCharset.trim();
            try {
                if (Charset.isSupported(cs)) {
                    return cs;
                }
            } catch (Exception ignored) {
            }
        }
        if (!AUTO.equals(this.charsetString)) {
            return this.charsetString;
        }
        if (this.lastDecodedCharset != null && this.lastDecodedCharset.length() > 0) {
            return this.lastDecodedCharset;
        }
        return resolveIoCharset(AUTO, null);
    }

    public String Decoding(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        if (AUTO.equals(this.charsetString)) {
            return autoDecoding(bytes);
        }
        // 已知目标字符集时也不能钉死解码方向：目标端的响应本来就是混合编码的
        // —— 命令输出是操作系统码页的字节原样返回，payload 自己拼的文本才走
        // JVM/协议字符集。钉死任何一个都会错一半，所以交给逐响应探测。
        // （常见情形下探测结果与钉死的值一致，行为不变。）
        if (this.remoteCharset != null && this.remoteCharset.trim().length() > 0) {
            return autoDecoding(bytes);
        }
        try {
            return new String(bytes, this.charsetString);
        } catch (UnsupportedEncodingException e) {
            util.Log.error(e);
            return new String(bytes);
        }
    }

    // Auto: BOM markers win first, then strict UTF-8. GBK bytes can occasionally
    // form valid UTF-8 (e.g. CJK GBK pairs decode as Hebrew), so when strict UTF-8
    // succeeds but shows no CJK while strict GBK decode does contain CJK, treat the
    // content as GBK. Latinish UTF-8 (accents) is kept as UTF-8 even though GBK
    // misdecode of it yields CJK. Never uses isMessyCode (false-positives on pure
    // Chinese+digit strings).
    private String autoDecoding(byte[] bytes) {
        String bom = decodeWithBom(bytes);
        if (bom != null) {
            return bom;
        }
        String utf8 = strictDecode("UTF-8", bytes);
        if (utf8 != null) {
            if (containsCjk(utf8)) {
                this.lastDecodedCharset = "UTF-8";
                return utf8;
            }
            if (!containsNonAscii(utf8)) {
                return utf8; // pure ASCII: keep previous detection
            }
            if (looksLatinish(utf8)) {
                this.lastDecodedCharset = "UTF-8";
                return utf8;
            }
            String gbk = strictDecode("GBK", bytes);
            if (gbk != null && containsCjk(gbk)) {
                this.lastDecodedCharset = "GBK";
                return gbk;
            }
            this.lastDecodedCharset = "UTF-8";
            return utf8;
        }
        String gbk = strictDecode("GBK", bytes);
        if (gbk != null) {
            this.lastDecodedCharset = "GBK";
            return gbk;
        }
        String gb18030 = strictDecode("GB18030", bytes);
        if (gb18030 != null) {
            this.lastDecodedCharset = "GB18030";
            return gb18030;
        }
        String big5 = strictDecode("BIG5", bytes);
        if (big5 != null) {
            this.lastDecodedCharset = "BIG5";
            return big5;
        }
        return new String(bytes);
    }

    private String decodeWithBom(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            String s = strictDecode("UTF-8", bytes);
            if (s != null) {
                this.lastDecodedCharset = "UTF-8";
                return stripBom(s);
            }
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
            String s = strictDecode("UTF-16LE", bytes);
            if (s != null) {
                this.lastDecodedCharset = "UTF-16LE";
                return stripBom(s);
            }
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
            String s = strictDecode("UTF-16BE", bytes);
            if (s != null) {
                this.lastDecodedCharset = "UTF-16BE";
                return stripBom(s);
            }
        }
        return null;
    }

    private static String stripBom(String s) {
        return s.startsWith("﻿") ? s.substring(1) : s;
    }

    // Returns null when the byte sequence is not valid in the charset.
    private static String strictDecode(String charset, byte[] bytes) {
        try {
            CharsetDecoder decoder = Charset.forName(charset).newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean containsCjk(String text) {
        for (int i = 0; i < text.length(); ++i) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(text.charAt(i));
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsNonAscii(String text) {
        for (int i = 0; i < text.length(); ++i) {
            if (text.charAt(i) >= 0x80) {
                return true;
            }
        }
        return false;
    }

    // Non-ASCII chars all in common European letter/punctuation blocks: real UTF-8.
    // GBK misdecoding of UTF-8 accents yields CJK (e.g. é -> 茅), which must not win.
    private static boolean looksLatinish(String text) {
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c < 0x80) {
                continue;
            }
            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
            if (block != Character.UnicodeBlock.LATIN_1_SUPPLEMENT
                    && block != Character.UnicodeBlock.LATIN_EXTENDED_A
                    && block != Character.UnicodeBlock.LATIN_EXTENDED_B
                    && block != Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL
                    && block != Character.UnicodeBlock.GENERAL_PUNCTUATION
                    && block != Character.UnicodeBlock.CURRENCY_SYMBOLS) {
                return false;
            }
        }
        return true;
    }

    /**
     * Charset name safe for {@code InputStreamReader}/{@code OutputStreamWriter}.
     * {@code Auto} is a detector label, not a Java charset.
     */
    public String ioCharsetName() {
        return resolveIoCharset(this.charsetString, this.lastDecodedCharset);
    }

    public static String resolveIoCharset(String name) {
        return resolveIoCharset(name, null);
    }

    public static String resolveIoCharset(String name, String autoHint) {
        if (name != null) {
            String trimmed = name.trim();
            if (trimmed.length() > 0 && !AUTO.equalsIgnoreCase(trimmed)) {
                try {
                    if (Charset.isSupported(trimmed)) {
                        return trimmed;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (autoHint != null && autoHint.trim().length() > 0) {
            try {
                if (Charset.isSupported(autoHint.trim())) {
                    return autoHint.trim();
                }
            } catch (Exception ignored) {
            }
        }
        return Charset.defaultCharset().name();
    }

    /**
     * 声明目标端的真实字符集（见 {@link #remoteCharset}）。传 null/空串表示未知，
     * 退回原有推断逻辑。由 payload 类在拿到目标的信息后调用。
     */
    public void setRemoteCharset(String charset) {
        this.remoteCharset = charset;
    }

    public String getRemoteCharset() {
        return this.remoteCharset;
    }

    public void setCharsetString(String charsetString) {
        this.charsetString = charsetString;
    }

    public String getCharsetString() {
        return this.charsetString;
    }

    public static Encoding getEncoding(ShellEntity shellEntity) {
        return shellEntity.getEncodingModule();
    }

    public static Encoding getEncoding(String charsetString) {
        return new Encoding(charsetString);
    }

    @Override
    public String toString() {
        return this.charsetString;
    }
}
