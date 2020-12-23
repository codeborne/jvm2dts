package jvm2dts;

import jvm2dts.types.EnumConverter;

public class Converter implements ToTypeScriptConverter {
  final EnumConverter enumConverter = new EnumConverter();
  final jvm2dts.types.ClassConverter classConverter = new jvm2dts.types.ClassConverter();

  public String convert(Class<?> clazz) {
    if (clazz.isAnnotation() || clazz.getSimpleName().isEmpty())
      return "";
    else if (clazz.isEnum()) {
      return enumConverter.convert(clazz);
    } else {
      return classConverter.convert(clazz);
    }
  }
}
