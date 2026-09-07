//



// Source code recreated from a .class file by IntelliJ IDEA



// (powered by FernFlower decompiler)



//







package core;







import com.formdev.flatlaf.demo.intellijthemes.IJThemeInfo;



import com.httpProxy.server.CertUtil;



import core.annotation.CryptionAnnotation;



import core.annotation.PayloadAnnotation;



import core.annotation.PluginAnnotation;



import core.c2profile.C2Profile;



import core.c2profile.C2ProfileLoader;



import core.imp.Cryption;



import core.imp.Payload;



import core.imp.Plugin;



import core.shell.ShellEntity;



import core.ui.MainActivity;



import core.ui.component.RTextArea;



import java.awt.Font;



import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;



import java.io.ByteArrayInputStream;



import java.io.File;



import java.io.FileInputStream;



import java.io.FileNotFoundException;



import java.io.FileOutputStream;



import java.io.IOException;



import java.io.InputStream;
import java.io.UnsupportedEncodingException;



import java.lang.annotation.Annotation;



import java.net.Proxy;



import java.net.URL;



import java.security.KeyPair;



import java.security.PrivateKey;



import java.security.cert.X509Certificate;



import java.security.spec.PKCS8EncodedKeySpec;



import java.util.ArrayList;



import java.util.Arrays;



import java.util.Date;



import java.util.Enumeration;



import java.util.HashMap;



import java.util.Iterator;



import java.util.LinkedHashMap;



import java.util.LinkedList;



import java.util.List;



import java.util.Set;



import java.util.concurrent.CopyOnWriteArrayList;



import java.util.concurrent.TimeUnit;



import java.util.jar.JarEntry;



import java.util.jar.JarFile;



import javax.swing.JFrame;



import javax.swing.JLabel;



import javax.swing.UIManager;



import javax.swing.plaf.FontUIResource;



import util.Log;



import util.functions;







public class ApplicationContext {



    public static final String VERSION = "4.15";

    /** GSL release tag, checked against GitHub latest for update notification. */
    public static final String RELEASE_TAG = "3.1.7.1";



    public static final int JAVA_VERSION = (int)(Float.parseFloat(System.getProperty("java.class.version")) - 44.0F);



    private static final HashMap<String, Class<?>> payloadMap = new HashMap();



    private static final LinkedHashMap<String, LinkedHashMap<String, Class<?>>> cryptionMap = new LinkedHashMap();



    private static final HashMap<String, HashMap<String, Class<?>>> pluginMap = new HashMap();



    private static File[] pluginJarFiles;



    private static final String C2PROFILE_SUFFIX = "profile";



    public static int windowWidth = 0;



    public static int windowsHeight = 0;



    public static boolean asyncClick = true;



    public static ThreadLocal<Boolean> isShowHttpProgressBar = new ThreadLocal();



    public static final CoreClassLoader PLUGIN_CLASSLOADER = new CoreClassLoader(Thread.currentThread().getContextClassLoader());



    public static boolean easterEgg = true;



    private static FontUIResource font;



    public static Font systemDefaultFont;



    private static LinkedHashMap<String, LinkedList<String>> headerMap;



    public static final byte[] NULL_BYTES = null;







    protected ApplicationContext() {



    }







    public static void init() {



        initFont();



        try {
            RTextArea.initialized();
        } catch (Throwable ignored) {
        }



        initHttpHeader();



        scanPluginJar();



        scanPayload();



        scanCryption();



        scanPlugin();



        // \u6ce8\u518c\u7cfb\u7edf\u7ea7\u63d2\u4ef6(MCP\u670d\u52a1\u7b49)






    }















