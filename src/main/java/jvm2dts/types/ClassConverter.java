package jvm2dts.types;

import jvm2dts.TypeNameToTSMap;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClassConverter {
  Logger logger = Logger.getLogger(this.getClass().getName());

  public String convert(Class<?> clazz) {
    StringBuilder output = new StringBuilder();

    output.append("interface ").append(clazz.getSimpleName()).append(" {");

    try {
      Field[] fields = clazz.getDeclaredFields();
      for (int i = 0; i < fields.length; i++) {
        Field field = fields[i];
        output
                .append(field.getName())
                .append(": ")
                .append(TypeNameToTSMap.getTSType(field.getType()));

        if (i + 1 < fields.length)
          output.append("; ");
        else
          output.append(";");
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, e.toString());
    }

    output.append("}");

    return output.toString();
  }
}
