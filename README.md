# jvm2dts

Generates TypeScript definitions (d.ts) for Java/Kotlin model classes using reflection/asm 
that can be used for client-side type checking of API calls and responses.

## Usage

Java version must be at least 11

### CLI
```
Usage: java -classpath PATH/TO/PACKAGE... jvm2dts.Main [OPTION] CLASS...

    -exclude REGEXP     Exclude classes matching the Java RegExp pattern    
```

[Read more about setting class paths in Java](https://docs.oracle.com/javase/11/docs/technotes/tools/windows/classpath.html)

The TypeScript interfaces output from stdout and all errors are through stderr (and prefixed by a comment)

### Using in Gradle (Kotlin DSL)

```kotlin
// Required dependencies
dependencies {
  compileOnly("com.codeborne:jvm2dts:1.1.4")
}

// Create the Gradle task to generate TypeScript interfaces and enums
// This buffers the standard output of the task into a stream, then gets written to a file
tasks.register("generateTSTypes") {
  doLast {
    File("ui/api/types.ts").writeText(ByteArrayOutputStream().use { out ->
      project.exec {
        standardOutput = out
        commandLine = """java -classpath ${sourceSets.main.get().runtimeClasspath.asPath}${File.pathSeparator}${sourceSets.main.get().compileClasspath.asPath}
          jvm2dts.Main -exclude (.*SuffixOfClassNameIDontWant|PrefixOfClassNameIDontWant.*)"
          mypackage1 mypackage2 ...""".split("\\s+".toRegex())
      }
      out.toString()
    })
  }
}
```

### Enums

Due to how definition files in TypeScript work, enums inside ``*.d.ts``
will always be undefined - if you have enums, it is suggested to write into a ``*.ts`` file instead

### Nullability

jvm2dts can read *Nullable* annotations and will append ``?`` to the name of a variable.
This is done using ASM, so it may need to be updated once new versions of Java release.