    private static void initFont() {



        String fontName = Db.getSetingValue("font-name");



        String fontType = Db.getSetingValue("font-type");



        String fontSize = Db.getSetingValue("font-size");



        Font baseFont = null;



        if (fontName != null && fontType != null && fontSize != null) {



            baseFont = new Font(fontName, Integer.parseInt(fontType), Integer.parseInt(fontSize));



        } else {



            baseFont = systemDefaultFont;



        }



        font = new FontUIResource(ensureCjkFont(baseFont));



        InitGlobalFont(font);



        if (!isHeadlessMode()) {
            MainActivity.setPluginMenuFont(font);
        }



    }



    private static final String CJK_TEST_CHARS = "中文测试口";



    private static Font ensureCjkFont(Font baseFont) {



        if (baseFont == null) {



            baseFont = Font.decode("default");



        }



        try {



            if (baseFont.canDisplayUpTo(CJK_TEST_CHARS) == -1) {



                return baseFont;



            }



            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();



            String[] families = ge.getAvailableFontFamilyNames();



            for (String family : families) {



                Font candidate = new Font(family, baseFont.getStyle(), baseFont.getSize());



                if (candidate.canDisplayUpTo(CJK_TEST_CHARS) == -1) {



                    return candidate;



                }



            }



            Font embedded = loadEmbeddedCjkFont();



            if (embedded != null) {



                return embedded.deriveFont(baseFont.getStyle(), baseFont.getSize());



            }



        } catch (Throwable ignored) {



        }



        return baseFont;



    }



    private static Font loadEmbeddedCjkFont() {



        InputStream in = ApplicationContext.class.getResourceAsStream("/fonts/wqy-microhei.ttf");



        if (in == null) {



            return null;



        }



        try {



            Font loaded = Font.createFont(Font.TRUETYPE_FONT, in);



            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(loaded);



            return loaded;



        } catch (Throwable ignored) {



            return null;



        } finally {



            try {



                in.close();



            } catch (Throwable ignored) {



            }



        }



    }







    private static void initHttpHeader() {



        String headerString = getGloballHttpHeader();



        if (headerString != null) {



            String[] reqLines = headerString.split("\n");



            headerMap = new LinkedHashMap();







            for(int i = 0; i < reqLines.length; ++i) {



                if (!reqLines[i].trim().isEmpty()) {



                    int index = reqLines[i].indexOf(":");



                    if (index > 1) {



                        String keyName = reqLines[i].substring(0, index).trim();



                        String keyValue = reqLines[i].substring(index + 1).trim();



                        LinkedList<String> values = (LinkedList)headerMap.get(keyName);



                        if (values == null) {



                            values = new LinkedList();



                            headerMap.put(keyName, values);



                        }







                        values.add(keyValue);



                    }



                }



            }



        }







    }







    private static void scanPayload() {



        try {



            URL url = ApplicationContext.class.getResource("/shells/payloads/");



            ArrayList<Class> destList = new ArrayList();



            int loadNum = scanClass(url, "shells.payloads", Payload.class, PayloadAnnotation.class, destList);



            destList.forEach((t) -> {



                try {



                    Annotation annotation = t.getAnnotation(PayloadAnnotation.class);



                    String name = (String)annotation.annotationType().getMethod("Name").invoke(annotation, (Object[])null);



                    payloadMap.put(name, t);



                    cryptionMap.put(name, new LinkedHashMap());



                    pluginMap.put(name, new HashMap());



                } catch (Exception var3) {



                    Log.error(var3);



                }







            });



            Log.log(String.format("load payload success! payloadMaxNum:%s onceLoadPayloadNum:%s", payloadMap.size(), loadNum));



        } catch (Exception var3) {



            Log.error(var3);



        }







    }







