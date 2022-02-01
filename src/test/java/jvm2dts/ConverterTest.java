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
      "rawType: ArrayList; " +
      "map: {[key: string]: number}; " +
      "roles: ConverterTestRole[]; " +
      "dates: Date|string[]; " +
      "ids: string[]; " +
      "mapInMap: {[key: string]: {[key: string]: number}}; " +
      "extendedGeneric: SingleGeneric<T>; " +
      "superGeneric: SingleGeneric<T>; " +
      "generic: MultiGeneric<T,U,V>; " +
      "superGenericList: ConverterTestRole[]; " +
      "genericRecursiveList: SingleGeneric[];" +
      "}");
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

  static class MultiGeneric<T, U, V> {}
  static class SingleGeneric<T> {}
}
