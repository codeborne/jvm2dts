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
      "roles: Role[]; " +
      "dates: string[]; " +
      "ids: string[]; " +
      "map: {[key: string]: number}; " +
      "mapInMap: {[key: string]: {[key: string]: number}}; " +
      "extendedGeneric: SingleGeneric<T>; " +
      "superGeneric: SingleGeneric<T>; " +
      "generic: MultiGeneric<T,U,V>; " +
      "superGenericList: Role[]; " +
      "genericRecursiveList: SingleGeneric[]; " +
      "rawType: ArrayList;" +
      "}");
  }
}

enum Role {
  ADMIN, USER
}

interface Collections {
  Role[] getRoles();
  Date[] getDates();
  List<UUID> getIds();
  Map<String, Integer> getMap();
  Map<String, Map<String, Integer>> getMapInMap();
  SingleGeneric<? extends Role> getExtendedGeneric();
  SingleGeneric<? super Role> getSuperGeneric();
  MultiGeneric<?, ?, ?> getGeneric();
  List<? super Role> getSuperGenericList();
  List<SingleGeneric<?>> getGenericRecursiveList();
  @SuppressWarnings("rawtypes") ArrayList getRawType();

  static boolean getDoNotGenerateStatics() { return false; }
}

class MultiGeneric<T, U, V> {}
class SingleGeneric<T> {}