    private static void scanCryption() {



        try {



            URL url = ApplicationContext.class.getResource("/shells/cryptions/");



            ArrayList<Class> destList = new ArrayList();



            int loadNum = scanClass(url, "shells.cryptions", Cryption.class, CryptionAnnotation.class, destList);



            int pluginMaxNum = 0;



            destList.forEach((t) -> {



                try {



                    Annotation annotation = t.getAnnotation(CryptionAnnotation.class);



                    String name = (String)annotation.annotationType().getMethod("Name").invoke(annotation, (Object[])null);



                    String payloadName = (String)annotation.annotationType().getMethod("payloadName").invoke(annotation, (Object[])null);



                    LinkedHashMap<String, Class<?>> destMap = (LinkedHashMap)cryptionMap.get(payloadName);



                    if (destMap == null) {



                        cryptionMap.put(payloadName, new LinkedHashMap());



                        destMap = (LinkedHashMap)cryptionMap.get(payloadName);



                    }







                    destMap.put(name, t);



                } catch (Exception var5) {



                    var5.printStackTrace();



                    Log.error(var5);



                }







            });



            Iterator<String> iterator = cryptionMap.keySet().iterator();







            while(iterator.hasNext()) {



                String keyString = (String)iterator.next();



                HashMap<String, Class<?>> map = (HashMap)cryptionMap.get(keyString);



                if (map != null) {



                    pluginMaxNum += map.size();



                }



            }







            Log.log(String.format("load cryption success! cryptionMaxNum:%s onceLoadCryptionNum:%s", pluginMaxNum, loadNum));



        } catch (Exception var7) {



            Log.error(var7);



        }







    }







    private static void scanPlugin() {



        try {



            URL url = ApplicationContext.class.getResource("/shells/plugins/");



            ArrayList<Class> destList = new ArrayList();



            int loadNum = scanClass(url, "shells.plugins", Plugin.class, PluginAnnotation.class, destList);



            int pluginMaxNum = 0;



            destList.forEach((t) -> {



                try {



                    Annotation annotation = t.getAnnotation(PluginAnnotation.class);



                    String name = (String)annotation.annotationType().getMethod("Name").invoke(annotation, (Object[])null);



                    String payloadName = (String)annotation.annotationType().getMethod("payloadName").invoke(annotation, (Object[])null);



                    HashMap<String, Class<?>> destMap = (HashMap)pluginMap.get(payloadName);



                    if (destMap == null) {



                        pluginMap.put(payloadName, new HashMap());



                        destMap = (HashMap)pluginMap.get(payloadName);



                    }







                    destMap.put(name, t);



                } catch (Exception var5) {



                    Log.error(var5);



                }







            });



            Iterator<String> iterator = pluginMap.keySet().iterator();







            while(iterator.hasNext()) {



                String keyString = (String)iterator.next();



                HashMap<String, Class<?>> map = (HashMap)pluginMap.get(keyString);



                if (map != null) {



                    pluginMaxNum += map.size();



                }



            }







            Log.log(String.format("load plugin success! pluginMaxNum:%s onceLoadPluginNum:%s", pluginMaxNum, loadNum));



        } catch (Exception var7) {



            Log.error(var7);



        }







    }







    private static void scanPluginJar() {



        String[] pluginJars = Db.getAllPlugin();



        ArrayList list = new ArrayList();







        for(int i = 0; i < pluginJars.length; ++i) {



            File jarFile = new File(pluginJars[i]);



            if (jarFile.exists() && jarFile.isFile()) {



                addJar(jarFile);



                list.add(jarFile);



            } else {



                Log.error(String.format("PluginJarFile : %s no found", pluginJars[i]));



            }



        }







        pluginJarFiles = (File[])((File[])list.toArray(new File[0]));



        Log.log(String.format("load pluginJar success! pluginJarNum:%s LoadPluginJarSuccessNum:%s", pluginJars.length, pluginJars.length));



    }







    public static List<Class> scanClass(URL url, String packageName, Class<?> parentClass, Class<?> annotationClass) {



        List<Class> destList = new ArrayList();



        scanClass(url, packageName, parentClass, annotationClass, destList);



        return destList;



    }







