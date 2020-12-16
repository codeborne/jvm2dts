package jvm2dts;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Converter {
  Logger logger = Logger.getLogger(this.getClass().getName());

  public String convert(Class<?> clazz) {
    StringBuilder output = new StringBuilder();

    if (clazz.isEnum()) {
      output.append("enum {");
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
    }

    for (Field field : clazz.getFields()) {
      logger.info(field.getName() + " " + field.getType());
    }
    return output.toString();
  }


}
