package jvm2dts;

import java.util.HashMap;

public class TypeNameToTSMap {
  private static final HashMap<String, String> typeNameToTS = new HashMap<>();

  static {
    typeNameToTS.put("int", "number");
    typeNameToTS.put("String", "string");
  }

  public static String getTSType(String javaTypeName) {
    return typeNameToTS.getOrDefault(javaTypeName, javaTypeName);
  }
}
