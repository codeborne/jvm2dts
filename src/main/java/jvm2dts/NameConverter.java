package jvm2dts;

public class NameConverter {
  public static String getName(Class<?> clazz) {
    if (!clazz.isEnum())
      if (clazz.getComponentType() == null || !clazz.getComponentType().isEnum())
        return clazz.getSimpleName();
    String name = clazz.getName();
    name = name.substring(name.lastIndexOf('.') + 1).replace("$", "").replace(";", "");
    return name;
  }
}
