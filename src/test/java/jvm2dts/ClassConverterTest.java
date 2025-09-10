package jvm2dts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

class ClassConverterTest {
  private final Converter converter = new Converter(new TypeMapper(emptyMap()));

  @Test
  void modelClass() {
    assertThat(converter.convert(Model.class)).isEqualTo("interface Model {" +
      "age: number; " +
      "details: ModelDetails; " +
      "id: string; " +
      "listOfList: string[][]; " +
      "listOfLong: number[]; " +
      "name: string; " +
      "role: ModelRole;" +
      "}");

    assertThat(converter.convert(Model.Details.class)).isEqualTo("interface ModelDetails {" +
      "stuff: string;}");
  }

  @Test
  void primitiveTypes() {
    assertThat(converter.convert(Primitives.class)).isEqualTo("interface Primitives {" +
      "aByte: number; " +
      "aShort: number; " +
      "boolean: boolean; " +
      "double: number; " +
      "float: number; " +
      "int: number; " +
      "long: number;}");
  }

  @Test
  void wrapperTypes() {
    assertThat(converter.convert(WrapperTypes.class)).isEqualTo("interface WrapperTypes {" +
      "aByte: number; " +
      "aShort: number; " +
      "boolean: boolean; " +
      "double?: number; " +
      "float?: number; " +
      "integer: number; " +
      "long: number; " +
      "optional?: string;" +
      "}");
  }

  @Test
  void jsonProperty() {
    assertThat(converter.convert(JsonPropertyObject.class)).isEqualTo("interface JsonPropertyObject {" +
      "literalObject: any; " +
      "namedProperty: boolean;" +
      "}");
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
      "notIncluded: boolean; " +
      "\"@key\": string; " +
      "hello: string; " +
      "world?: string; " +
      "isCool: boolean;" +
      "}");
  }

  @Test
  void emptyTypes() {
    assertThat(converter.convert(Empty.class)).isNull();
    assertThat(converter.convert(OnlyPrivate.class)).isNull();
  }

  @Retention(RUNTIME)
  @interface Nullable {}
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
  @ClassConverterTest.Nullable Double getDouble();
  Optional<String> getOptional();
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

record Record(String hello, @ClassConverterTest.Nullable String world, boolean isCool) {
  @JsonProperty public boolean notIncluded() { return false; }
  @JsonProperty("@key") public String key() { return ""; }
}
