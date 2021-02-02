package jvm2dts;

import jvm2dts.types.EnumConverter;

import java.util.HashMap;
import java.util.Map;

public class Converter implements ToTypeScriptConverter {
  final EnumConverter enumConverter = new EnumConverter();
  final jvm2dts.types.ClassConverter classConverter = new jvm2dts.types.ClassConverter();

  public String convert(Class<?> clazz) {
    return convert(clazz, new HashMap<>());
  }

  public String convert(Class<?> clazz, Map<String, String> castMap) {
    if (clazz.isAnnotation() || clazz.getSimpleName().isEmpty())
      return "";
    else if (clazz.isEnum()) {
      return enumConverter.convert(clazz, castMap);
    } else {
      return classConverter.convert(clazz, castMap);
    }
  }
}