    public static int scanClass(URL url, String packageName, Class<?> parentClass, Class<?> annotationClass, List<Class> destList) {



        int num = scanClassX(url, packageName, parentClass, annotationClass, destList);







        for(int i = 0; i < pluginJarFiles.length; ++i) {



            File jarFile = pluginJarFiles[i];



            num += scanClassByJar(jarFile, packageName, parentClass, annotationClass, destList);



        }







        if (functions.getCurrentJarFile() == null) {



            Iterator var9 = destList.iterator();







            while(var9.hasNext()) {



                Class clazz = (Class)var9.next();



                System.out.println(clazz.getName().replace(".", "/") + ".class");



            }



        }







        return num;



    }







    public static int scanClassX(URL url, String packageName, Class<?> parentClass, Class<?> annotationClass, List<Class> destList) {



        String jarFileString;



        if ((jarFileString = functions.getJarFileByClass(ApplicationContext.class)) != null) {



            return scanClassByJar(new File(jarFileString), packageName, parentClass, annotationClass, destList);



        } else {



            int addNum = 0;







            try {



                File file = new File(url.toURI());



                File[] file2 = file.listFiles();







                for(int i = 0; i < file2.length; ++i) {



                    File objectFile = file2[i];



                    if (objectFile.isDirectory()) {



                        File[] objectFiles = objectFile.listFiles();







                        for(int j = 0; j < objectFiles.length; ++j) {



                            File objectClassFile = objectFiles[j];



                            if (objectClassFile.getPath().endsWith(".class")) {



                                try {



                                    String objectClassName = String.format("%s.%s.%s", packageName, objectFile.getName(), objectClassFile.getName().substring(0, objectClassFile.getName().length() - ".class".length()));



                                    Class objectClass = Class.forName(objectClassName, true, PLUGIN_CLASSLOADER);



                                    if (parentClass.isAssignableFrom(objectClass) && objectClass.isAnnotationPresent(annotationClass)) {



                                        destList.add(objectClass);



                                        ++addNum;



                                    }



                                } catch (Throwable var19) {



                                    Log.error(var19);



                                }



                            }



                        }



                    }



                }



            } catch (Exception var20) {



                Log.error(var20);



            }







            return addNum;



        }



    }







    private static int scanClassByJar(File srcJarFile, String packageName, Class<?> parentClass, Class<?> annotationClass, List<Class> destList) {



        int addNum = 0;







        try {



            JarFile jarFile = new JarFile(srcJarFile);



            Enumeration<JarEntry> jarFiles = jarFile.entries();



            packageName = packageName.replace(".", "/");







            while(jarFiles.hasMoreElements()) {



                JarEntry jarEntry = (JarEntry)jarFiles.nextElement();



                String name = jarEntry.getName();



                if (name.startsWith(packageName) && name.endsWith(".class")) {



                    name = name.replace("/", ".");



                    name = name.substring(0, name.length() - 6);







                    try {



                        Class objectClass = Class.forName(name, true, PLUGIN_CLASSLOADER);



                        if (parentClass.isAssignableFrom(objectClass) && objectClass.isAnnotationPresent(annotationClass)) {



                            destList.add(objectClass);



                            ++addNum;



                        }



                    } catch (Exception var14) {



                        Log.error(var14);



                    }



                }



            }







            jarFile.close();



        } catch (Exception var15) {



            Log.error(var15);



        }







        return addNum;



    }







    public static String[] getAllPayload() {



        Set<String> keys = payloadMap.keySet();



        return (String[])keys.toArray(new String[0]);



    }







    public static Payload getPayload(String payloadName) {



        Class<?> payloadClass = (Class)payloadMap.get(payloadName);



        Payload payload = null;



        if (payloadClass != null) {



            try {



                payload = (Payload)payloadClass.newInstance();



            } catch (Exception var4) {



                Log.error(var4);



            }



        }







        if (payload == null) {



            throw new UnsupportedOperationException(EasyI18N.getI18nString("\u65e0\u6cd5\u627e\u5230\u8fd9\u4e2a\u6709\u6548\u8f7d\u8377 :%s", new Object[]{payloadName}));



        } else {



            return payload;



        }



    }







