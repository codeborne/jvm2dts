package jvm2dts.types;

import jdk.internal.org.objectweb.asm.*;
import jvm2dts.ToTypeScriptConverter;
import jvm2dts.TypeMapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Comparator.naturalOrder;
import static java.util.logging.Level.SEVERE;
import static java.util.stream.Collectors.toMap;
import static jvm2dts.NameConverter.convertName;
import static jvm2dts.types.ClassConverter.ASM_VERSION;
import static jvm2dts.types.ClassConverter.isLikeGetter;

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
    var output = new StringBuilder("interface ").append(convertName(clazz)).append(" {");
    var methodAnnotations = new LinkedHashMap<String, List<String>>();

    try {
      var in = clazz.getClassLoader().getResourceAsStream(clazz.getName().replace(".", "/") + ".class");
      new ClassReader(in).accept(new ClassAnnotationExtractor(methodAnnotations), ClassReader.SKIP_CODE);

      var getters = stream(clazz.getMethods())
        .filter(m -> !isStatic(m.getModifiers()) && m.getParameterCount() == 0 && isLikeGetter(m.getName()))
        .collect(toMap(Method::getName, m -> m, (m1, m2) -> m1.getReturnType().isAssignableFrom(m2.getReturnType()) ? m2 : m1));

      var methodNamesInOrder = new ArrayList<>(methodAnnotations.keySet());
      methodNamesInOrder.retainAll(getters.keySet());

      var superClassGetters = new ArrayList<>(getters.keySet());
      superClassGetters.removeAll(methodNamesInOrder);
      superClassGetters.sort(naturalOrder());
      methodNamesInOrder.addAll(superClassGetters);

      for (String name : methodNamesInOrder) {
        processProperty(getters.get(name), output, methodAnnotations);
      }
    } catch (Exception e) {
      logger.log(SEVERE, "Failed to convert " + clazz, e);
    }

    if (output.charAt(output.length() - 1) == ' ') output.setLength(output.length() - 1);
    output.append("}");
    var result = output.toString();
    return result.endsWith("{}") ? null : result;
  }

  static boolean isLikeGetter(String methodName) {
    return (methodName.startsWith("get") || methodName.startsWith("is")) && !methodName.equals("getClass");
  }

  private void processProperty(Method method, StringBuilder out, Map<String, List<String>> classAnnotations) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    var fieldBuffer = new StringBuilder();

    var name = method.getName();
    var propertyName = name.startsWith("get") ? name.substring(3, 4).toLowerCase() + name.substring(4) :
                              name.startsWith("is") ? name.substring(2, 3).toLowerCase() + name.substring(3) : null;

    if (propertyName == null) return;
    var dashPos = propertyName.indexOf('-');
    if (dashPos > 0) propertyName = propertyName.substring(0, dashPos);

    var annotations = method.getAnnotations();
    try {
      if (annotations.length == 0)
        annotations = method.getDeclaringClass().getMethod(method.getName() + "$annotations").getAnnotations();
    } catch (NoSuchMethodException ignore) {}

    for (Annotation annotation : annotations) {
      String annotationName = annotation.annotationType().getSimpleName();
      if (annotationName.equals("JsonIgnore")) return;
      else if (annotationName.equals("JsonProperty"))
        propertyName = (String) annotation.getClass().getMethod("value").invoke(annotation);
    }

    fieldBuffer.append(propertyName);

    if (!classAnnotations.isEmpty())
      for (String annotation : classAnnotations.getOrDefault(name, emptyList()))
        if (annotation.contains("Nullable;"))
          fieldBuffer.append("?");

    fieldBuffer.append(": ");

    boolean isIterable = false;
    var typeBuffer = new StringBuilder();
    try {
      var type = method.getReturnType();

      if (method.getGenericReturnType() instanceof ParameterizedType) {
        var parameterTypes = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments();
        isIterable = processGenericField(typeBuffer, type, parameterTypes);
      } else {
        isIterable = type.isArray();
        typeBuffer.append(typeMapper.getTSType(isIterable ? type.getComponentType() : type));
      }
    } catch (Exception e) {
      logger.log(SEVERE, "Failed to convert property type for `" + name + "` in `" + method.getDeclaringClass() + "`, defaulting to `any`", e);
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
      var type = typeMapper.getSimpleTSType(fieldType);
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
    for (int j = 0; j < parameterTypes.length; j = 2) {
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

class ClassAnnotationExtractor extends ClassVisitor {
  Map<String, List<String>> annotations;

  public ClassAnnotationExtractor(Map<String, List<String>> annotations) {
    super(ASM_VERSION);
    this.annotations = annotations;
  }

  @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    if (!isPublic(access) || !isLikeGetter(name)) return null;
    return new MethodAnnotationExtractor(annotations.computeIfAbsent(name, k -> new ArrayList<>()));
  }
}

class MethodAnnotationExtractor extends MethodVisitor {
  List<String> annotations;

  public MethodAnnotationExtractor(List<String> annotations) {
    super(ASM_VERSION);
    this.annotations = annotations;
  }

  @Override public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    annotations.add(descriptor);
    return null;
  }
}
