# jvm2dts

Generates TypeScript definitions (d.ts) for Java/Kotlin model classes using reflection/asm 
that can be used for client-side type checking of API calls and responses.

## Usage

Java version must be at least 11

### CLI
```
Example: java -classpath path/to/package jvm2dts.Main -exclude regexp -cast MyClass=number,AnotherClass=string package
Usage: <main class> [options]
  Options:
    -c, -cast
      Comma-separated key=value map to make classnames (only inside interface) matching key into value
    -e, -exclude
      Excludes classes in the generation matching a Java RegExp pattern
    -h, -help
```

[Read more about setting class paths in Java](https://docs.oracle.com/javase/11/docs/technotes/tools/windows/classpath.html)

The TypeScript interfaces output from stdout and all errors are through stderr (and prefixed by a comment)

### Using in Gradle (Kotlin DSL)

```kotlin
val jvm2dts by configurations.creating

// Required dependencies
dependencies {
  jvm2dts("com.codeborne:jvm2dts:1.2.8")
}

// Create the Gradle task to generate TypeScript interfaces and enums
// This buffers the standard output of the task into a stream, then gets written to a file
tasks.register("generateTSTypes") { 
  dependsOn("classes")
  doLast {
      val mainSource = sourceSets.main.get()
      File("ui/api/types.ts").writeText(ByteArrayOutputStream().use { out ->
      project.exec {
        standardOutput = out
        commandLine = """java -classpath ${(jvm2dts + sourceSets.main.get().runtimeClasspath).asPath}
          jvm2dts.Main 
          -exclude .*SuffixOfClassNameIDontWant|PrefixOfClassNameIDontWant.*
          -cast MyNumericClass=number
          mypackage1 mypackage2 ...""".split("\\s+".toRegex())
      }
      out.toString()
    })
  }
}
```

### Enums

Because TypeScript reads definition files only at compile-time, enums inside ``*.d.ts``
will always be undefined - if you have enums, it is suggested to write into a ``*.ts`` file instead

### Nullability

jvm2dts can read _Nullable_ annotations and will append ``?`` to the name of a variable.
This is done using ASM, so it may need to be updated once new versions of Java release.

### JsonProperty (from [com.fasterxml.jackson.core/jackson-annotations](https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-annotations))

jvm2dts can read _JsonProperty_ annotations and uses reflection to obtain the value()
- If using Kotlin, the annotations should **target the field** to be accessible for jvm2dts