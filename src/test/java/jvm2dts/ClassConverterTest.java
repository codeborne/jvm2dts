package jvm2dts;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;
import jvm2dts.types.ClassConverter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassConverterTest {
  private final ClassConverter converter = new ClassConverter();

  @Test
  void modelClass() {
    assertThat(converter.convert(Model.class)).isEqualTo("interface Model {name: string; age: number; role: ModelRole;}");
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
      "aFloat?: number; " +
      "aDouble?: number;}");
  }

  @Test
  void jsonProperty() {
    assertThat(converter.convert(JsonPropertyObject.class)).isEqualTo("interface JsonPropertyObject {" +
      "namedProperty: boolean; " +
      "literalObject: any;}");
  }

  @SuppressWarnings("unused")
  static class NumbersPrimitive {
    byte aByte;
    short aShort;
    int anInt;
    long aLong;
    float aFloat;
    double aDouble;
  }

  @SuppressWarnings("unused")
  static class NumbersObjects {
    Byte aByte;
    Short aShort;
    Integer anInteger;
    Long aLong;
    @Nullable Float aFloat;
    @Nullable Double aDouble;
  }

  @SuppressWarnings("unused")
  static class JsonPropertyObject {
    @JsonProperty("namedProperty") Boolean notWhatIWant;
    Object literalObject = new Object();
  }
}

@SuppressWarnings("unused")
class Model {
  String name;
  int age;
  Role role;

  enum Role {
    HELLO, WORLD
  }
}
