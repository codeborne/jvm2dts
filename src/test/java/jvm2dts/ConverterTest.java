package jvm2dts;

import org.junit.jupiter.api.Test;

import java.util.*;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

public class ConverterTest {
  private final Converter converter = new Converter(new TypeMapper(emptyMap()));

  @Test
  void collections() {
    assertThat(converter.convert(Collections.class)).isEqualTo("interface Collections {" +
      "roles: ConverterTestRole[]; " +
      "dates: Date|string[]; " +
      "ids: string[]; " +
      "map: {[key: string]: number}; " +
      "mapInMap: {[key: string]: {[key: string]: number}}; " +
      "extendedGeneric: SingleGeneric<T>; " +
      "superGeneric: SingleGeneric<T>; " +
      "generic: MultiGeneric<T,U,V>; " +
      "superGenericList: ConverterTestRole[]; " +
      "genericRecursiveList: SingleGeneric[]; " +
      "rawType: ArrayList;" +
      "}");
  }

  enum Role {
    ADMIN, USER
  }

  static class Collections {
    Role[] roles;
    Date[] dates;
    List<UUID> ids;
    Map<String, Integer> map;
    Map<String, Map<String, Integer>> mapInMap;
    SingleGeneric<? extends Role> extendedGeneric;
    SingleGeneric<? super Role> superGeneric;
    MultiGeneric<?, ?, ?> generic;
    List<? super Role> superGenericList;
    List<SingleGeneric<?>> genericRecursiveList;
    @SuppressWarnings("rawtypes") ArrayList rawType;

    static boolean doNotGenerateStatics = true;
    static Date unwantedDate = new Date();
  }

  static class MultiGeneric<T, U, V> {}
  static class SingleGeneric<T> {}
}