# jvm2dts

Generates TypeScript definitions (d.ts) for Java/Kotlin model classes using reflection/asm 
that can be used for client-side type checking of API calls and responses.

All public getters are read by default, in a similar way to Jackson.
Works with Java/Lombok/Kotlin data classes. Java records are not yet supported.

## Usage

Using `jvm2dts` requires that you include class paths of both the target project and `jvm2dts` itself. 
Java version must be **at least 11**

```
Example: java -classpath path/to/package jvm2dts.Main -exclude MyRegExp -cast MyClass=number,AnotherClass=string package1 package2 package3
Usage: <main class> [options]
  Options:
    -c, -cast
      Comma-separated key=value map to make classnames matching the key into 
      specified value
    -classesDir
      Recursively look for classes from a location
    -d, -data-only
      Find only Kotlin data classes
      Default: false
    -e, -exclude
      Excludes classes in the generation matching a Java RegExp pattern
    -excludeDir
      Comma-separated list to filter out package names when using classesDir
```

[Read more about setting class paths in Java](https://docs.oracle.com/javase/11/docs/technotes/tools/windows/classpath.html)

Converted **TypeScript interfaces output from stdout** and all **errors are through stderr**

### Using in Gradle (Kotlin DSL)

Note: unreleased versions can also be obtained from https://jitpack.io/#codeborne/jvm2dts

```kotlin
val jvm2dts by configurations.creating

repositories {
  mavenCentral()
  maven { url = uri("https://jitpack.io") }
}

dependencies {
  jvm2dts("com.github.codeborne:jvm2dts:1.6.0")
}

tasks.register("types.ts") { 
  dependsOn("classes")
  doLast {
      val mainSource = sourceSets.main.get()
      project.file("api/types.ts").writeText(ByteArrayOutputStream().use { out ->
        project.javaexec {
          standardOutput = out
          mainClass.set("jvm2dts.Main")
          jvmArgs = listOf("--add-exports=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED") // Java 16+ needs this
          classpath = jvm2dts + sourceSets.main.get().runtimeClasspath
          args("-exclude", ".*SuffixOfClassNameIDontWant|PrefixOfClassNameIDontWant.*", 
               "-cast", "MyNumericClass=number",
               "-classesDir", "${project.buildDir}/classes/kotlin/main")
        }
      out.toString()
    })
  }
}

tasks.withType<KotlinCompile> { // or JavaCompile
  finalizedBy("types.ts")
}
```

### Recursive directory class loading

While `jvm2dts` can **recursively walk through directories** with `-classesDir` parameter, this is not 
the suggested mode of operation, as it might be necessary to maintain an exclusion list
(`-excludeDir` parameter) against internal packages in the project. It is also susceptible to 
Kotlin's internals which may change over time.

Most common use of this will be inside Gradle's build directories in a project:

```kotlin
// Gradle Kotlin DSL, Kotlin
"-classesDir ${project.buildDir}/classes/kotlin/main"
```

```groovy
// Gradle Groovy, Java
"-classessDir " + project.buildDir + "/classes/java/main"
```

### Enums

Because TypeScript reads definition files only at compile-time and discards them on runtime, 
enums inside ``*.d.ts`` will always be undefined - if you have enums, it is suggested to write 
into a ``*.ts`` file instead

### Nullability

jvm2dts can read _Nullable_ annotations and will append ``?`` to the name of a variable.

### JsonProperty (from [com.fasterxml.jackson.core/jackson-annotations](https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-annotations))

`jvm2dts` can read _JsonProperty_ annotations and uses reflection to obtain the `value()`

Similarly, getters annotated with _JsonIgnore_ will be omitted.

# Releasing a new version to Maven Central

```
MAVEN_USERNAME=xxx MAVEN_PASSWORD='xxx' ./gradlew publishSonaTypePublicationToMavenCentralRepository
```

Then navigate to https://oss.sonatype.org/, close, and then release the staging repository.
