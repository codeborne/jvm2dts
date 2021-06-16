package jvm2dts.types;

import jvm2dts.ToTypeScriptConverter;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;

import static java.util.logging.Level.SEVERE;
import static jvm2dts.NameConverter.getName;
import static jvm2dts.TypeNameToTSMap.getTSType;

// TODO: seems like having a builder will be more beneficial - allowing more args
public class ClassConverter implements ToTypeScriptConverter {
  static final char[] ALPHABET = "TUVWYXYZABCDEFGHIJKLMNOPQRS".toCharArray();

  public String convert(Class<?> clazz) {
    return convert(clazz, new HashMap<>());
  }

  public String convert(Class<?> clazz, Map<String, String> castMap) {
    final int ASM_API = Opcodes.ASM9;
    StringBuilder output = new StringBuilder("interface ").append(clazz.getSimpleName()).append(" {");

    HashMap<String, ArrayList<String>> activeAnnotations = new HashMap<>();
    ArrayList<String> activeField = new ArrayList<>();

    class FieldAnnotationAdapter extends FieldVisitor {
      @Override
      public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (activeAnnotations.containsKey(activeField.get(0)))
          activeAnnotations.get(activeField.get(0)).add(descriptor);
        else
          activeAnnotations.put(activeField.get(0), new ArrayList<>(Collections.singleton(descriptor)));
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
          StringBuilder fieldBuffer = new StringBuilder();
          Field field = fields[i];

          if (Modifier.isStatic(field.getModifiers())) continue;

          if (i > 0)
            fieldBuffer.append(" ");

          String expectedFieldName = field.getName();
          for (Annotation annotation : field.getDeclaredAnnotations())
            if (annotation.annotationType().getSimpleName().matches("JsonProperty"))
              expectedFieldName = (String) annotation.getClass().getMethod("value").invoke(annotation);

          fieldBuffer.append(expectedFieldName);

          if (!activeAnnotations.isEmpty())
            for (String annotation : activeAnnotations.getOrDefault(field.getName(), new ArrayList<>()))
              if (annotation.matches(".*Nullable;.*"))
                fieldBuffer.append("?");

          fieldBuffer.append(": ");

          boolean isIterable = false;
          try {
            Class<?> fieldType = field.getType();
            StringBuilder typeBuffer = new StringBuilder();

            if (field.getGenericType() instanceof ParameterizedType) {
              ParameterizedType genericType = (ParameterizedType) field.getGenericType();
              Type[] parameterTypes = genericType.getActualTypeArguments();

              if (castMap.containsKey(fieldType.getName())) {
                typeBuffer.append(castMap.get(fieldType.getName()));
              } else if (Map.class.isAssignableFrom(fieldType)) {
                typeBuffer.append(readAsMapGeneric(parameterTypes, castMap));
              } else if (Iterable.class.isAssignableFrom(fieldType)) {
                isIterable = true;
                for (Type parameterType : parameterTypes) {
                  convertIterableGenerics(parameterType, typeBuffer, castMap);
                }
              } else {
                typeBuffer.append(getTSType(fieldType));
                typeBuffer.append("<");

                for (int j = 0; j < parameterTypes.length; j++) {
                  if (j > 0) typeBuffer.append(",");
                  typeBuffer.append(ALPHABET[j % ALPHABET.length]);
                }
                typeBuffer.append(">");
              }
            } else {
              typeBuffer.append(castMap.getOrDefault(
                fieldType.getName(),
                getTSType(fieldType)));
              if (fieldType.isArray() && !typeBuffer.toString().endsWith("[]"))
                typeBuffer.append("[]");
            }
            output.append(fieldBuffer);
            output.append(typeBuffer);
            if (isIterable) output.append("[]");
            output.append(";");
          } catch (Exception e) {
            output.append(fieldBuffer);
            output.append("any");
            if (isIterable) output.append("[]");
            output.append(";");
            logger.log(SEVERE, "Failed to convert field type for `" + field.getName() + "` in `" + clazz + "`, defaulting to `any`", e);
          }

        }
      } catch (Exception e) {
        logger.log(SEVERE, "Failed to convert " + clazz, e);
      }

      output.append("}");
      return output.toString();
    }

    return "";
  }

  private void convertIterableGenerics(Type type, StringBuilder typeBuffer, Map<String, String> castMap) throws ClassCastException {
    if (type instanceof WildcardType) {
      WildcardType wildcardType = (WildcardType) type;
      Type[] bounds = wildcardType.getLowerBounds();

      if (bounds.length == 0) {
        bounds = wildcardType.getUpperBounds();
      }
      if (bounds[0] instanceof ParameterizedType) {
        convertIterableGenerics(bounds[0], typeBuffer, castMap);
      } else {
        Class<?> target = (Class<?>) bounds[0];
        typeBuffer.append(castMap.getOrDefault(target.getName(), getTSType(target)));
      }
    } else if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      Class<?> target = (Class<?>) parameterizedType.getRawType();
      typeBuffer.append(castMap.getOrDefault(target.getName(), getTSType(target)));
    } else {
      Class<?> target = (Class<?>) type;
      typeBuffer.append(castMap.getOrDefault(target.getName(), getTSType(target)));
    }
  }

  private String readAsMapGeneric(Type[] parameterTypes, Map<String, String> castMap) {
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
