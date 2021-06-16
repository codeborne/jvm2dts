package jvm2dts.types;

import jvm2dts.ToTypeScriptConverter;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.reflect.Modifier.isStatic;
import static java.util.logging.Level.SEVERE;
import static jvm2dts.NameConverter.getName;
import static jvm2dts.TypeNameToTSMap.getTSType;
import static org.objectweb.asm.Opcodes.ASM9;

// TODO: seems like having a builder will be more beneficial - allowing more args
public class ClassConverter implements ToTypeScriptConverter {
  static final char[] ALPHABET = "TUVWYXYZABCDEFGHIJKLMNOPQRS".toCharArray();

  public String convert(Class<?> clazz) {
    return convert(clazz, new HashMap<>());
  }

  public String convert(Class<?> clazz, Map<String, String> castMap) {
    var output = new StringBuilder("interface ").append(clazz.getSimpleName()).append(" {");
    var activeAnnotations = new HashMap<String, List<String>>();

    var fields = clazz.getDeclaredFields();
    if (fields.length > 0) {
      try {
        var in = clazz.getClassLoader().getResourceAsStream(clazz.getName().replace(".", "/") + ".class");
        new ClassAnnotationReader(in).accept(new ClassAdapter(activeAnnotations), 0);

        for (int i = 0; i < fields.length; i++) {
          var field = fields[i];
          if (isStatic(field.getModifiers())) continue;
          if (i > 0) output.append(" ");
          processField(field, castMap, output, activeAnnotations);
        }
      } catch (Exception e) {
        logger.log(SEVERE, "Failed to convert " + clazz, e);
      }

      output.append("}");
      return output.toString();
    }

    return "";
  }

  private void processField(Field field, Map<String, String> castMap, StringBuilder output, HashMap<String, List<String>> activeAnnotations) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    var fieldBuffer = new StringBuilder();

    var expectedFieldName = field.getName();
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
      var fieldType = field.getType();
      var typeBuffer = new StringBuilder();

      if (field.getGenericType() instanceof ParameterizedType) {
        var genericType = (ParameterizedType) field.getGenericType();
        var parameterTypes = genericType.getActualTypeArguments();

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
      logger.log(SEVERE, "Failed to convert field type for `" + field.getName() + "` in `" + field.getDeclaringClass() + "`, defaulting to `any`", e);
    }
  }

  private void convertIterableGenerics(Type type, StringBuilder typeBuffer, Map<String, String> castMap) throws ClassCastException {
    if (type instanceof WildcardType) {
      var wildcardType = (WildcardType) type;
      var bounds = wildcardType.getLowerBounds();

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
      var parameterizedType = (ParameterizedType) type;
      var target = (Class<?>) parameterizedType.getRawType();
      typeBuffer.append(castMap.getOrDefault(target.getName(), getTSType(target)));
    } else {
      var target = (Class<?>) type;
      typeBuffer.append(castMap.getOrDefault(target.getName(), getTSType(target)));
    }
  }

  private String readAsMapGeneric(Type[] parameterTypes, Map<String, String> castMap) {
    var output = new StringBuilder();

    output.append("{");
    for (int j = 0; j < parameterTypes.length; j = +2) {
      var value = parameterTypes[j + 1];
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

class ClassAdapter extends ClassVisitor {
  Map<String, List<String>> activeAnnotations;

  public ClassAdapter(Map<String, List<String>> activeAnnotations) {
    super(ASM9);
    this.activeAnnotations = activeAnnotations;
  }

  @Override public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
    return new FieldAnnotationAdapter(activeAnnotations.computeIfAbsent(name, k -> new ArrayList<>()));
  }
}

class FieldAnnotationAdapter extends FieldVisitor {
  List<String> activeAnnotations;

  public FieldAnnotationAdapter(List<String> activeAnnotations) {
    super(ASM9);
    this.activeAnnotations = activeAnnotations;
  }

  @Override public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    activeAnnotations.add(descriptor);
    return super.visitAnnotation(descriptor, visible);
  }
}

class ClassAnnotationReader extends ClassReader {
  public ClassAnnotationReader(InputStream in) throws IOException {
    super(in);
  }
}
