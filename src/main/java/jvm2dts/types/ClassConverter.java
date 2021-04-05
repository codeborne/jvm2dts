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

          try {
            StringBuilder typeBuffer = new StringBuilder();
            if (field.getGenericType() instanceof ParameterizedType) {
              ParameterizedType genericType = (ParameterizedType) field.getGenericType();
              Type[] parameterTypes = genericType.getActualTypeArguments();
              if (castMap.containsKey(field.getType().getName())) {
                typeBuffer.append(castMap.get(field.getType().getName()));
              } else if (Map.class.isAssignableFrom(field.getType())) {
                typeBuffer.append(readAsMapGeneric(parameterTypes, castMap));
              } else if (Iterable.class.isAssignableFrom(field.getType())) {
                // TODO: will not work with iterables that have more than one generic
                // obscure piece "retrofitted" from https://issues.apache.org/jira/browse/CXF-3470
                if (parameterTypes[0] instanceof WildcardType) {
                  WildcardType wildcardType = (WildcardType) parameterTypes[0];
                  Type[] bounds = wildcardType.getLowerBounds();
                  if (bounds.length == 0) {
                    bounds = wildcardType.getUpperBounds();
                  }

                  // TODO: get the actual type rather than defaulting to any
                  //  there are weird anomalies, where test results here
                  //  give jvm2dts.ConverterTest$Role while an external
                  //  project returns package.Role<?> (external project uses Kotlin)
                  //  getTypeName only returns a String, are there any other options?

                  // typeBuffer.append(bounds[0].getTypeName()).append("[]");
                  typeBuffer.append("any");
                } else
                  typeBuffer.append(getTSType((Class<?>) parameterTypes[0])).append("[]");
              } else {
                typeBuffer.append(getTSType(field.getType()));
                typeBuffer.append("<");
                for (int j = 0; j < parameterTypes.length; j++) {
                  if (j > 0) typeBuffer.append(",");
                  typeBuffer.append(ALPHABET[j % ALPHABET.length]);
                }
                typeBuffer.append(">");
              }
            } else {
              typeBuffer.append(castMap.getOrDefault(
                field.getType().getName(),
                getTSType(field.getType())));
            }

            output.append(fieldBuffer);
            output.append(typeBuffer);
            output.append(";");
          } catch (Exception e) {
            output.append(fieldBuffer);
            output.append("any");
            output.append(";");
            logger.log(Level.SEVERE, "Failed to convert field type for `" + field.getName() + "` in `" + clazz + "`, defaulting to `any`", e);
          }
        }
      } catch (
        Exception e) {
        logger.log(Level.SEVERE, "Failed to convert " + clazz, e);
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
