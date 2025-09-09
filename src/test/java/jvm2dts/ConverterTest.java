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
      "dates: string[]; " +
      "extendedGeneric: SingleGeneric<T>; " +
      "generic: MultiGeneric<T,U,V>; " +
      "genericRecursiveList: SingleGeneric[]; " +
      "ids: string[]; " +
      "map: {[key: string]: number}; " +
      "mapInMap: {[key: string]: {[key: string]: number}}; " +
      "rawType: ArrayList; " +
      "roles: Role[]; " +
      "superGeneric: SingleGeneric<T>; " +
      "superGenericList: Role[];" +
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
