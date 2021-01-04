# jvm2dts

Generates TypeScript definitions (d.ts) for Java/Kotlin model classes that can be used for 
client-side type checking of API calls and responses.

## Usage

### CLI
```
Usage: java -classpath PATH/TO/PACKAGE... [OPTION] jvm2dts.Main CLASS...

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

[Read more about setting class paths in Java (Java SE 8)](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/classpath.html)

The TypeScript interfaces output from stdout and all errors are through stderr (and prefixed by a comment)

### Anonima Usage
```
java 
    -cp 
        out/production/classes:
        ../anonima/anonima/build/classes/kotlin/main:
        ~/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/1.4.21/4a668382d7c38688d3490afde93b6a113ed46698/kotlin-stdlib-1.4.21.jar 
    -exclude 
        (.*)(Service|Repository|Controller)$ 
    auth
```

## TODO

* Kotlin nullability support (add ? to TypeScript fields if @Nullable annotation is present on Java fields)
  * Kotlin uses `org.jetbrains.annotations.Nullable`, but it's better to support any annotation with name `Nullable`
  * While a declared annotation check exists, getting a Nullable annotation in Reflection doesn't appear to work, 
  both getDeclaredAnnotations and getAnnotations return null? (probably RetentionPolicy) 

* GitHub actions already build `jvm2dts` (see .github/workflow/build.yml)
  * Research how to publish the built jar into GitHub Maven repository, so it can be used as a dependency in other projects
  * You may start here: https://docs.github.com/en/free-pro-team@latest/actions/guides/publishing-java-packages-with-gradle
  * Configurations are set up, but does not appear to be working?

* ~~Generate types in Anonima in well-defined location, e.g. `ui/api/types.d.ts`.~~
  * ~~e.g. `java -cp jvm2dts.jar:../anonima/build/classes/kotlin/main:~/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/1.4.21/*/kotlin-stdlib-1.4.21.jar jvm2dts.Main auth > ../anonima/ui/api/types.d.ts`.~~
  * ~~Commit them~~ and use in some .ts files
  * Add to Anonima build (build.gradle.kts) to regenerate types on every build (to check that client-side still compiles with modified server-side)
    * gradle gives access to project's classpath very easily, which can be used to run jvm2dts
