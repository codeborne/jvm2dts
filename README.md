# jvm2dts

Generates TypeScript definitions (d.ts) for Java/Kotlin model classes from Reflection 
that can be used for client-side type checking of API calls and responses.

## Usage

Java SDK version must be at least 11

### CLI
```
Usage: java -classpath PATH/TO/PACKAGE... jvm2dts.Main [OPTION] CLASS...

    -exclude REGEXP     Exclude classes matching the Java RegExp pattern    
```

On **Windows**, separate multiple class paths by **semicolons** (`;`)  
``` 
java -classpath path1;path2\to;path3\to\package
```

On **Unix**, class path separation works by **colons** (`:`)  
```
java -classpath path1:path2/to:path3/to/package
```

[Read more about setting class paths in Java](https://docs.oracle.com/javase/11/docs/technotes/tools/windows/classpath.html)

The TypeScript interfaces output from stdout and all errors are through stderr (and prefixed by a comment)

### Using in Gradle Kotlin DSL

```kotlin
// Required dependencies
dependencies {
  compileOnly("com.codeborne:jvm2dts:1.0.1")
}

// Create the Gradle task to generate TypeScript interfaces and enums
// This buffers the standard output of the task into a stream, then gets written to a file
tasks.register("createTSTypes") {
  val outputText: String = ByteArrayOutputStream().use { outputStream ->
    project.exec {
      standardOutput = outputStream
      val command = "java -cp " +
        sourceSets.main.get().runtimeClasspath.asPath + ":" + 
        sourceSets.main.get().compileClasspath.asPath + " "
        "jvm2dts.Main " +
        "-exclude (.*)SuffixOfClassNameIDontWant" + // or PrefixOfClassNameIDontWant(.*)
        "mypackage1 mypackage2 ..."
      commandLine = command.split(" ")
    }
    outputStream.toString()
  }

  File("target/path/myTypes.d.ts").writeText(outputText)
}
```

### Enums

Due to how definition files in TypeScript work, enums inside ``*.d.ts``
will always be undefined - if you have enums, it is suggested to write into a ``*.ts`` file instead

## TODO

* Kotlin nullability support (add ? to TypeScript fields if @Nullable annotation is present on Java fields)
  * Kotlin uses `org.jetbrains.annotations.Nullable`, but it's better to support any annotation with name `Nullable`
  * While a declared annotation check exists, getting a Nullable annotation in Reflection doesn't appear to work, 
  both getDeclaredAnnotations and getAnnotations return empty? (possibly RetentionPolicy of the annotation) 
