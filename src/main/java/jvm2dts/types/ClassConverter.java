package jvm2dts.types;

import jvm2dts.ToTypeScriptConverter;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static jvm2dts.TypeNameToTSMap.getTSType;

public class ClassConverter implements ToTypeScriptConverter {
  public String convert(Class<?> clazz) {

    final int ASM_API = Opcodes.ASM9;

    StringBuilder output = new StringBuilder("interface ").append(clazz.getSimpleName()).append(" {");
    HashMap<String, String> activeAnnotations = new HashMap<>();
    List<String> activeField = new ArrayList<>();

    class FieldAnnotationAdapter extends FieldVisitor {

      @Override
      public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        activeAnnotations.put(activeField.get(0), descriptor);
        return super.visitAnnotation(descriptor, visible);
      }

      public FieldAnnotationAdapter() {
        super(ASM_API);
      }
    }

    class ClassAdapter extends ClassVisitor {
      public ClassAdapter() {
        super(ASM_API);
      }

      @Override
      public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        // awful hack to access an upper class value inside an inner class
        activeField.clear();
        activeField.add(name);
        return new FieldAnnotationAdapter();
      }
    }

    class ClassAnnotationReader extends ClassReader {
      public ClassAnnotationReader(InputStream in) throws IOException {
        super(in);
      }
    }

    Field[] fields = clazz.getDeclaredFields();
    if (fields.length > 0) {
      try {

        InputStream in = clazz.getClassLoader().getResourceAsStream(clazz.getName().replace(".", "/") + ".class");
        ClassAnnotationReader reader = new ClassAnnotationReader(in);
        reader.accept(new ClassAdapter(), 0);

        for (int i = 0; i < fields.length; i++) {
          Field field = fields[i];
          try {
            ParameterizedType genericType = (ParameterizedType) field.getGenericType();
            Type[] parameterTypes = genericType.getActualTypeArguments();

            output.append(field.getName());

            String annotation = activeAnnotations.get(field.getName());
            if (annotation != null && annotation.matches("(.*)Nullable;")) {
              output.append("?");
            }


            output.append(": ");
            if (parameterTypes.length <= 1) {
              output
                .append(getTSType((Class<?>) parameterTypes[0]))
                .append("[]");
            } else {
              output.append(readWithGenerics(parameterTypes));
            }

          } catch (ClassCastException e) {

            output.append(field.getName());

            String annotation = activeAnnotations.get(field.getName());
            if (annotation != null && annotation.matches("(.*)Nullable;")) {
              output.append("?");
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
        logger.warning(
          e.getMessage() +
            System.lineSeparator() +
            Arrays.toString(e.getStackTrace())
              .substring(1).replace(", ", System.lineSeparator())
        );
      }

      output.append("}");
      return output.toString();
    }

    return "";
  }

  public String readWithGenerics(Type[] parameterTypes) {
    StringBuilder output = new StringBuilder();

    output.append("{");
    for (int j = 0; j < parameterTypes.length; j = +2) {
      Type key = parameterTypes[j];
      Type value = parameterTypes[j + 1];
      output.append("[key: ");
      output.append(getTSType((Class<?>) key));
      output.append("]: ");
      if (value instanceof ParameterizedType) {
        output.append(
          readWithGenerics(
            ((ParameterizedType) value).getActualTypeArguments()
          )
        );
      } else
        output.append(getTSType((Class<?>) value));
    }
    output.append("}");

    return output.toString();
  }
}
