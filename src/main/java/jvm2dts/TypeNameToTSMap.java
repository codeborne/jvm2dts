package jvm2dts;

import java.util.HashMap;

public class TypeNameToTSMap {
  private static final HashMap<String, String> typeNameToTS = new HashMap<>();

  static {
    typeNameToTS.put("byte", "number");
    typeNameToTS.put("short", "number");
    typeNameToTS.put("int", "number");
    typeNameToTS.put("long", "number");
    typeNameToTS.put("float", "number");
    typeNameToTS.put("double", "number");

    typeNameToTS.put("Byte", "number");
    typeNameToTS.put("Short", "number");
    typeNameToTS.put("Integer", "number");
    typeNameToTS.put("Long", "number");
    typeNameToTS.put("Float", "number");
    typeNameToTS.put("Double", "number");

    typeNameToTS.put("String", "string");
  }

  public static String getTSType(String javaTypeName) {
    return typeNameToTS.getOrDefault(javaTypeName, javaTypeName);
  }
}
