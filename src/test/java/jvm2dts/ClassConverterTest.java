package jvm2dts;

import jvm2dts.types.ClassConverter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassConverterTest {
  private final ClassConverter converter = new ClassConverter();

  @Test
  void modelClass() {
    assertThat(converter.convert(Model.class)).isEqualTo("interface Model {name: string; age: number; role: Role;}");
  }

  @Test
  void primitiveNumbers() {
    assertThat(converter.convert(NumbersPrimitive.class)).isEqualTo("interface NumbersPrimitive {" +
      "aByte: number; " +
      "aShort: number; " +
      "anInt: number; " +
      "aLong: number; " +
      "aFloat: number; " +
      "aDouble: number;}");
  }

  @Test
  void objectNumbers() {
    assertThat(converter.convert(NumbersObjects.class)).isEqualTo("interface NumbersObjects {" +
      "aByte: number; " +
      "aShort: number; " +
      "anInteger: number; " +
      "aLong: number; " +
      "aFloat: number; " +
      "aDouble: number;}");
  }

  enum Role {
    ADMIN, USER
  }

  static class Model {
    String name;
    int age;
    Role role;
  }

  static class NumbersPrimitive {
    byte aByte;
    short aShort;
    int anInt;
    long aLong;
    float aFloat;
    double aDouble;
  }

  static class NumbersObjects {
    Byte aByte;
    Short aShort;
    Integer anInteger;
    Long aLong;
    Float aFloat;
    Double aDouble;
  }
}