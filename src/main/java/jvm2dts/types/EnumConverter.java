package jvm2dts.types;

import jvm2dts.Converter;

import java.util.logging.Level;
import java.util.logging.Logger;

public class EnumConverter extends Converter {
  Logger logger = Logger.getLogger(this.getClass().getName());

  public String convert(Class<?> clazz) {
    StringBuilder output = new StringBuilder();

    output.append("enum ").append(clazz.getSimpleName()).append(" {");

    try {
      Object[] enumConstants = clazz.getEnumConstants();
      for (int i = 0; i < enumConstants.length; i++) {
        Object field = enumConstants[i];
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
