package jvm2dts.types;

import jdk.internal.org.objectweb.asm.*;
import jvm2dts.ToTypeScriptConverter;
import jvm2dts.TypeMapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.reflect.Modifier.isStatic;
import static java.util.logging.Level.SEVERE;
import static jvm2dts.NameConverter.convertName;
import static jvm2dts.types.ClassConverter.ASM_VERSION;

// TODO: seems like having a builder will be more beneficial - allowing more args
public class ClassConverter implements ToTypeScriptConverter {
  static final int ASM_VERSION = detectAsmVersion();
  static final char[] ALPHABET = "TUVWYXYZABCDEFGHIJKLMNOPQRS".toCharArray();
  TypeMapper typeMapper;

  static int detectAsmVersion() {
    try {
      return (int) Opcodes.class.getField("ASM7").get(null); // Java 16
    } catch (Exception e) {
      return Opcodes.ASM6; // Java 11
    }
  }

  public ClassConverter(TypeMapper typeMapper) {
    this.typeMapper = typeMapper;
  }

  @Override public String convert(Class<?> clazz) {
    var output = new StringBuilder("interface ").append(clazz.getSimpleName()).append(" {");
    var activeAnnotations = new HashMap<String, List<String>>();

    var fields = clazz.getDeclaredFields();
    if (fields.length > 0) {
      try {
        var in = clazz.getClassLoader().getResourceAsStream(clazz.getName().replace(".", "/") + ".class");
        new ClassAnnotationReader(in).accept(new ClassAdapter(activeAnnotations), ClassReader.SKIP_CODE);
        for (Field field : fields) {
          if (isStatic(field.getModifiers())) continue;
          processField(field, output, activeAnnotations);
        }
      } catch (Exception e) {
        logger.log(SEVERE, "Failed to convert " + clazz, e);
      }

      if (output.charAt(output.length() - 1) == ' ') output.setLength(output.length() - 1);
      output.append("}");
      return output.toString();
    }

    return "";
  }

  private void processField(Field field, StringBuilder out, Map<String, List<String>> activeAnnotations) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    var fieldBuffer = new StringBuilder();

    var expectedFieldName = field.getName();
    for (Annotation annotation : field.getDeclaredAnnotations()) {
      String annotationName = annotation.annotationType().getSimpleName();
      if (annotationName.equals("JsonIgnore"))
        return;
      else if (annotationName.equals("JsonProperty"))
        expectedFieldName = (String) annotation.getClass().getMethod("value").invoke(annotation);
    }

    fieldBuffer.append(expectedFieldName);

    if (!activeAnnotations.isEmpty())
      for (String annotation : activeAnnotations.getOrDefault(field.getName(), new ArrayList<>()))
        if (annotation.matches(".*Nullable;.*"))
          fieldBuffer.append("?");

    fieldBuffer.append(": ");

    boolean isIterable = false;
    var typeBuffer = new StringBuilder();
    try {
      var fieldType = field.getType();

      if (field.getGenericType() instanceof ParameterizedType) {
        var genericType = (ParameterizedType) field.getGenericType();
        var parameterTypes = genericType.getActualTypeArguments();
        isIterable = processGenericField(typeBuffer, fieldType, parameterTypes);
      } else {
        isIterable = fieldType.isArray();
        typeBuffer.append(typeMapper.getTSType(isIterable ? fieldType.getComponentType() : fieldType));
      }
    } catch (Exception e) {
      logger.log(SEVERE, "Failed to convert field type for `" + field.getName() + "` in `" + field.getDeclaringClass() + "`, defaulting to `any`", e);
      typeBuffer = new StringBuilder("any");
    }
    out.append(fieldBuffer);
    out.append(typeBuffer);
    if (isIterable) out.append("[]");
    out.append("; ");
  }

  private boolean processGenericField(StringBuilder typeBuffer, Class<?> fieldType, Type[] parameterTypes) {
    var isIterable = false;
    if (Map.class.isAssignableFrom(fieldType)) {
      typeBuffer.append(readAsMapGeneric(parameterTypes));
    } else if (Iterable.class.isAssignableFrom(fieldType)) {
      isIterable = true;
      for (Type parameterType : parameterTypes) {
        convertIterableGenerics(parameterType, typeBuffer);
      }
    } else {
      String type = typeMapper.getSimpleTSType(fieldType);
      if (type != null) typeBuffer.append(type);
      else {
        typeBuffer.append(convertName(fieldType));
        typeBuffer.append("<");
        for (int j = 0; j < parameterTypes.length; j++) {
          if (j > 0) typeBuffer.append(",");
          typeBuffer.append(ALPHABET[j % ALPHABET.length]);
        }
        typeBuffer.append(">");
      }
    }
    return isIterable;
  }

  private void convertIterableGenerics(Type type, StringBuilder typeBuffer) throws ClassCastException {
    if (type instanceof WildcardType) {
      var wildcardType = (WildcardType) type;
      var bounds = wildcardType.getLowerBounds();
      if (bounds.length == 0) bounds = wildcardType.getUpperBounds();
      if (bounds[0] instanceof ParameterizedType) convertIterableGenerics(bounds[0], typeBuffer);
      else typeBuffer.append(typeMapper.getTSType((Class<?>) bounds[0]));
    } else if (type instanceof ParameterizedType) {
      var parameterizedType = (ParameterizedType) type;
      var elementType = (Class<?>) parameterizedType.getRawType();
      if (Iterable.class.isAssignableFrom(elementType))
        typeBuffer.append(typeMapper.getTSType((Class<?>) parameterizedType.getActualTypeArguments()[0])).append("[]");
      else
        typeBuffer.append(typeMapper.getTSType(elementType));
    } else {
      typeBuffer.append(typeMapper.getTSType((Class<?>) type));
    }
  }

  private String readAsMapGeneric(Type[] parameterTypes) {
    var output = new StringBuilder();

    output.append("{");
    for (int j = 0; j < parameterTypes.length; j = +2) {
      var value = parameterTypes[j + 1];
      output.append("[key: string]: ");
      if (value instanceof ParameterizedType)
        output.append(readAsMapGeneric(((ParameterizedType) value).getActualTypeArguments()));
      else
        output.append(typeMapper.getTSType((Class<?>) value));
    }
    output.append("}");

    return output.toString();
  }
}

class ClassAdapter extends ClassVisitor {
  Map<String, List<String>> activeAnnotations;

  public ClassAdapter(Map<String, List<String>> activeAnnotations) {
    super(ASM_VERSION);
    this.activeAnnotations = activeAnnotations;
  }

  @Override public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
    return new FieldAnnotationAdapter(activeAnnotations.computeIfAbsent(name, k -> new ArrayList<>()));
  }
}

class FieldAnnotationAdapter extends FieldVisitor {
  List<String> activeAnnotations;

  public FieldAnnotationAdapter(List<String> activeAnnotations) {
    super(ASM_VERSION);
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
