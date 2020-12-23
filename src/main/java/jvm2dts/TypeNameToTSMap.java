package jvm2dts;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import static jvm2dts.NameConverter.getName;

public class TypeNameToTSMap {
  private static final HashMap<Class<?>, String> typeNameToTS = new HashMap<>();

  static {
    typeNameToTS.put(byte.class, "number");
    typeNameToTS.put(short.class, "number");
    typeNameToTS.put(int.class, "number");
    typeNameToTS.put(long.class, "number");
    typeNameToTS.put(float.class, "number");
    typeNameToTS.put(double.class, "number");
    typeNameToTS.put(Number.class, "number");
    typeNameToTS.put(String.class, "string");
    typeNameToTS.put(UUID.class, "string");
    typeNameToTS.put(Date.class, "Date|string");
    typeNameToTS.put(Instant.class, "Date|string");
    typeNameToTS.put(LocalDate.class, "Date|string");
  }

  public static String getTSType(Class<?> javaType) {
    return typeNameToTS.getOrDefault(javaType, typeNameToTS.getOrDefault(javaType.getSuperclass(), getName(javaType)));
  }
}