    public static Plugin[] getAllPlugin(String payloadName) {



        HashMap<String, Class<?>> pluginSrcMap = (HashMap)pluginMap.get(payloadName);



        ArrayList<Plugin> list = new ArrayList();



        Class<?> payloadClass = (Class)payloadMap.get(payloadName);







        while(payloadClass != null && (payloadClass = payloadClass.getSuperclass()) != null) {



            if (payloadClass != null && payloadClass.isAnnotationPresent(PayloadAnnotation.class)) {



                list.addAll(new CopyOnWriteArrayList(getAllPlugin(payloadClass)));



            }



        }







        if (pluginSrcMap != null) {



            Iterator<String> keys = pluginSrcMap.keySet().iterator();







            while(keys.hasNext()) {



                String cryptionName = (String)keys.next();



                Class<?> pluginClass = (Class)pluginSrcMap.get(cryptionName);



                if (pluginClass != null) {



                    PluginAnnotation pluginAnnotation = (PluginAnnotation)pluginClass.getAnnotation(PluginAnnotation.class);



                    if (pluginAnnotation.payloadName().equals(payloadName)) {



                        try {



                            Plugin plugin = (Plugin)pluginClass.newInstance();



                            list.add(plugin);



                        } catch (Exception var10) {



                            Log.error(var10);



                        }



                    }



                }



            }



        }







        return (Plugin[])list.toArray(new Plugin[0]);



    }







    public static Plugin[] getAllPlugin(Class payloadClass) {



        Annotation annotation = payloadClass.getAnnotation(PayloadAnnotation.class);



        if (annotation != null) {



            PayloadAnnotation payloadAnnotation = (PayloadAnnotation)annotation;



            return getAllPlugin(payloadAnnotation.Name());



        } else {



            return new Plugin[0];



        }



    }







    public static String[] getAllCryption(String payloadName) {



        HashMap<String, Class<?>> cryptionSrcMap = (HashMap)cryptionMap.get(payloadName);



        ArrayList<String> list = new ArrayList();



        if (cryptionSrcMap != null) {



            Iterator<String> keys = cryptionSrcMap.keySet().iterator();







            while(keys.hasNext()) {



                String cryptionName = (String)keys.next();



                Class<?> cryptionClass = (Class)cryptionSrcMap.get(cryptionName);



                if (cryptionClass != null) {



                    CryptionAnnotation cryptionAnnotation = (CryptionAnnotation)cryptionClass.getAnnotation(CryptionAnnotation.class);



                    if (cryptionAnnotation.payloadName().equals(payloadName)) {



                        list.add(cryptionName);



                    }



                }



            }



        }







        return (String[])list.toArray(new String[0]);



    }







    public static Cryption getCryption(String payloadName, String crytionName) {



        HashMap<String, Class<?>> cryptionSrcMap = (HashMap)cryptionMap.get(payloadName);



        if (cryptionSrcMap != null) {



            Class<?> cryptionClass = (Class)cryptionSrcMap.get(crytionName);



            if (cryptionMap != null) {



                CryptionAnnotation cryptionAnnotation = (CryptionAnnotation)cryptionClass.getAnnotation(CryptionAnnotation.class);



                if (cryptionAnnotation.payloadName().equals(payloadName)) {



                    Cryption cryption = null;







                    try {



                        cryption = (Cryption)cryptionClass.newInstance();



                        return cryption;



                    } catch (Exception var7) {



                        Log.error(var7);



                        return null;



                    }



                }



            }



        }







        return null;



    }







    private static void addJar(File jarPath) {



        try {



            PLUGIN_CLASSLOADER.addJar(jarPath.toURI().toURL());



        } catch (Exception var2) {



            Log.error(var2);



        }







    }







