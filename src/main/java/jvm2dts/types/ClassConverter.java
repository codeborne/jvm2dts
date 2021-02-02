package jvm2dts.types;

import jvm2dts.ToTypeScriptConverter;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.*;

import static jvm2dts.NameConverter.getName;
import static jvm2dts.TypeNameToTSMap.getTSType;

public class ClassConverter implements ToTypeScriptConverter {

  static final char[] ALPHABET = "TUVWYXYZABCDEFGHIJKLMNOPQRS".toCharArray();

  public String convert(Class<?> clazz) {
    return convert(clazz, new HashMap<>());
  }

  public String convert(Class<?> clazz, Map<String, String> castMap) {

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
            String annotation = activeAnnotations.get(field.getName());

            output.append(field.getName());
            if (annotation != null && annotation.matches("(.*)Nullable;")) {
              output.append("?");
            }

            output.append(": ");
            if (Map.class.isAssignableFrom(field.getType()))
              output.append(readAsMapGeneric(parameterTypes, castMap));
            else if (!(parameterTypes[0] instanceof WildcardType)) {
              String typeName = castMap.getOrDefault(
                getName((Class<?>) parameterTypes[0]),
                getTSType((Class<?>) parameterTypes[0]));

              output.append(typeName).append("[]");
            } else {
              output.append(castMap.getOrDefault(
                getName(field.getType()),
                getTSType(field.getType())));
              output.append("<");
              for (int j = 0; j < parameterTypes.length; j++) {
                output.append(ALPHABET[j % ALPHABET.length]);
                if (j < parameterTypes.length - 1) {
                  output.append(",");
                }
              }
              output.append(">");
            }
          } catch (ClassCastException e) {
            String annotation = activeAnnotations.get(field.getName());

            output.append(field.getName());
            if (annotation != null && annotation.matches("(.*)Nullable;"))
              output.append("?");

            output.append(": ").append(castMap.getOrDefault(
              getName(field.getType()),
              getTSType(field.getType())));
          }

          output.append(";");
          if (i + 1 < fields.length)
            output.append(" ");
        }
      } catch (Exception e) {
        logger.warning(
          e.getMessage() + System.lineSeparator() + Arrays.toString(e.getStackTrace())
            .substring(1).replace(", ", System.lineSeparator())
        );
      }

      output.append("}");
      return output.toString();
    }

    return "";
  }

  public String readAsMapGeneric(Type[] parameterTypes, Map<String, String> castMap) {
    StringBuilder output = new StringBuilder();

    output.append("{");
    for (int j = 0; j < parameterTypes.length; j = +2) {
      Type value = parameterTypes[j + 1];
      output.append("[key: string]: ");
      if (value instanceof ParameterizedType) {
        output.append(
          readAsMapGeneric(
            ((ParameterizedType) value).getActualTypeArguments(), castMap
          )
        );
      } else
        output.append(
          castMap.getOrDefault(
            getName((Class<?>) value),
            getTSType((Class<?>) value)
          )
        );
    }
    output.append("}");

    return output.toString();
  }
}
