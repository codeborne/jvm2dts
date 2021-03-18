package jvm2dts;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class ConverterTest {
  private final Converter converter = new Converter();

  @Test
  void collections() {
    assertThat(converter.convert(Collections.class)).isEqualTo("interface Collections {" +
      "roles: Role[]; " +
      "dates: Date[]; " +
      "ids: string[]; " +
      "map: {[key: string]: number}; " +
      "mapInMap: {[key: string]: {[key: string]: number}}; " +
      "generic: Generic<T,U,V>; " +
      "extendedGeneric: ExtendedGeneric<T>; " +
      "rawType: ArrayList;}");
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
    Generic<?, ?, ?> generic;
    ExtendedGeneric<? extends Role> extendedGeneric;
    ArrayList rawType;

    static boolean doNotGenerate = true;
    static Date unwantedDate = new Date();
  }

  static class Generic<T, U, V> {}
  static class ExtendedGeneric<T> {}
}