    public static void InitGlobalFont(FontUIResource fontRes) {



        font = fontRes;



        Enumeration<Object> keys = UIManager.getDefaults().keys();







        while(keys.hasMoreElements()) {



            Object key = keys.nextElement();



            Object value = UIManager.get(key);



            if (value instanceof FontUIResource) {



                UIManager.put(key, fontRes);



            } else if (value instanceof Font) {



                UIManager.put(key, fontRes);



            }



        }







    }







    public static Proxy getProxy(ShellEntity shellContext) {



        return ProxyT.getProxy(shellContext);



    }







    public static String[] getAllProxy() {



        return ProxyT.getAllProxyType();



    }







    public static String[] getAllEncodingTypes() {



        return Encoding.getAllEncodingTypes();



    }







    public static Font getFont() {



        return font;



    }







    public static void setFont(Font font) {



        Db.updateSetingKV("font-name", font.getName());



        Db.updateSetingKV("font-type", Integer.toString(font.getStyle()));



        Db.updateSetingKV("font-size", Integer.toString(font.getSize()));



    }







    public static void resetFont() {



        Db.removeSetingK("font-name");



        Db.removeSetingK("font-type");



        Db.removeSetingK("font-size");



    }







    public static String getGloballHttpHeader() {



        return Db.getSetingValue("globallHttpHeader", "Cache-Control: max-age=0\n" +



                "Sec-Ch-Ua: \"Chromium\"; v=\"128\", \"Not A Brand\"; v=\"24\", \"Google Chrome\"; v=\"128\"\n" +



                "Sec-Fetch-Site: none\n" +



                "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7\n" +



                "Sec-Ch-Ua-Platform: \"Windows\"\n" +



                "User-Agent: Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.200 Safari/537.36\n" +



                "Sec-Fetch-User: ?1\n" +



                "Sec-Ch-Ua-Mobile: ?0\n" +



                "Sec-Fetch-Mode: navigate\n" +



                "Upgrade-Insecure-Requests: 1\n");



    }







    public static LinkedHashMap<String, LinkedList<String>> getGloballHttpHeaderX() {



        return headerMap;



    }















    public static boolean updateGloballHttpHeader(String header) {



        boolean state = Db.updateSetingKV("globallHttpHeader", header);



        initHttpHeader();



        return state;



    }







    public static boolean isGodMode() {



        return Boolean.valueOf(Db.getSetingValue("godMode"));



    }







    public static boolean setGodMode(boolean state) {



        return Db.updateSetingKV("godMode", String.valueOf(state));



    }







    public static boolean isOpenC(String k) {



        return Boolean.valueOf(Db.getSetingValue(k));



    }







    public static boolean setOpenC(String k, boolean state) {



        return Db.updateSetingKV(k, String.valueOf(state));



    }







    public static boolean isOpenCache() {



        return Db.getSetingBooleanValue("shellOpenCache", true);



    }







    public static boolean setOpenCache(boolean state) {



        return setOpenC("shellOpenCache", state);



    }







    public static boolean saveUi(IJThemeInfo themeInfo) {



        try {



            String resourceNameString = themeInfo.getResourceName();



            String lafClassNameString = themeInfo.getLafClassName();



            if (resourceNameString != null && lafClassNameString == null) {



                Db.updateSetingKV("ui-resourceName", resourceNameString);



                Db.removeSetingK("ui-lafClassName");



            }







            if (lafClassNameString != null && resourceNameString == null) {



                Db.updateSetingKV("ui-lafClassName", lafClassNameString);



                Db.removeSetingK("ui-resourceName");



            }







            return lafClassNameString != null || resourceNameString != null;



        } catch (Exception var3) {



            Log.error(var3);



            return false;



        }



    }







    public static void genHttpsConfig() {



        try {



            KeyPair keyPair = CertUtil.genKeyPair();



            String base64HttpsCert = functions.base64EncodeToString(CertUtil.genCACert("C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=GodzillaHttpsProxy", new Date(), new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3650L)), keyPair).getEncoded());



