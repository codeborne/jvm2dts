package jvm2dts;

public class NameConverter {
  public static String convertName(Class<?> clazz) {
    String name = clazz.getName();
    name = name.substring(name.lastIndexOf('.') + 1).replace("$", "").replace(";", "");
    return name;
  }
}
