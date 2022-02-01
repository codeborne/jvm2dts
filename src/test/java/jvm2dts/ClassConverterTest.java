package jvm2dts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jvm2dts.types.ClassConverter;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

class ClassConverterTest {
  private final ClassConverter converter = new ClassConverter(new TypeMapper(emptyMap()));

  @Test
  void modelClass() {
    assertThat(converter.convert(Model.class)).isEqualTo("interface Model {" +
      "name: string; " +
      "age: number; " +
      "role: ModelRole; " +
      "listOfLong: number[]; " +
      "listOfList: string[][]; " +
      "id: string;}");
  }

  @Test
  void primitiveTypes() {
    assertThat(converter.convert(Primitives.class)).isEqualTo("interface Primitives {" +
      "int: number; " +
      "long: number; " +
      "float: number; " +
      "double: number; " +
      "aByte: number; " +
      "aShort: number; " +
      "boolean: boolean;}");
  }

  @Test
  void wrapperTypes() {
    assertThat(converter.convert(WrapperTypes.class)).isEqualTo("interface WrapperTypes {" +
      "long: number; " +
      "float?: number; " +
      "double?: number; " +
      "integer: number; " +
      "aByte: number; " +
      "aShort: number; " +
      "boolean: boolean;}");
  }

  @Test
  void jsonProperty() {
    assertThat(converter.convert(JsonPropertyObject.class)).isEqualTo("interface JsonPropertyObject {" +
      "namedProperty: boolean; " +
      "literalObject: any;}");
  }

  @SuppressWarnings("unused")
  interface Primitives {
    byte getAByte();
    short getAShort();
    int getInt();
    long getLong();
    float getFloat();
    double getDouble();
    boolean isBoolean();
  }

  @SuppressWarnings("unused")
  interface WrapperTypes {
    Byte getAByte();
    Short getAShort();
    Integer getInteger();
    Long getLong();
    Boolean isBoolean();
    @Nullable Float getFloat();
    @Nullable Double getDouble();
  }

  @SuppressWarnings("unused")
  interface JsonPropertyObject {
    @JsonProperty("namedProperty") Boolean getNotWhatIWant();
    Object getLiteralObject();
    @JsonIgnore String getIgnore();
  }
}

@SuppressWarnings("unused")
interface Model extends Base {
  String getName();
  int getAge();
  Role getRole();
  List<Long> getListOfLong();
  List<List<String>> getListOfList();

  enum Role {
    HELLO, WORLD
  }
}

interface Base {
  UUID getId();
}
