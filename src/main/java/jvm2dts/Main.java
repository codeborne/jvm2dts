package jvm2dts;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static java.lang.System.*;
import static java.lang.reflect.Modifier.isFinal;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class Main {

  static class Args {
    @Parameter
    private List<String> packages = new ArrayList<>();

    @Parameter(names = {"-e", "-exclude"}, description = "Excludes classes in the generation matching a Java RegExp pattern")
    private String excludeRegex;

    @Parameter(names = {"-c", "-cast"}, description = "Comma-separated key=value map to make classnames matching the key into specified value")
    private String cast;

    @Parameter(names = {"-d", "-kotlin-data"}, description = "Process only Kotlin data classes, but also enums & interfaces")
    private boolean kotlinDataOnly;

    @Parameter(names = {"-classesDir"}, description = "Recursively look for classes from a location")
    private String classesDir;

    @Parameter(names = {"-excludeDir"}, description = "Comma-separated list to filter out package names when using classesDir")
    private String excludeDirs;

    @Parameter(names = {"-h", "-help"}, help = true)
    private boolean help;
  }

  public static void main(String[] args) throws ClassNotFoundException, IOException {
    var parsedArgs = new Args();
    var jc = JCommander.newBuilder().addObject(parsedArgs).build();
    jc.parse(args);
    if (args.length < 1 || parsedArgs.help) {
      err.println("Example: java -classpath path/to/package " + Main.class.getName() + " -exclude MyRegExp -cast MyClass=number,AnotherClass=string package1 package2 package3");
      jc.usage();
      return;
    }

    var packages = parsedArgs.packages;
    var kotlinDataOnly = parsedArgs.kotlinDataOnly;
    var basePath = Paths.get(parsedArgs.classesDir);
    Set<String> excludeDirs = parsedArgs.excludeDirs == null ? emptySet() :
      stream(parsedArgs.excludeDirs.split(",")).collect(toSet());

    var classLoader = Main.class.getClassLoader();
    if (packages.isEmpty() && parsedArgs.classesDir != null) {
      packages = findPackages(basePath, excludeDirs);
      err.println("Packages detected: " + packages);
    }

    if (packages.isEmpty()) {
      err.println("No packages found");
      jc.usage();
      exit(1);
    }

    var customTypes = new HashMap<Class<?>, String>();
    if (parsedArgs.cast != null) {
      var kvpairs = parsedArgs.cast.split(",");
      for (var pair : kvpairs) {
        customTypes.put(Class.forName(pair.split("=")[0]), pair.split("=")[1]);
      }
    }
    var converter = new Converter(new TypeMapper(customTypes));

    for (var packageName : packages) {
      var packageUrl = classLoader.getResource(packageName.replace('.', '/'));
      if (packageUrl == null) {
        err.println("Cannot load " + packageName + " using ClassLoader, is it missing from classpath?");
        continue;
      }

      if (packageUrl.getProtocol().equals("file")) {
        try {
          var exclude = parsedArgs.excludeRegex;

          Files.walk(Paths.get(packageUrl.toURI())).filter(Files::isRegularFile).filter(path -> {
            var pathString = path.getFileName().toString();
            if (pathString.endsWith(".class")) {
              if (exclude == null) return true;

              var classString = pathString.substring(0, pathString.lastIndexOf('.'));
              if (classString.matches(exclude)) return false;
              return !classString.matches(".*\\$\\d+$");
            }
            return false;
          })
          .map(path -> packageName + "." + path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf('.')))
          .sorted().forEach(className -> {
            try {
              Class<?> clazz = Class.forName(className);
              if (kotlinDataOnly && !isKotlinData(clazz) && !clazz.isEnum() && !clazz.isInterface()) return;
              var converted = converter.convert(clazz);
              if (converted != null) {
                out.print("// ");
                out.println(className);
                out.print("export ");
                out.println(converted);
              }
            } catch (Throwable e) {
              err.println("// Failed to load: " + e);
            }
          });
        } catch (IOException | URISyntaxException e) {
          err.println("// Could not access package " + packageName + ": " + e);
        }
      } else {
        err.println("// Cannot load " + packageUrl + ": unsupported protocol");
        exit(3);
      }
    }
    exit(0);
  }

  private static List<String> findPackages(Path basePath, Set<String> excludeDirs) throws IOException {
    return Files.walk(basePath)
      .filter(Files::isDirectory)
      .filter(path -> !path.getFileName().toString().equals("META-INF"))
      .filter(path -> !path.equals(basePath))
      .filter(path -> !excludeDirs.contains(basePath.relativize(path).toString()))
      .sorted()
      .map(name -> basePath.relativize(name).toString().replace(File.separatorChar, '.'))
      .collect(toList());
  }

  private static boolean isKotlinData(Class<?> clazz) {
    return isFinal(clazz.getModifiers()) && stream(clazz.getDeclaredMethods()).anyMatch(m ->
      m.getName().equals("copy") || m.getName().startsWith("copy-"));
  }
}
