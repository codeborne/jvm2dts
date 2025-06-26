package jvm2dts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

class ClassConverterTest {
  private final Converter converter = new Converter(new TypeMapper(emptyMap()));

  @Test
  void modelClass() {
    assertThat(converter.convert(Model.class)).isEqualTo("interface Model {" +
      "name: string; " +
      "age: number; " +
      "role: ModelRole; " +
      "listOfLong: number[]; " +
      "listOfList: string[][]; " +
      "details: ModelDetails; " +
      "id: string;}");

    assertThat(converter.convert(Model.Details.class)).isEqualTo("interface ModelDetails {" +
      "stuff: string;}");
  }

  @Test
  void primitiveTypes() {
    assertThat(converter.convert(Primitives.class)).isEqualTo("interface Primitives {" +
      "aByte: number; " +
      "aShort: number; " +
      "int: number; " +
      "long: number; " +
      "float: number; " +
      "double: number; " +
      "boolean: boolean;}");
  }

  @Test
  void wrapperTypes() {
    assertThat(converter.convert(WrapperTypes.class)).isEqualTo("interface WrapperTypes {" +
      "aByte: number; " +
      "aShort: number; " +
      "integer: number; " +
      "long: number; " +
      "boolean: boolean; " +
      "float?: number; " +
      "double?: number;}");
  }

  @Test
  void jsonProperty() {
    assertThat(converter.convert(JsonPropertyObject.class)).isEqualTo("interface JsonPropertyObject {" +
      "namedProperty: boolean; " +
      "literalObject: any;}");
  }

  @Test
  void realClass() {
    assertThat(converter.convert(RealClass.class)).isEqualTo("interface RealClass {" +
      "hello: string; " +
      "id: string;}");
  }

  @Test
  void record() {
    assertThat(converter.convert(Record.class)).isEqualTo("interface Record {" +
      "hello: string; " +
      "world: string;}");
  }

  @Test
  void emptyTypes() {
    assertThat(converter.convert(Empty.class)).isNull();
    assertThat(converter.convert(OnlyPrivate.class)).isNull();
  }
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
  String getKotlinPropertyIgnore();
  @JsonIgnore String getKotlinPropertyIgnore$annotations();
}

class RealClass implements Base {
  public String getHello() { return ""; }
  void method() {}
  @Override public UUID getId() { return null; }
  private int getPrivate() { return 0; }
}

interface Empty {}
class OnlyPrivate {
  private String getHello() { return ""; }
}

@SuppressWarnings("unused")
interface Model extends Base {
  String getName();
  int getAge();
  Role getRole();
  List<Long> getListOfLong();
  List<List<String>> getListOfList();
  Details getDetails();

  enum Role {
    HELLO, WORLD
  }

  interface Details {
    String getStuff();
  }
}

interface Base extends AnyId {
  UUID getId();
}

interface AnyId {
  Object getId();
}

record Record(String hello, String world) {}
