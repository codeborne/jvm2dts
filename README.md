# jvm2dts

Generates TypeScript definitions (d.ts) for Java/Kotlin model classes that can be used for 
client-side type checking of API calls and responses.

## TODO

* Kotlin nullability support (add ? to TypeScript fields if @Nullable annotation is present on Java fields)
  * Kotlin uses `org.jetbrains.annotations.Nullable`, but it's better to support any annotation with name `Nullable`
* Specify multiple packages on the command-line (current workaround: run program multiple times)
* Support for traversing sub-packages recursively?  
* Optionally specify class-name regexp exclude filters, to exclude e.g. `(Service|Repository|Controller)$`

* GitHub actions already build `jvm2dts` (see .github/workflow/build.yml)
  * Research how to publish the built jar into GitHub Maven repository, so it can be used as a dependency in other projects
  * You may start here: https://docs.github.com/en/free-pro-team@latest/actions/guides/publishing-java-packages-with-gradle
* Add nicer documentation and usage examples to this README file

* Generate types in Anonima in well-defined location, e.g. `ui/api/types.d.ts`
  * e.g. `java -cp jvm2dts.jar:../anonima/build/classes/kotlin/main:~/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/1.4.21/*/kotlin-stdlib-1.4.21.jar jvm2dts.Main auth > ../anonima/ui/api/types.dts`
  * Commit them and use in some .ts files
  * Add to Anonima build (build.gradle.kts) to regenerate types on every build (to check that client-side still compiles with modified server-side)
    * gradle gives access to project's classpath very easily, which can be used to run jvm2dts