            String base64HttpsPrivateKey = functions.base64EncodeToString((new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded())).getEncoded());



            Db.addSetingKV("HttpsPrivateKey", base64HttpsPrivateKey);



            Db.addSetingKV("HttpsCert", base64HttpsCert);



        } catch (Exception var3) {



            throw new RuntimeException(var3);



        }



    }







    public static PrivateKey getHttpsPrivateKey() {



        try {



            String base64String = Db.getSetingValue("HttpsPrivateKey");



            if (base64String == null) {



                genHttpsConfig();



            }







            return CertUtil.loadPriKey(functions.base64Decode(Db.getSetingValue("HttpsPrivateKey")));



        } catch (Exception var1) {



            throw new RuntimeException(var1);



        }



    }







    public static X509Certificate getHttpsCert() {



        try {



            String base64String = Db.getSetingValue("HttpsCert");



            if (base64String == null) {



                genHttpsConfig();



            }







            return CertUtil.loadCert(new ByteArrayInputStream(functions.base64Decode(Db.getSetingValue("HttpsCert"))));



        } catch (Exception var1) {



            throw new RuntimeException(var1);



        }



    }







    private static boolean hasProfile() {



        File c2profileDir = new File("profile");



        if (c2profileDir.exists() && !c2profileDir.isDirectory()) {



            c2profileDir.delete();



        }







        if (!c2profileDir.exists()) {



            c2profileDir.mkdirs();



        }







        



        // Auto-generate built-in traffic-template profiles if missing (never overwrite user files)



        ensureBuiltinProfiles(c2profileDir);

return true;



    }







    /** Write bundled profile templates (pngC2/pdfC2/gifC2) into profile/ when absent. */
    private static void ensureBuiltinProfiles(File c2profileDir) {
        String[] names = {"pngC2", "pdfC2", "gifC2"};
        for (String name : names) {
            File target = new File(c2profileDir, name + ".profile");
            if (target.exists() && target.isFile() && target.length() > 0) {
                continue;
            }
            try (java.io.InputStream in = ApplicationContext.class.getResourceAsStream("/core/c2profile/profileTemplate/" + name + ".profile")) {
                if (in == null) {
                    continue;
                }
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(target)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        fos.write(buf, 0, n);
                    }
                }
                Log.log("Auto-generated missing c2profile template: " + name + ".profile");
            } catch (Exception e) {
                Log.error(e);
            }
        }
    }

    public static String[] listC2Profile() {



        return listC2Profile((String)null);



    }







    public static String[] listC2Profile(String targetPayload) {



        ArrayList<String> result = new ArrayList();



        if (hasProfile()) {



            File[] files = (new File("profile")).listFiles();



            File[] var3 = files;



            int var4 = files.length;







            for(int var5 = 0; var5 < var4; ++var5) {



                File file = var3[var5];



                String fileName = file.getName();



                if (fileName.endsWith(".profile") && file.isFile()) {



                    String profileName = fileName.substring(0, fileName.length() - ("profile".length() + 1));



                    if (targetPayload == null) {



                        result.add(profileName);



                    } else {



                        try {



                            C2Profile c2Profile = C2ProfileLoader.testLoadC2Profile(getC2Profile(profileName));



                            if (Arrays.stream(c2Profile.supportPayload).anyMatch((v) -> {



                                return v.equals(targetPayload);



                            })) {



                                result.add(profileName);



                            }



                        } catch (Throwable var10) {



                            var10.getMessage();



                        }



                    }



                }



            }



        }







        return (String[])result.toArray(new String[0]);



    }







    public static String getC2Profile(String name) {



        String profile = null;



        if (hasProfile()) {



            File file = new File(String.format("%s/%s.%s", "profile", name, "profile"));



            if (file.exists() && file.isFile()) {



                try {



                    profile = new String(functions.readInputStreamAutoClose(new FileInputStream(file)), "UTF-8");



                } catch (FileNotFoundException var4) {



                    var4.printStackTrace();



                } catch (UnsupportedEncodingException var5) {



                    throw new RuntimeException(var5);



                }



            }



        }







        return profile;



    }







    public static boolean addC2Profile(String name, String value) {



        boolean flag = false;



        if (hasProfile()) {



            File file = new File(String.format("%s/%s.%s", "profile", name, "profile"));



            if (!file.exists()) {



                try {



                    FileOutputStream fileOutputStream = new FileOutputStream(file);



                    Throwable var5 = null;







                    try {



                        fileOutputStream.write(value.getBytes("UTF-8"));



                        flag = true;



                    } catch (Throwable var16) {



                        var5 = var16;



                        throw var16;



                    } finally {



                        if (fileOutputStream != null) {



                            if (var5 != null) {



                                try {



                                    fileOutputStream.close();



                                } catch (Throwable var15) {



                                    var5.addSuppressed(var15);



                                }



                            } else {



                                fileOutputStream.close();



                            }



                        }







                    }



                } catch (FileNotFoundException var18) {



                    var18.printStackTrace();



                } catch (IOException var19) {



                    var19.printStackTrace();



                }



            }



        }







        return flag;



    }







    public static boolean updateC2Profile(String name, String value) {



        boolean flag = false;



        if (hasProfile()) {



            File file = new File(String.format("%s/%s.%s", "profile", name, "profile"));







            try {



                FileOutputStream fileOutputStream = new FileOutputStream(file);



                Throwable var5 = null;







                try {



                    fileOutputStream.write(value.getBytes("UTF-8"));



                    flag = true;



                    fileOutputStream.flush();



                } catch (Throwable var16) {



                    var5 = var16;



                    throw var16;



                } finally {



                    if (fileOutputStream != null) {



                        if (var5 != null) {



                            try {



                                fileOutputStream.close();



                            } catch (Throwable var15) {



                                var5.addSuppressed(var15);



                            }



                        } else {



                            fileOutputStream.close();



                        }



                    }







                }



            } catch (FileNotFoundException var18) {



                var18.printStackTrace();



            } catch (IOException var19) {



                var19.printStackTrace();



            }



        }







        return flag;



    }







    public static boolean removeC2Profile(String name) {



        boolean flag = false;



        File file = new File(String.format("%s/%s.%s", "profile", name, "profile"));



        if (file.delete()) {



            flag = true;



        }







        return flag;



    }







    public static int getFileExtPixel() {



        return Db.getSetingIntValue("FileExtPixel", 16);



    }







    public static void setFileExtPixel(int newPixel) {



        Db.updateSetingKV("FileExtPixel", String.valueOf(newPixel));



    }







    /** Property-only check so MCP can skip Swing before AWT initializes. */
    public static boolean isHeadlessMode() {
        return Boolean.parseBoolean(System.getProperty("java.awt.headless", "false"));
    }



    static {



        if (!isHeadlessMode()) {
            try {
                windowWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
                windowsHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
            } catch (Throwable var4) {
            }
            try {
                JFrame frame = new JFrame();
                frame.setSize(1, 1);
                JLabel label = new JLabel("\u5b57\u4f53\u521d\u59cb\u5316! (font init!)");
                frame.add(label);
                frame.setDefaultCloseOperation(2);
                frame.setVisible(true);
                functions.sleep(20L);
                frame.setVisible(false);
                frame.dispose();
                systemDefaultFont = label.getFont();
                if (systemDefaultFont == null) {
                    systemDefaultFont = Font.decode("default");
                } else {
                    systemDefaultFont = new Font(systemDefaultFont.getName(), 0, systemDefaultFont.getSize() + 1);
                }
            } catch (Throwable var3) {
            }
        } else {
            try {
                systemDefaultFont = Font.decode("default");
            } catch (Throwable ignored) {
            }
        }



        try {
            init();
        } catch (Throwable var2) {
            var2.printStackTrace();
        }



    }



}



