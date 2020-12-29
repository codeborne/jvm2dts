package jvm2dts.types;

import jvm2dts.ToTypeScriptConverter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static jvm2dts.TypeNameToTSMap.getTSType;

public class ClassConverter implements ToTypeScriptConverter {
  public String convert(Class<?> clazz) {
    StringBuilder output = new StringBuilder("interface ").append(clazz.getSimpleName()).append(" {");

    try {
      Field[] fields = clazz.getDeclaredFields();
      for (int i = 0; i < fields.length; i++) {
        Field field = fields[i];
        Annotation[] annotations = field.getDeclaredAnnotations();
        try {
          ParameterizedType genericType = (ParameterizedType) field.getGenericType();
          Type[] parameterTypes = genericType.getActualTypeArguments();
          output.append(field.getName());

          for (Annotation annotation : annotations) {
            if (annotation.annotationType().getName().matches("Nullable")) {
              output.append("?");
              break;
            }
          }

          output.append(": ");
          if (parameterTypes.length <= 1) {
            output
              .append(getTSType((Class<?>) parameterTypes[0]))
              .append("[]");
          } else {
            output.append("{");
            for (int j = 0; j < parameterTypes.length; j = +2) {
              Type key = parameterTypes[j];
              Type value = parameterTypes[j + 1];
              output
                .append("[key: ")
                .append(getTSType((Class<?>) key))
                .append("]: ")
                .append(getTSType((Class<?>) value));
            }
            output.append("}");
          }
        } catch (ClassCastException e) {
          output.append(field.getName());

          for (Annotation annotation : annotations) {
            if (annotation.annotationType().getName().matches("Nullable")) {
              output.append("?");
              break;
            }
          }
          output.append(": ");
          output.append(getTSType(field.getType()));
        }

        if (i + 1 < fields.length)
          output.append("; ");
        else
          output.append(";");
      }
    } catch (Exception e) {
      logger.warning(e.toString());
    }

    output.append("}");

    return output.toString();
  }
}
