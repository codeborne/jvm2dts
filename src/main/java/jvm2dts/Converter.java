package jvm2dts;

import jvm2dts.types.ClassConverter;
import jvm2dts.types.EnumConverter;

public class Converter {
  final EnumConverter enumConverter = new EnumConverter();
  final ClassConverter classConverter = new ClassConverter();

  public String convert(Class<?> clazz) {
    StringBuilder output = new StringBuilder();

    if (clazz.isEnum()) {
      output.append(enumConverter.convert(clazz));
    } else {
      output.append(classConverter.convert(clazz));
    }

    return output.toString();
  }
}
