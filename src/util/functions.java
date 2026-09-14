//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package util;

import cn.hutool.extra.pinyin.PinyinUtil;
import com.formdev.flatlaf.util.StringUtils;
import com.google.common.base.Preconditions;
import core.ApplicationContext;
import javassist.ClassPool;
import javassist.CtClass;
import sun.misc.Unsafe;
import util.http.Http;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.awt.Desktop.Action;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.*;

public final class functions {
    private static final char[] toBase64 = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};
    private static final char[] toBase64URL = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'};
    private static final byte[] HEX_BYTES = new byte[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final double TOOLSKIT_WIDTH = 1920.0;
    private static final double TOOLSKIT_HEIGHT = 1080.0;
    private static double CURRENT_WIDTH = 1920.0;
    private static double CURRENT_HEIGHT = 1080.0;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final ThreadLocal<SplittableRandom> SplittableRandomLocal = new ThreadLocal();
    public static Unsafe unsafe;

    private functions() {
    }

    public static boolean toBoolean(String s) {
        try {
            return Boolean.parseBoolean(s.toLowerCase());
        } catch (Exception var2) {
            Log.error(var2);
            return false;
        }
    }

    public static String substring(String str, int beginIndex, int endIndex) {
        // NOTE: this used to be "str == null && str.length() == 0", which can
        // never be true (a null str NPEs on length(), and the && needs both), and
        // it clamped endIndex to str.length() - 1. Truncating an empty or 1-char
        // string therefore called substring(0, -1) and threw
        // "String index out of range: -1", hiding the real upstream error.
        if (str == null || str.length() == 0) {
            return "";
        }
        if (beginIndex < 0) {
            beginIndex = 0;
        }
        if (beginIndex > str.length()) {
            beginIndex = str.length();
        }
        if (endIndex > str.length()) {
            endIndex = str.length();
        }
        if (endIndex < beginIndex) {
            endIndex = beginIndex;
        }

        return str.substring(beginIndex, endIndex);
    }

    public static byte[] stringToUnicodeBytes(String str, boolean isAppendZeroChar) {


        try {
            byte[] bytes;
            if (!StringUtils.isEmpty(str)) {
                bytes = str.getBytes("UTF_16LE");
                if (bytes[0] == -2 && bytes[1] == -1) {
                    bytes = Arrays.copyOfRange(bytes, 2, bytes.length);
                }
            } else {
                bytes = new byte[0];
            }

            if (isAppendZeroChar) {
                byte[] bytes1 = new byte[bytes.length + 2];
                System.arraycopy(bytes, 0, bytes1, 0, bytes.length);
                bytes = bytes1;
            }

            return bytes;
        } catch (Throwable var4) {
            return null;
        }
    }

    public static String getMethodSignature(Method method) {
        StringBuilder s = new StringBuilder();
        Class[] types = new Class[method.getParameterTypes().length + 1];
        String[] typeStrArr = new String[types.length];
        System.arraycopy(method.getParameterTypes(), 0, types, 0, types.length - 1);
        types[types.length - 1] = method.getReturnType();

        int i;
        for(i = 0; i < types.length; ++i) {
            Class type = types[i];
            boolean isArray = type.isArray();
            if (isArray) {
                type = type.getComponentType();
            }

            if (Integer.TYPE.equals(type)) {
                typeStrArr[i] = "I";
            } else if (Void.TYPE.equals(type)) {
                typeStrArr[i] = "V";
            } else if (Boolean.TYPE.equals(type)) {
                typeStrArr[i] = "Z";
            } else if (Character.TYPE.equals(type)) {
                typeStrArr[i] = "C";
            } else if (Byte.TYPE.equals(type)) {
                typeStrArr[i] = "B";
            } else if (Short.TYPE.equals(type)) {
                typeStrArr[i] = "S";
            } else if (Float.TYPE.equals(type)) {
                typeStrArr[i] = "F";
            } else if (Long.TYPE.equals(type)) {
                typeStrArr[i] = "J";
            } else if (Double.TYPE.equals(type)) {
                typeStrArr[i] = "D";
            } else {
                typeStrArr[i] = "L" + type.getName().replace(".", "/") + ";";
            }

            if (isArray) {
                typeStrArr[i] = "[" + typeStrArr[i];
            }
        }

        s.append("(");

        for(i = 0; i < typeStrArr.length - 1; ++i) {
            s.append(typeStrArr[i]);
        }

        s.append(")");
        s.append(typeStrArr[typeStrArr.length - 1]);
        return s.toString();
    }

    public static String getNetworSpeedk(long size) {
        if (size < 1024L) {
            return size + "B";
        } else {
            size /= 1024L;
            if (size < 1024L) {
                return size + "KB";
            } else {
                size /= 1024L;
                if (size < 1024L) {
                    size *= 100L;
                    return size / 100L + "." + size % 100L + "MB";
                } else {
                    size = size * 100L / 1024L;
                    return size / 100L + "." + size % 100L + "GB";
                }
            }
        }
    }

    public static void concatMap(Map<String, List<String>> receiveMap, Map<String, List<String>> map) {
        Iterator<String> iterator = map.keySet().iterator();
        String key = null;

        while(iterator.hasNext()) {
            key = (String)iterator.next();
            receiveMap.put(key, map.get(key));
        }

    }

    public static boolean isMatch(String s, String p, boolean us) {
        return us ? isMatch(s, p) : isMatch(s.toLowerCase(), p.toLowerCase());
    }

    public static String SHA(byte[] data, String strType) {
        String strResult = null;
        if (data != null && data.length > 0) {
            try {
                MessageDigest messageDigest = MessageDigest.getInstance(strType);
                messageDigest.update(data);
                byte[] byteBuffer = messageDigest.digest();
                StringBuffer strHexString = new StringBuffer();

                for(int i = 0; i < byteBuffer.length; ++i) {
                    String hex = Integer.toHexString(255 & byteBuffer[i]);
                    if (hex.length() == 1) {
                        strHexString.append('0');
                    }

                    strHexString.append(hex);
                }

                strResult = strHexString.toString();
            } catch (NoSuchAlgorithmException var8) {
                var8.printStackTrace();
            }
        }

        return strResult;
    }

    public static byte[] hash(String algorithm, byte[] bytes) {
        byte[] hashBytes = null;

        try {
            MessageDigest hashDigest = MessageDigest.getInstance(algorithm);
            hashDigest.update(bytes);
            hashBytes = hashDigest.digest();
        } catch (Throwable var4) {
        }

        return hashBytes;
    }

    public static boolean isMatch(String str, String pattern) {
        int i = 0;
        int j = 0;
        int starIndex = -1;
        int iIndex = -1;

        while(true) {
            while(i < str.length()) {
                if (j < pattern.length() && (pattern.charAt(j) == '?' || pattern.charAt(j) == str.charAt(i))) {
                    ++i;
                    ++j;
                } else if (j < pattern.length() && pattern.charAt(j) == '*') {
                    starIndex = j;
                    iIndex = i;
                    ++j;
                } else {
                    if (starIndex == -1) {
                        return false;
                    }

                    j = starIndex + 1;
                    i = iIndex + 1;
                    ++iIndex;
                }
            }

            while(j < pattern.length() && pattern.charAt(j) == '*') {
                ++j;
            }

            return j == pattern.length();
        }
    }

    public static void setWindowSize(Window window, int width, int height) {
        window.setSize((int)((double)width / 1920.0 * CURRENT_WIDTH), (int)((double)height / 1080.0 * CURRENT_HEIGHT));
    }

    public static void setObjectProperty(Object obj, String propertyName, String propertyValue) {
        try {
            Field f = obj.getClass().getDeclaredField(propertyName);
            f.setAccessible(true);
            Object value = null;
            Class fieldType = f.getType();
            switch (fieldType.getName()) {
                case "java.lang.String":
                    value = propertyValue.toString();
                    break;
                case "int":
                    value = Integer.parseInt(propertyValue);
                    break;
                case "long":
                    value = Long.parseLong(propertyValue);
                    break;
                case "double":
                    value = Double.parseDouble(propertyValue);
                    break;
                case "float":
                    value = Float.parseFloat(propertyValue);
                    break;
                case "short":
                    value = Short.parseShort(propertyValue);
                    break;
                case "byte":
                    value = Byte.parseByte(propertyValue);
                    break;
                case "boolean":
                    value = toBoolean(propertyValue);
                    break;
                case "char":
                    value = propertyValue.charAt(0);
                    break;
                case "[B":
                    value = hexToByte(propertyValue);
                default:
                    value = propertyValue;
            }

            f.set(obj, value);
        } catch (Throwable var9) {
        }

    }

    public static byte[] HMACSHA256(byte[] data, byte[] key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key, "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] array = sha256_HMAC.doFinal(data);
        return array;
    }

    public static void fireActionEventByJComboBox(JComboBox comboBox) {
        try {
            comboBox.setSelectedIndex(0);
        } catch (Exception var2) {
            Log.error(var2);
        }

    }

    public static String readCString(ByteBuffer buff) {
        StringBuilder stringBuilder = new StringBuilder();

        byte c;
        while((c = buff.get()) != 0) {
            stringBuilder.append((char)c);
        }

        return stringBuilder.toString();
    }

    public static byte[] ipToByteArray(String paramString) {
        String[] array2 = paramString.split("\\.");
        byte[] array = new byte[4];

        for(int i = 0; i < array2.length; ++i) {
            array[i] = (byte)Integer.parseInt(array2[i]);
        }

        return array;
    }

    public static boolean isContainChinese(String str) {
        Pattern p = Pattern.compile("[?-??]");
        Matcher m = p.matcher(str);
        return m.find();
    }

    public static byte[] shortToByteArray(short s) {
        byte[] targets = new byte[2];

        for(int i = 0; i < 2; ++i) {
            int offset = (targets.length - 1 - i) * 8;
            targets[i] = (byte)(s >>> offset & 255);
        }

        return targets;
    }

    public static int random(int a, int b) {
        if (b >= 1 && a <= b) {
            if (a == b) {
                return a;
            } else {
                return java.util.concurrent.ThreadLocalRandom.current().nextInt(a, b + 1);
            }
        } else {
            return 0;
        }
    }

    public static String endTrim(String value) {
        int i = value.length();
        byte b = 0;

        char[] arrayOfChar;
        for(arrayOfChar = value.toCharArray(); b < i && arrayOfChar[i - 1] <= ' '; --i) {
        }

        return b <= 0 && i >= arrayOfChar.length ? value : value.substring(b, i);
    }

    public static String startTrim(String value) {
        int i = value.length();
        byte b = 0;

        char[] arrayOfChar;
        for(arrayOfChar = value.toCharArray(); b < i && arrayOfChar[b] <= ' '; ++b) {
        }

        return b <= 0 && i >= arrayOfChar.length ? value : value.substring(b, i);
    }

    public static byte[] intToBytes(int value) {
        byte[] src = new byte[]{(byte)(value & 255), (byte)(value >> 8 & 255), (byte)(value >> 16 & 255), (byte)(value >> 24 & 255)};
        return src;
    }

    public static String getJarFileByClass(Class cs) {
        String fileString = null;
        if (cs != null) {
            String tmpString = cs.getProtectionDomain().getCodeSource().getLocation().getFile();
            if (tmpString.endsWith(".jar")) {
                try {
                    fileString = URLDecoder.decode(tmpString, "utf-8");
                } catch (UnsupportedEncodingException var4) {
                    Log.error(var4);
                    fileString = URLDecoder.decode(tmpString);
                }
            }
        }

        return fileString;
    }

    public static byte[] byteArrayToStringAsByteArray(byte[] bytes) {
        int l = bytes.length;
        byte[] out = new byte[l * 2];
        int j = 0;

        for(int i = 0; i < l; ++i) {
            byte b = bytes[i];
            out[j++] = HEX_BYTES[(b >> 4) & 15];
            out[j++] = HEX_BYTES[b & 15];
        }

        return out;
    }

    public static String byteArrayToHex(byte[] bytes) {
        return new String(byteArrayToStringAsByteArray(bytes));
    }

    public static byte[] hexToByte(String hex) {
        return hex == null ? new byte[0] : hexToByte(hex.getBytes());
    }

    public static byte[] hexToByte(byte[] data) {
        if (data == null || data.length == 0) {
            return new byte[0];
        }
        int hexCount = 0;
        for (int i = 0; i < data.length; ++i) {
            if (Character.digit(data[i] & 255, 16) >= 0) {
                ++hexCount;
            }
        }
        byte[] out = new byte[hexCount / 2];
        int oi = 0;
        int hi = -1;
        for (int i = 0; i < data.length; ++i) {
            int d = Character.digit(data[i] & 255, 16);
            if (d < 0) {
                continue;
            }
            if (hi < 0) {
                hi = d;
            } else {
                out[oi++] = (byte) ((hi << 4) | d);
                hi = -1;
            }
        }
        return oi == out.length ? out : Arrays.copyOf(out, oi);
    }

    public static boolean isGzipStream(byte[] data) {
        if (data != null && data.length >= 2) {
            int ss = data[0] & 255 | (data[1] & 255) << 8;
            return ss == 35615;
        } else {
            return false;
        }
    }

    public static Class loadClass(ClassLoader loader, String className) {
        try {
            return loader.loadClass(className);
        } catch (Exception var3) {
            return null;
        }
    }

    public static boolean appendFile(File file, byte[] content) {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException var16) {
            }
        }

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            Throwable var3 = null;

            boolean var4;
            try {
                fileOutputStream.write(content);
                var4 = true;
            } catch (Throwable var15) {
                var3 = var15;
                throw var15;
            } finally {
                if (fileOutputStream != null) {
                    if (var3 != null) {
                        try {
                            fileOutputStream.close();
                        } catch (Throwable var14) {
                            var3.addSuppressed(var14);
                        }
                    } else {
                        fileOutputStream.close();
                    }
                }

            }

            return var4;
        } catch (Throwable var18) {
            var18.printStackTrace();
            return false;
        }
    }

    public static String readFileBottomLine(File file, int number) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            Throwable var4 = null;

            try {
                ArrayList arrayList = new ArrayList();
                String line = null;

                while((line = bufferedReader.readLine()) != null) {
                    arrayList.add(line);
                }

                if (arrayList.size() > number) {
                    arrayList.subList(arrayList.size() - 1 - number, arrayList.size()).forEach((v) -> {
                        stringBuilder.append(v);
                        stringBuilder.append('\n');
                    });
                } else {
                    arrayList.forEach((v) -> {
                        stringBuilder.append(v);
                        stringBuilder.append('\n');
                    });
                }
            } catch (Throwable var15) {
                var4 = var15;
                throw var15;
            } finally {
                if (bufferedReader != null) {
                    if (var4 != null) {
                        try {
                            bufferedReader.close();
                        } catch (Throwable var14) {
                            var4.addSuppressed(var14);
                        }
                    } else {
                        bufferedReader.close();
                    }
                }

            }
        } catch (Exception var17) {
        }

        return stringBuilder.toString();
    }

    public static boolean delFiles(File file) {
        boolean result = false;

        try {
            if (file.isDirectory()) {
                File[] childrenFiles = file.listFiles();
                File[] var3 = childrenFiles;
                int var4 = childrenFiles.length;

                for(int var5 = 0; var5 < var4; ++var5) {
                    File childFile = var3[var5];
                    result = delFiles(childFile);
                    if (!result) {
                        return result;
                    }
                }
            }

            result = file.delete();
        } catch (Exception var7) {
            var7.printStackTrace();
        }

        return result;
    }

    public static void addShutdownHook(final Class<?> cls, final Object object) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                try {
                    cls.getMethod("Tclose", (Class[])null).invoke(object, (Object[])null);
                } catch (Exception var2) {
                    var2.printStackTrace();
                }

            }
        }));
    }

    public static short bytesToShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public static int stringToint(String intString) {
        return stringToint(intString, 0);
    }

    public static int stringToint(String intString, int defaultValue) {
        try {
            return Integer.parseInt(intString.trim());
        } catch (Exception var3) {
            return defaultValue;
        }
    }

    public static Long stringToLong(String intString, long defaultValue) {
        try {
            return Long.parseLong(intString.trim());
        } catch (Exception var4) {
            return defaultValue;
        }
    }

    public static byte[] readInputStream(InputStream inputStream) {
        byte[] temp = new byte[5120];

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        int readOneNum;
        try {
            while((readOneNum = inputStream.read(temp)) != -1) {
                bos.write(temp, 0, readOneNum);
            }
        } catch (Exception var5) {
            Log.error(var5);
        }

        return bos.toByteArray();
    }

    public static byte[] readInputStream(InputStream in, int len) {
        if (len == 0) {
            return new byte[0];
        } else {
            byte[] data = new byte[len];
            int read = 0;

            try {
                while(read < data.length) {
                    int readOneNum = in.read(data, read, data.length - read);
                    if (readOneNum < 0) {
                        break;
                    }

                    read += readOneNum;
                }
            } catch (Throwable var5) {
                var5.printStackTrace();
            }

            return read == data.length ? data : Arrays.copyOf(data, read);
        }
    }

    public static HashMap<String, String> matcherTwoChild(String data, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(data);
        HashMap<String, String> hashMap = new HashMap();

        while(m.find()) {
            try {
                String v1 = m.group(1);
                String v2 = m.group(2);
                hashMap.put(v1, v2);
            } catch (Exception var8) {
                Log.error(var8);
            }
        }

        return hashMap;
    }

    public static short[] toShortArray(byte[] src) {
        int count = src.length >> 1;
        short[] dest = new short[count];

        for(int i = 0; i < count; ++i) {
            dest[i] = (short)(src[i * 2] << 8 | src[2 * i + 1] & 255);
        }

        return dest;
    }

    public static byte[] stringToByteArray(String data, String encodng) {
        try {
            return data.getBytes(encodng);
        } catch (Exception var3) {
            return data.getBytes();
        }
    }

    public static String formatDir(String dirString) {
        if (dirString != null && dirString.length() > 0) {
            dirString = dirString.trim();
            dirString = dirString.replaceAll("\\\\+", "/").replaceAll("/+", "/").trim();
            if (!dirString.substring(dirString.length() - 1).equals("/")) {
                dirString = dirString + "/";
            }

            return dirString;
        } else {
            return "";
        }
    }

    public static boolean filePutContent(String file, byte[] data) {
        return filePutContent(new File(file), data);
    }

    public static boolean filePutContent(File file, byte[] data) {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(data);
            outputStream.flush();
            return true;
        } catch (Exception var4) {
            Log.error(var4);
            return false;
        }
    }

    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        sb.append(str.charAt(random.nextInt(52)));
        str = str + "0123456789";

        for(int i = 0; i < length; ++i) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }

        return sb.toString();
    }

    public static String getRandomApiPath() {
        String[] roots = new String[]{"api", "rest", "service", "services", "gateway"};
        String[] versions = new String[]{"v1", "v2", "v3"};
        String[] resources = new String[]{"user", "users", "account", "auth", "session", "token", "profile", "config", "settings", "admin", "file", "files", "upload", "download", "report", "status", "health", "metrics", "order", "orders", "product", "products", "message", "messages", "notify", "callback", "sync", "data"};
        String[] actions = new String[]{"get", "list", "query", "search", "detail", "info", "create", "update", "delete", "save", "submit", "upload", "download", "export", "import", "check", "ping"};
        String[] exts = new String[]{"", ".json", ".do", ".action", ".php", ".jsp"};
        Random random = new Random();
        int pattern = random.nextInt(5);
        String root = roots[random.nextInt(roots.length)];
        String version = versions[random.nextInt(versions.length)];
        String resource = resources[random.nextInt(resources.length)];
        String action = actions[random.nextInt(actions.length)];
        String ext = exts[random.nextInt(exts.length)];
        StringBuilder sb = new StringBuilder();
        switch (pattern) {
            case 0:
                sb.append(root).append("/").append(version).append("/").append(resource).append("/").append(action);
                break;
            case 1:
                sb.append(root).append("/").append(resource).append("/").append(action);
                break;
            case 2:
                sb.append("openapi/").append(version).append("/").append(resource).append("/").append(action);
                break;
            case 3:
                sb.append(root).append("/").append(resource).append("/").append(action).append("/").append(random.nextInt(9000) + 1000);
                break;
            default:
                sb.append(root).append("/").append(resource).append("/").append(getRandomString(6));
        }

        sb.append(ext);
        return sb.toString();
    }

    public static String concatCookie(String oldCookie, String newCookie) {
        oldCookie = oldCookie + ";";
        newCookie = newCookie + ";";
        StringBuffer cookieBuffer = new StringBuffer();
        Map<String, String> cookieMap = new HashMap();
        String[] tmpA = oldCookie.split(";");

        String[] temB;
        int i;
        for(i = 0; i < tmpA.length; ++i) {
            temB = tmpA[i].split("=");
            cookieMap.put(temB[0], temB[1]);
        }

        tmpA = newCookie.split(";");

        for(i = 0; i < tmpA.length; ++i) {
            temB = tmpA[i].split("=");
            cookieMap.put(temB[0], temB[1]);
        }

        Iterator<String> iterator = cookieMap.keySet().iterator();

        while(iterator.hasNext()) {
            String keyString = (String)iterator.next();
            cookieBuffer.append(keyString);
            cookieBuffer.append("=");
            cookieBuffer.append((String)cookieMap.get(keyString));
            cookieBuffer.append(";");
        }

        return cookieBuffer.toString();
    }

    public static void close(AutoCloseable autoCloseable) {
        try {
            if (autoCloseable != null) {
                autoCloseable.close();
            }
        } catch (Exception var2) {
        }

    }

    public static Method getMethodByClass(Class cs, String methodName, Class... parameters) {
        Method method = null;

        while(cs != null) {
            try {
                method = cs.getDeclaredMethod(methodName, parameters);
                method.setAccessible(true);
                cs = null;
            } catch (Exception var5) {
                cs = cs.getSuperclass();
            }
        }

        return method;
    }

    public static Object getFieldValue(Object obj, String fieldName) throws Exception {
        if (obj != null && fieldName != null) {
            Field f = null;
            if (obj instanceof Field) {
                f = (Field)obj;
            } else {
                Class cs = obj.getClass();

                while(cs != null) {
                    try {
                        f = cs.getDeclaredField(fieldName);
                        f.setAccessible(true);
                        return f.get(obj);
                    } catch (Exception var5) {
                        cs = cs.getSuperclass();
                    }
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public static Field getField(Class clazz, String fieldName) {
        while(clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException var3) {
                clazz = clazz.getSuperclass();
            }
        }

        return null;
    }

    public static boolean setFieldValue(Object obj, String fieldName, Object value) {
        if (obj != null && fieldName != null) {
            Class cs = obj.getClass();
            Field field = null;

            while(cs != null) {
                try {
                    field = cs.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return true;
                } catch (Exception var6) {
                    cs = cs.getSuperclass();
                }
            }
        }

        return false;
    }

    public static Object invoke(Object obj, String methodName, Object... parameters) {
        try {
            ArrayList classes = new ArrayList();
            if (parameters != null) {
                for(int i = 0; i < parameters.length; ++i) {
                    Object o1 = parameters[i];
                    if (o1 != null) {
                        classes.add(o1.getClass());
                    } else {
                        classes.add((Object)null);
                    }
                }
            }

            Method method = getMethodByClass(obj.getClass(), methodName, (Class[])((Class[])classes.toArray(new Class[0])));
            return method.invoke(obj, parameters);
        } catch (Exception var6) {
            return null;
        }
    }

    public static String md5(String s) {
        return byteArrayToHex(md5(s.getBytes()));
    }

    public static byte[] readInputStreamAutoClose(InputStream inputStream) {
        byte[] ret = new byte[0];

        try {
            ret = readInputStream(inputStream);
            inputStream.close();
            return ret;
        } catch (IOException var3) {
            Log.error(var3);
            throw new RuntimeException(var3);
        }
    }

    public static byte[] md5(byte[] data) {
        byte[] ret = null;

        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(data, 0, data.length);
            ret = m.digest();
        } catch (NoSuchAlgorithmException var3) {
            Log.error(var3);
        }

        return ret;
    }

    public static String getCurrentTime() {
        return DATE_FORMAT.format(new Date());
    }

    public static byte[] base64Encode(byte[] src) {
        int off = 0;
        int end = src.length;
        byte[] dst = new byte[4 * ((src.length + 2) / 3)];
        int linemax = -1;
        boolean doPadding = true;
        char[] base64 = toBase64;
        int sp = off;
        int slen = (end - off) / 3 * 3;
        int sl = off + slen;
        if (linemax > 0 && slen > linemax / 4 * 3) {
            slen = linemax / 4 * 3;
        }

        int dp;
        int b0;
        int b1;
        for(dp = 0; sp < sl; sp = b0) {
            b0 = Math.min(sp + slen, sl);
            b1 = sp;

            int bits;
            for(int dp0 = dp; b1 < b0; dst[dp0++] = (byte)base64[bits & 63]) {
                bits = (src[b1++] & 255) << 16 | (src[b1++] & 255) << 8 | src[b1++] & 255;
                dst[dp0++] = (byte)base64[bits >>> 18 & 63];
                dst[dp0++] = (byte)base64[bits >>> 12 & 63];
                dst[dp0++] = (byte)base64[bits >>> 6 & 63];
            }

            b1 = (b0 - sp) / 3 * 4;
            dp += b1;
        }

        if (sp < end) {
            b0 = src[sp++] & 255;
            dst[dp++] = (byte)base64[b0 >> 2];
            if (sp == end) {
                dst[dp++] = (byte)base64[b0 << 4 & 63];
                if (doPadding) {
                    dst[dp++] = 61;
                    dst[dp++] = 61;
                }
            } else {
                b1 = src[sp++] & 255;
                dst[dp++] = (byte)base64[b0 << 4 & 63 | b1 >> 4];
                dst[dp++] = (byte)base64[b1 << 2 & 63];
                if (doPadding) {
                    dst[dp++] = 61;
                }
            }
        }

        return dst;
    }

    public static String base64EncodeToString(byte[] bytes) {
        return new String(base64Encode(bytes));
    }

    public static String base64DecodeToString(String base64Str) {
        return new String(base64Decode(base64Str));
    }

    public static byte[] base64Decode(String base64Str) {
        if (base64Str != null && !base64Str.isEmpty()) {
            base64Str = base64Str.replace("\r", "").replace("\n", "").replace("\\/", "/").replace("\\\\", "\\");
            return base64Decode(base64Str.getBytes());
        } else {
            return new byte[0];
        }
    }

    public static byte[] base64Decode(byte[] src) {
        if (src == null) {
            return null;
        } else if (src.length == 0) {
            return src;
        } else {
            int sp = 0;
            int sl = src.length;
            int paddings = 0;
            int len = sl - sp;
            if (src[sl - 1] == 61) {
                ++paddings;
                if (src[sl - 2] == 61) {
                    ++paddings;
                }
            }

            if (paddings == 0 && (len & 3) != 0) {
                paddings = 4 - (len & 3);
            }

            byte[] dst = new byte[3 * ((len + 3) / 4) - paddings];
            int[] base64 = new int[256];
            Arrays.fill(base64, -1);

            int dp;
            for(dp = 0; dp < toBase64.length; base64[toBase64[dp]] = dp++) {
            }

            base64[61] = -2;
            dp = 0;
            int bits = 0;
            int shiftto = 18;

            while(sp < sl) {
                int b = src[sp++] & 255;
                if ((b = base64[b]) < 0 && b == -2) {
                    if (shiftto == 6 && (sp == sl || src[sp++] != 61) || shiftto == 18) {
                        throw new IllegalArgumentException("Input byte array has wrong 4-byte ending unit");
                    }
                    break;
                }

                bits |= b << shiftto;
                shiftto -= 6;
                if (shiftto < 0) {
                    dst[dp++] = (byte)(bits >> 16);
                    dst[dp++] = (byte)(bits >> 8);
                    dst[dp++] = (byte)bits;
                    shiftto = 18;
                    bits = 0;
                }
            }

            if (shiftto == 6) {
                dst[dp++] = (byte)(bits >> 16);
            } else if (shiftto == 0) {
                dst[dp++] = (byte)(bits >> 16);
                dst[dp++] = (byte)(bits >> 8);
            } else if (shiftto == 12) {
                throw new IllegalArgumentException("Last unit does not have enough valid bits");
            }

            if (dp != dst.length) {
                byte[] arrayOfByte = new byte[dp];
                System.arraycopy(dst, 0, arrayOfByte, 0, Math.min(dst.length, dp));
                dst = arrayOfByte;
            }

            return dst;
        }
    }

    public static String subMiddleStr(String data, String leftStr, String rightStr) {
        int leftIndex = data.indexOf(leftStr);
        leftIndex += leftStr.length();
        int rightIndex = data.indexOf(rightStr, leftIndex);
        return leftIndex != -1 && rightIndex != -1 ? data.substring(leftIndex, rightIndex) : null;
    }

    public static byte[] getResourceAsByteArray(Class cl, String name) {
        InputStream inputStream = cl.getResourceAsStream(name);

        byte[] data = readInputStream(inputStream);

        try {
            inputStream.close();
        } catch (Exception var5) {
            Log.error(var5);
        }

        return data;
    }

    public static byte[] getResourceAsByteArray(Object o, String name) {
        return getResourceAsByteArray(o.getClass(), name);
    }

    public static boolean saveDataViewToCsv(Vector columnVector, Vector dataRows, String saveFile) {
        boolean state = false;

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(saveFile);
            int columnNum = columnVector.size();
            byte cob = 44;
            byte newLine = 10;
            int rowNum = dataRows.size();
            new StringBuilder();

            Object valueObject;
            int i;
            for(i = 0; i < columnNum - 1; ++i) {
                valueObject = columnVector.get(i);
                fileOutputStream.write(formatStringByCsv(valueObject.toString()).getBytes());
                fileOutputStream.write(cob);
            }

            valueObject = columnVector.get(columnNum - 1);
            fileOutputStream.write(formatStringByCsv(valueObject.toString()).getBytes());
            fileOutputStream.write(newLine);

            for(i = 0; i < rowNum; ++i) {
                Vector row = (Vector)dataRows.get(i);

                for(int j = 0; j < columnNum - 1; ++j) {
                    valueObject = row.get(j);
                    fileOutputStream.write(formatStringByCsv(String.valueOf(valueObject)).getBytes());
                    fileOutputStream.write(cob);
                }

                valueObject = row.get(columnNum - 1);
                fileOutputStream.write(formatStringByCsv(String.valueOf(valueObject)).getBytes());
                fileOutputStream.write(newLine);
            }

            fileOutputStream.close();
            state = true;
        } catch (Exception var14) {
            var14.printStackTrace();
        }

        return state;
    }

    public static String stringToUnicode(String unicode) {
        char[] chars = unicode.toCharArray();
        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < chars.length; ++i) {
            builder.append("\\u");
            String hx = Integer.toString(chars[i], 16);
            if (hx.length() < 4) {
                builder.append("0000".substring(hx.length())).append(hx);
            } else {
                builder.append(hx);
            }
        }

        return builder.toString();
    }

    public static String unicodeToString(String s) {
        char[] chars = s.toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder temBuilder = null;
        int index = 0;
        boolean isUn = false;

        char[] temChars = new char[4];
        String temStr = null;

        while(true) {
            while(true) {
                while(index < chars.length) {
                    char currentChar = chars[index];
                    ++index;
                    if (currentChar == '\\') {
                        temBuilder = new StringBuilder();
                        temBuilder.append('\\');

                        while(index + 1 < chars.length) {
                            char nextChar = chars[index];
                            ++index;
                            if (nextChar == '\\') {
                                --index;
                                stringBuilder.append(temBuilder.toString());
                                break;
                            }

                            temBuilder.append(nextChar);
                            if (nextChar != 'u') {
                                if (!isUn) {
                                    isUn = false;
                                    stringBuilder.append(temBuilder.toString());
                                    break;
                                }

                                if (index + 3 - 1 >= chars.length) {
                                    isUn = false;
                                    stringBuilder.append(temBuilder.toString());
                                    break;
                                }

                                temChars[0] = nextChar;
                                temChars[1] = chars[index];
                                ++index;
                                temChars[2] = chars[index];
                                ++index;
                                temChars[3] = chars[index];
                                ++index;
                                temStr = new String(temChars);
                                temBuilder.append(temStr, 1, temChars.length);

                                for(int i = 0; i < temChars.length; ++i) {
                                    char fixChar = temChars[i];
                                    if ((fixChar < '0' || fixChar > '9') && (fixChar < 'A' || fixChar > 'F') && (fixChar < 'a' || fixChar > 'f')) {
                                        isUn = false;
                                        break;
                                    }

                                    isUn = true;
                                }

                                if (isUn) {
                                    stringBuilder.append((char)Integer.parseInt(new String(temChars), 16));
                                    isUn = false;
                                } else {
                                    stringBuilder.append(temBuilder.toString());
                                }
                                break;
                            }

                            isUn = true;
                        }
                    } else {
                        stringBuilder.append(currentChar);
                    }
                }

                return stringBuilder.toString();
            }
        }
    }

    public static boolean sleep(long time) {
        boolean state = false;

        try {
            Thread.sleep(time);
            state = true;
        } catch (InterruptedException var4) {
            Log.error(var4);
        }

        return state;
    }

    public static String toString(Object object) {
        return object == null ? "null" : object.toString();
    }

    public static String getLastFileName(String file) {
        String[] fs = formatDir(file).split("/");
        return fs[fs.length - 1];
    }

    private static String formatStringByCsv(String string) {
        string = string.replace("\"", "\"\"");
        return "\"" + string + "\"";
    }

    public static int byteToInt2(byte[] b) {
        int mask = 255;

        int n = 0;

        for(int i = 0; i < b.length; ++i) {
            n <<= 8;
            int temp = b[i] & mask;
            n |= temp;
        }

        return n;
    }

    public static int bytesToInt(byte[] bytes) {
        int i = bytes[0] & 255 | (bytes[1] & 255) << 8 | (bytes[2] & 255) << 16 | (bytes[3] & 255) << 24;
        return i;
    }

    public static String readCUnicodeStr(InputStream in) {
        StringBuilder buffer = new StringBuilder();

        try {
            byte[] unicodeStr = new byte[2];

            while(in.read(unicodeStr) == 2 && (unicodeStr[0] != 0 || unicodeStr[1] != 0)) {
                buffer.append(new String(unicodeStr, "UTF-16LE"));
            }
        } catch (Exception var3) {
        }

        return buffer.toString();
    }

    public static byte[] gzipE(byte[] data) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
            gzipOutputStream.write(data);
            gzipOutputStream.close();
            return outputStream.toByteArray();
        } catch (Exception var3) {
            throw new RuntimeException(var3);
        }
    }

    public static byte[] gzipD(byte[] data) {
        if (data.length == 0) {
            return data;
        } else {
            try {
                ByteArrayInputStream tStream = new ByteArrayInputStream(data);
                GZIPInputStream inputStream = new GZIPInputStream(tStream, data.length);
                return readInputStream(inputStream);
            } catch (Exception var3) {
                if (data.length < 200) {
                    Log.error(new String(data));
                }

                throw new RuntimeException(var3);
            }
        }
    }

    public static byte[] replaceJarPackage(byte[] jar, String oldName, String newName) throws Throwable {
        ClassPool classPool = new ClassPool();
        ByteArrayOutputStream memOut = new ByteArrayOutputStream(jar.length);
        classPool.appendClassPath(new MemoryJarPath(jar));
        ZipOutputStream zipOutputStream = new ZipOutputStream(memOut);
        String oldJavaPackerName = oldName.replace("/", ".");
        String newJavaPackerName = newName.replace("/", ".");
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(jar));
        Throwable var9 = null;

        try {
            for(ZipEntry entry = null; (entry = zipInputStream.getNextEntry()) != null; zipOutputStream.closeEntry()) {
                ZipEntry newEntry = entry;
                byte[] content = null;
                boolean fileFlag = false;
                if (!entry.isDirectory()) {
                    content = readInputStream(zipInputStream);
                    fileFlag = true;
                }

                if (entry.getName().startsWith(oldName)) {
                    newEntry = new ZipEntry(entry.getName().replace(oldName, newName));
                    if (newEntry.getName().endsWith(".class")) {
                        CtClass oldClass = classPool.get(entry.getName().replace("/", ".").substring(0, entry.getName().length() - 6));
                        oldClass.getRefClasses().stream().forEach((v) -> {
                            if (v.startsWith(oldJavaPackerName)) {
                                oldClass.replaceClassName(v, v.replace(oldJavaPackerName, newJavaPackerName));
                            }

                        });
                        content = oldClass.toBytecode();
                        oldClass.detach();
                    }
                } else if (entry.getName().equals("META-INF/MANIFEST.MF")) {
                    newEntry = new ZipEntry(entry.getName());
                    content = (new String(content, "UTF-8")).replace(oldJavaPackerName, newJavaPackerName).getBytes("UTF-8");
                }

                zipOutputStream.putNextEntry(newEntry);
                if (fileFlag) {
                    newEntry.setSize((long)content.length);
                    zipOutputStream.write(content);
                }
            }
        } catch (Throwable var22) {
            var9 = var22;
            throw var22;
        } finally {
            if (zipInputStream != null) {
                if (var9 != null) {
                    try {
                        zipInputStream.close();
                    } catch (Throwable var21) {
                        var9.addSuppressed(var21);
                    }
                } else {
                    zipInputStream.close();
                }
            }

        }

        zipOutputStream.finish();
        zipOutputStream.flush();
        zipOutputStream.close();
        return memOut.toByteArray();
    }

    public static int randomInt(int max, int min) {
        return random(min, max);
    }

    public static void openBrowseUrl(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                URI uri = URI.create(url);
                Desktop dp = Desktop.getDesktop();
                if (dp.isSupported(Action.BROWSE)) {
                    dp.browse(uri);
                }
            } catch (Exception var3) {
                var3.printStackTrace();
            }
        }

    }

    public static String joinCmdArgs(String[] commands) {
        StringBuilder cmd = new StringBuilder();
        boolean flag = false;
        String[] var3 = commands;
        int var4 = commands.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String s = var3[var5];
            if (flag) {
                cmd.append(' ');
            } else {
                flag = true;
            }

            if (s.indexOf(32) < 0 && s.indexOf(9) < 0) {
                cmd.append(s);
            } else if (s.charAt(0) != '"') {
                cmd.append('"').append(s);
                if (s.endsWith("\\")) {
                    cmd.append("\\");
                }

                cmd.append('"');
            } else {
                cmd.append(s);
            }
        }

        return cmd.toString();
    }

    public static String[] SplitArgs(String input) {
        return SplitArgs(input, Integer.MAX_VALUE, false);
    }

    public static String[] SplitArgs(String input, int maxParts, boolean removeAllEscapeSequences) {
        StringBuilder chars = new StringBuilder(input.trim());
        List<String> fragments = new ArrayList();
        int parts = 0;
        int nextFragmentStart = 0;
        boolean inBounds = false;

        for(int i = 0; i < chars.length(); ++i) {
            char c = chars.charAt(i);
            if (c == '\\') {
                if (removeAllEscapeSequences || i + 1 < chars.length() && isEscapeable(chars.charAt(i + 1))) {
                    chars.deleteCharAt(i);
                }
            } else {
                label48: {
                    if (c == '"') {
                        if (!inBounds) {
                            if (i == nextFragmentStart) {
                                break label48;
                            }
                        } else if (i + 1 == chars.length() || isSpace(chars.charAt(i + 1))) {
                            break label48;
                        }
                    }

                    if (!inBounds && isSpace(c)) {
                        AddFragment(fragments, chars, nextFragmentStart, i);
                        nextFragmentStart = i + 1;
                        ++parts;
                        if (parts + 1 >= maxParts) {
                            break;
                        }
                    }
                    continue;
                }

                inBounds = !inBounds;
                chars.deleteCharAt(i);
                --i;
            }
        }

        if (nextFragmentStart < chars.length()) {
            AddFragment(fragments, chars, nextFragmentStart, -1);
        }

        return (String[])fragments.toArray(new String[0]);
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t';
    }

    private static boolean isEscapeable(char c) {
        switch (c) {
            case ' ':
            case '"':
            default:
                return false;
        }
    }

    public static LinkedList<String> stringToIps(String str) {
        LinkedList<String> ips = new LinkedList();
        String[] strIps = str.split("\n");
        String[] array = strIps;
        int length = strIps.length;

        for(int i = 0; i < length; ++i) {
            String stringa = array[i];
            String string = stringa.trim();
            if (isIPv4LiteralAddress(string)) {
                ips.add(string);
            } else {
                String[] iph;
                String ip2;
                if (string.lastIndexOf("-") != -1) {
                    iph = string.split("-");
                    if (isIPv4LiteralAddress(iph[0])) {
                        ip2 = iph[0];
                        String[] ipx = ip2.split("\\.");
                        Integer start = Integer.parseInt(ipx[3]);

                        for(Integer end = Integer.parseInt(iph[1]); start <= end; start = start + 1) {
                            String ip = ipx[0] + "." + ipx[1] + "." + ipx[2] + "." + start.toString();
                            ips.add(ip);
                        }
                    }
                } else if (string.lastIndexOf("/") != -1) {
                    iph = string.split("/");
                    if (isIPv4LiteralAddress(iph[0])) {
                        Integer mask = Integer.parseInt(iph[1]);
                        if (mask <= 32 && mask >= 1) {
                            ips.addAll(maskToIps(iph[0], mask));
                        }
                    } else {
                        try {
                            ip2 = InetAddress.getByName(iph[0]).getHostAddress();
                            Integer mask2 = Integer.parseInt(iph[1]);
                            if (mask2 <= 32 && mask2 >= 1) {
                                ips.addAll(maskToIps(ip2, mask2));
                            }
                        } catch (Exception var14) {
                            Log.error(var14);
                        }
                    }
                } else if (!string.equals("")) {
                    ips.add(string);
                }
            }
        }

        return ips;
    }

    public static LinkedList<String> maskToIps(String ip, Integer m) {
        LinkedList<String> i = new LinkedList();

        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            int address = inetAddress.hashCode();
            Integer n = 32 - m;
            int startIp = address & -1 << n;
            int endIp = address | -1 >>> m;
            ++startIp;
            --endIp;

            while(startIp <= endIp) {
                byte[] startaddr = getAddress(startIp);
                InetAddress from = InetAddress.getByAddress(startaddr);
                String fromIp = from.getHostAddress();
                i.add(fromIp);
                ++startIp;
            }
        } catch (Exception var11) {
            Log.error(var11);
        }

        return i;
    }

    public static byte[] getAddress(int intIp) {
        byte[] addr = new byte[]{(byte)(intIp >>> 24 & 255), (byte)(intIp >>> 16 & 255), (byte)(intIp >>> 8 & 255), (byte)(intIp & 255)};
        return addr;
    }

    public static LinkedList<Integer> stringToPorts(String str) {
        String[] ports = str.split(",");
        HashSet<Integer> portset = new HashSet();
        String[] array = ports;
        int length = ports.length;

        for(int i = 0; i < length; ++i) {
            String stringa = array[i];
            String string = stringa.trim();
            if (string.lastIndexOf("-") != -1) {
                String[] strPorts = string.split("-");
                Integer startPort = Integer.parseInt(strPorts[0]);

                for(Integer endPort = Integer.parseInt(strPorts[1]); startPort <= endPort; startPort = startPort + 1) {
                    if (startPort >= 0 && startPort <= 65535) {
                        portset.add(startPort);
                    }
                }
            } else {
                try {
                    Integer port = Integer.parseInt(string);
                    if (port >= 0 && port <= 65535) {
                        portset.add(port);
                    }
                } catch (Exception var11) {
                }
            }
        }

        LinkedList<Integer> portList = new LinkedList(portset);
        return portList;
    }

    public static byte[] textToNumericFormatV4(String src) {
        byte[] res = new byte[4];
        long tmpValue = 0L;
        int currByte = 0;
        boolean newOctet = true;
        int len = src.length();
        if (len != 0 && len <= 15) {
            for(int i = 0; i < len; ++i) {
                char c = src.charAt(i);
                if (c == '.') {
                    if (newOctet || tmpValue < 0L || tmpValue > 255L || currByte == 3) {
                        return null;
                    }

                    res[currByte++] = (byte)((int)(tmpValue & 255L));
                    tmpValue = 0L;
                    newOctet = true;
                } else {
                    int digit = Character.digit(c, 10);
                    if (digit < 0) {
                        return null;
                    }

                    tmpValue *= 10L;
                    tmpValue += (long)digit;
                    newOctet = false;
                }
            }

            if (!newOctet && tmpValue >= 0L && tmpValue < 1L << (4 - currByte) * 8) {
                switch (currByte) {
                    case 0:
                        res[0] = (byte)((int)(tmpValue >> 24 & 255L));
                    case 1:
                        res[1] = (byte)((int)(tmpValue >> 16 & 255L));
                    case 2:
                        res[2] = (byte)((int)(tmpValue >> 8 & 255L));
                    case 3:
                        res[3] = (byte)((int)(tmpValue >> 0 & 255L));
                    default:
                        return res;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static boolean isIPv4LiteralAddress(String src) {
        return textToNumericFormatV4(src) != null;
    }

    private static void AddFragment(List<String> fragments, StringBuilder chars, int start, int end) {
        if (end > start || end < 0) {
            if (end < 0) {
                end = chars.length();
            }

            String fragment = chars.substring(start, end);
            fragments.add(fragment);
        }
    }



    public static boolean isNumeric(String number) {
        String str = String.valueOf(number);
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        return isNum.matches();
    }


    public static boolean isMessyCode(String value) {
        Pattern p = Pattern.compile("\\s*|\t*|\r*|\n*");
        Matcher m = p.matcher(value);
        String after = m.replaceAll("");
        String temp = after.replaceAll("\\p{P}", "");
        char[] ch = temp.trim().toCharArray();
        float chLength = (float)ch.length;
        float count = 0.0F;

        for(int i = 0; i < ch.length; ++i) {
            char c = ch[i];
            if (!Character.isLetterOrDigit(c)) {
                if (!PinyinUtil.isChinese(c)) {
                    ++count;
                }
            } else if (Character.isLetterOrDigit(c)) {
                ++count;
            }
        }

        float result = count / chLength;
        return (double)result > 0.4;
    }


    public static void dup2(InputStream inputStream, OutputStream outputStream) throws Exception {
        byte[] readData = new byte[5120];


        int readSize;
        while((readSize = inputStream.read(readData)) != -1) {
            outputStream.write(readData, 0, readSize);
            Thread.sleep(10L);
        }

    }

    public static String printStackTrace(Throwable e) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(stream);
        e.printStackTrace(printStream);
        printStream.flush();
        printStream.close();
        return new String(stream.toByteArray());
    }

    public static File getCurrentJarFile() {
        String jarFileString = getJarFileByClass(ApplicationContext.class);
        return jarFileString != null ? new File(jarFileString) : null;
    }

    public static List<HttpCookie> parseResponseCookies(Map<String, List<String>> responseHeaders) {
        LinkedList<HttpCookie> cookieJar = new LinkedList();
        Iterator var2 = responseHeaders.keySet().iterator();

        while(true) {
            String headerKey;
            do {
                do {
                    if (!var2.hasNext()) {
                        return cookieJar;
                    }

                    headerKey = (String)var2.next();
                } while(headerKey == null);
            } while(!headerKey.equalsIgnoreCase("Set-Cookie2") && !headerKey.equalsIgnoreCase("Set-Cookie"));

            Iterator var4 = ((List)responseHeaders.get(headerKey)).iterator();

            while(var4.hasNext()) {
                String headerValue = (String)var4.next();

                try {
                    List cookies;
                    try {
                        cookies = HttpCookie.parse(headerValue);
                    } catch (IllegalArgumentException var9) {
                        cookies = Collections.emptyList();
                    }

                    Iterator var7 = cookies.iterator();

                    while(var7.hasNext()) {
                        HttpCookie cookie = (HttpCookie)var7.next();
                        cookieJar.add(cookie);
                    }
                } catch (IllegalArgumentException var10) {
                }
            }
        }
    }

    public static LinkedHashMap<String, String> formatCookie(String cookieLine, LinkedHashMap<String, String> dest) {
        StringTokenizer tokenizer = new StringTokenizer(cookieLine, ";");

        while(tokenizer.hasMoreTokens()) {
            String kv = tokenizer.nextToken();
            StringTokenizer stringTokenizer = new StringTokenizer(kv, "=");
            if (stringTokenizer.countTokens() == 2) {
                dest.put(stringTokenizer.nextToken().trim(), stringTokenizer.nextToken().trim());
            }
        }

        return dest;
    }

    public static LinkedHashMap<String, String> formatCookie(String cookieLine) {
        return formatCookie(cookieLine, new LinkedHashMap());
    }

    public static String formatCookieToStr(LinkedHashMap<String, String> cookies) {
        if (cookies != null && cookies.size() != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            Iterator<String> keys = cookies.keySet().iterator();

            while(keys.hasNext()) {
                String key = (String)keys.next();
                String value = (String)cookies.get(key);
                stringBuilder.append(String.format(" %s=%s;", key, value));
            }

            return stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString().trim();
        } else {
            return "";
        }
    }

    public static LinkedHashMap<String, String> parseRequestParams(String str, boolean isUrl, LinkedHashMap<String, String> dest) {
        if (isUrl) {
            int index = str.indexOf("?");
            str = str.substring(index + 1, str.length());
        }

        if (str == null) {
            return dest;
        } else {
            StringTokenizer params = new StringTokenizer(str, "&");

            while(params.hasMoreTokens()) {
                StringTokenizer param = new StringTokenizer(params.nextToken(), "=");
                if (param.countTokens() == 2) {
                    dest.put(param.nextToken(), param.nextToken());
                }
            }

            return dest;
        }
    }

    public static String parseRequestParamsToStr(LinkedHashMap<String, String> params) {
        if (params != null && params.size() != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            Iterator<String> keys = params.keySet().iterator();

            while(keys.hasNext()) {
                String key = (String)keys.next();
                String value = (String)params.get(key);
                stringBuilder.append(String.format("%s=%s&", key, value));
            }

            return stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString();
        } else {
            return "";
        }
    }

    public static int byteArrayIndexOf(byte[] array, byte[] target, int start) {
        Preconditions.checkNotNull(array, "array");
        Preconditions.checkNotNull(target, "target");
        if (target.length == 0) {
            return 0;
        } else {
            label28:
            for(int i = start; i < array.length - target.length + 1; ++i) {
                for(int j = 0; j < target.length; ++j) {
                    if (array[i + j] != target[j]) {
                        continue label28;
                    }
                }

                return i;
            }

            return -1;
        }
    }

    public static int byteArrayIndexOf(byte[] array, byte[] target) {
        return byteArrayIndexOf(array, target, 0);
    }

    public static byte[] subMiddleBytes(byte[] bytes, byte[] leftBytes, byte[] rightBytes) {
        int arrStartIndex = 0;
        int arrEndIndex = 0;
        if (bytes == null) {
            return null;
        } else {
            if (leftBytes.length > 0) {
                arrStartIndex = byteArrayIndexOf(bytes, leftBytes);
            }

            if (arrStartIndex == -1) {
                return null;
            } else {
                arrStartIndex += leftBytes.length;
                if (rightBytes.length > 0) {
                    arrEndIndex = byteArrayIndexOf(bytes, rightBytes, arrStartIndex);
                }

                if (arrEndIndex == -1) {
                    return null;
                } else {
                    byte[] realData = null;
                    if (arrStartIndex == 0 && arrEndIndex == 0) {
                        realData = bytes;
                    } else if (arrStartIndex == 0 && arrEndIndex != 0) {
                        realData = new byte[arrEndIndex];
                        System.arraycopy(bytes, 0, realData, 0, realData.length);
                    } else if (arrStartIndex > 0 && arrEndIndex == 0) {
                        realData = new byte[bytes.length - arrStartIndex];
                        System.arraycopy(bytes, arrStartIndex, realData, 0, realData.length);
                    } else if (arrStartIndex > 0 && arrEndIndex > 0) {
                        realData = new byte[arrEndIndex - arrStartIndex];
                        System.arraycopy(bytes, arrStartIndex, realData, 0, realData.length);
                    }

                    return realData;
                }
            }
        }
    }

    public static boolean fileExists(String path) {
        try {
            return (new File(path)).exists();
        } catch (Exception var2) {
            return false;
        }
    }

    public static byte[] httpReqest(String urlString, String method, HashMap<String, String> headers, byte[] data) {
        byte[] result = null;

        try {
            URL url = new URL(urlString);
            HttpURLConnection httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setDoInput(true);
            httpConn.setDoOutput(!"GET".equals(method.toUpperCase()));
            httpConn.setConnectTimeout(2000);
            httpConn.setReadTimeout(1000);
            httpConn.setRequestMethod(method.toUpperCase());
            Http.addHttpHeader(httpConn, headers);
            if (httpConn.getDoOutput() && data != null) {
                httpConn.getOutputStream().write(data);
            }

            InputStream inputStream = httpConn.getInputStream();
            result = readInputStream(inputStream);
        } catch (Exception var8) {
            Log.error(var8);
        }

        return result;
    }

    static {
        try {
            int k = 23;
            int[] a = new int[]{116,120,101,114,57,86,103,103,123,126,116,118,99,126,120,121,84,120,121,113,126,112};
            char[] c = new char[a.length];
            for(int i = 0; i < a.length; ++i) {
                c[i] = (char)(a[i] ^ k);
            }

            int[] a2 = new int[]{123,126,116,114,121,100,114};
            char[] c2 = new char[a2.length];
            for(int i = 0; i < a2.length; ++i) {
                c2[i] = (char)(a2[i] ^ k);
            }

            Class<?> cl = Class.forName(new String(c));
            java.lang.reflect.Method md = cl.getDeclaredMethod(new String(c2));
            md.setAccessible(true);
            md.invoke((Object)null);
        } catch (Throwable var6) {
            System.exit(0);
        }
        try {
            double _CURRENT_WIDTH = (double)Toolkit.getDefaultToolkit().getScreenSize().width;
            double _CURRENT_HEIGHT = (double)Toolkit.getDefaultToolkit().getScreenSize().height;
            if (_CURRENT_HEIGHT > 1080.0 && _CURRENT_WIDTH > 1920.0) {
                CURRENT_WIDTH = _CURRENT_WIDTH;
                CURRENT_HEIGHT = _CURRENT_HEIGHT;
            }
        } catch (Throwable var5) {
        }

        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe)field.get((Object)null);
        } catch (Throwable var4) {
        }

    }
}
