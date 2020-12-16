package jvm2dts;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ConverterTest {
  private final Converter converter = new Converter();

  @Test
  void collections() {
    assertThat(converter.convert(Collections.class)).isEqualTo("interface Collections {" +
      "roles: Role[]; " +
      "dates: Date[]; " +
      "ids: string[]; " +
      "map: {[key: string]: number};}");
  }

  enum Role {
    ADMIN, USER
  }

  static class Collections {
    Role[] roles;
    Date[] dates;
    List<UUID> ids;
    Map<String, Integer> map;
  }

}