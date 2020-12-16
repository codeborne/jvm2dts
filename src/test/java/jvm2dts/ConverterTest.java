package jvm2dts;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConverterTest {
  private final Converter converter = new Converter();

  @Test
  void convertEnum() {
    assertThat(converter.convert(Role.class)).isEqualTo("enum Role {ADMIN = 'ADMIN', USER = 'USER'}");
  }

  @Test
  void convertValueEnum() {
    assertThat(converter.convert(RoleWithValues.class)).isEqualTo("enum RoleWithValues {ADMIN = 'admin', USER = 'user'}");
  }
}

enum Role {
  ADMIN, USER
}

enum RoleWithValues {
  ADMIN("admin"), USER("user");

  private String s;

  RoleWithValues(String s) {
    this.s = s;
  }

  @Override
  public String toString() {
    return s;
  }
}
