package jvm2dts;

import jvm2dts.types.ClassConverter;
import jvm2dts.types.EnumConverter;

public class Converter implements ToTypeScriptConverter {
  final EnumConverter enumConverter = new EnumConverter();
  final ClassConverter classConverter;

  public Converter(TypeMapper typeMapper) {
    this.classConverter = new ClassConverter(typeMapper);
  }

  public String convert(Class<?> clazz) {
    if (clazz.isAnnotation() || clazz.getSimpleName().isEmpty())
      return "";
    else if (clazz.isEnum())
      return enumConverter.convert(clazz);
    else
      return classConverter.convert(clazz);
  }
}
