package jvm2dts.types;

import jvm2dts.ToTypeScriptConverter;

import java.util.logging.Level;

import static jvm2dts.NameConverter.convertName;

public class EnumConverter implements ToTypeScriptConverter {
  @Override public String convert(Class<?> clazz) {
    StringBuilder output = new StringBuilder("enum ").append(convertName(clazz)).append(" {");

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
      logger.log(Level.WARNING, "Converting Enum failed: " + clazz, e);
    }

    output.append("}");

    return output.toString();
  }
}
