package jvm2dts.types;

import jvm2dts.ToTypeScriptConverter;

import java.util.Map;
import java.util.logging.Level;

import static jvm2dts.NameConverter.getName;

public class EnumConverter implements ToTypeScriptConverter {

  public String convert(Class<?> clazz) {
    return convert(clazz, null);
  }

  public String convert(Class<?> clazz, Map<String, String> castMap) {
    StringBuilder output = new StringBuilder("enum ").append(getName(clazz)).append(" {");

    try {
      Object[] enumConstants = clazz.getEnumConstants();
      for (int i = 0; i < enumConstants.length; i++) {
        Object field = enumConstants[i];
        //noinspection rawtypes
        output.append(((Enum) field).name()).append(" = '").append(field).append("'");
        if (i + 1 < enumConstants.length)
          output.append(", ");
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, e.toString());
    }

    output.append("}");

    return output.toString();
  }
}
