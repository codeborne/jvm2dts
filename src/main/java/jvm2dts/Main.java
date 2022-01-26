package jvm2dts;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.System.*;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

public class Main {

  static class Args {
    @Parameter
    private List<String> packages = new ArrayList<>();

    @Parameter(names = {"-e", "-exclude"}, description = "Excludes classes in the generation matching a Java RegExp pattern")
    private String excludeRegex;

    @Parameter(names = {"-c", "-cast"}, description = "Comma-separated key=value map to make classnames matching the key into specified value")
    private String cast;

    @Parameter(names = {"-classesDir"}, description = "Recursively look for classes from a location")
    private String classesDir;

    @Parameter(names = {"-excludeDir"}, description = "Comma-separated list to filter out package names when using classesDir")
    private String excludeDirs;

    @Parameter(names = {"-h", "-help"}, help = true)
    private boolean help;
  }

  public static void main(String[] args) throws ClassNotFoundException {
    Args parsedArgs = new Args();
    JCommander jc = JCommander.newBuilder()
      .addObject(parsedArgs)
      .build();
    jc.parse(args);
    if (args.length < 1 || parsedArgs.help) {
      err.println("Example: java -classpath path/to/package " + Main.class.getName() + " -exclude MyRegExp -cast MyClass=number,AnotherClass=string package1 package2 package3");
      jc.usage();
      exit(0);
    }

    List<String> packages = parsedArgs.packages;
    Path basePath = Paths.get(parsedArgs.classesDir);
    Set<String> excludeDirs = parsedArgs.excludeDirs == null ? emptySet() :
            stream(parsedArgs.excludeDirs.split(",")).collect(toSet());

    ClassLoader classLoader = Main.class.getClassLoader();
    if (packages.isEmpty() && parsedArgs.classesDir != null) {
      try {
        Files.walk(basePath)
          .sorted()
          .filter(Files::isDirectory)
          .filter(path -> !path.getFileName().toString().equals("META-INF"))
          .filter(path -> !path.equals(basePath))
          .filter(path -> !excludeDirs.contains(basePath.relativize(path).toString()))
          .filter(path -> {
            try {
              return Files.list(path).anyMatch(Files::isRegularFile);
            } catch (IOException ex) {
              err.println("Failed to walk directory for files: " + path);
              ex.printStackTrace();
              return false;
            }
          })
          .forEach(name -> packages.add(basePath.relativize(name).toString()));
      } catch (IOException e) {
        e.printStackTrace();
        exit(2);
      }
      err.println("Packages detected: " + packages);
    }

    if (packages.isEmpty()) {
      err.println("No packages found");
      jc.usage();
      exit(1);
    }

    final Map<Class<?>, String> customTypes = new HashMap<>();
    if (parsedArgs.cast != null) {
      String[] kvpairs = parsedArgs.cast.split(",");
      for (String pair : kvpairs) {
        customTypes.put(Class.forName(pair.split("=")[0]), pair.split("=")[1]);
      }
    }
    Converter converter = new Converter(new TypeMapper(customTypes));

    for (String packageName : packages) {
      URL packageUrl = classLoader.getResource(packageName.replace('.', '/'));
      if (packageUrl == null) {
        err.println("Cannot load " + packageName + " using ClassLoader, is it missing from classpath?");
        continue;
      }

      if (packageUrl.getProtocol().equals("file")) {
        try {
          final String exclude = parsedArgs.excludeRegex;

          Files.walk(Paths.get(packageUrl.getPath())).filter(Files::isRegularFile).filter(path -> {
            String pathString = path.getFileName().toString();
            if (pathString.endsWith(".class")) {
              if (exclude == null) return true;

              String classString = pathString.substring(0, pathString.lastIndexOf('.'));
              if (classString.matches(exclude)) return false;
              return !classString.matches(".*\\$\\d+$");
            }
            return false;
          })
          .map(path -> packageName + "." + path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf('.')))
          .sorted().forEach(className -> {
            try {
              String converted = converter.convert(Class.forName(className));
              if (!converted.isEmpty()) {
                out.print("// ");
                out.println(className);
                out.print("export ");
                out.println(converted);
              }
            } catch (Throwable e) {
              err.println("// Failed to load: " + e);
            }
          });
        } catch (IOException e) {
          err.println("// Could not access package " + packageName + ": " + e);
        }
      } else {
        err.println("// Cannot load " + packageUrl + ": unsupported protocol");
        exit(3);
      }
    }
    exit(0);
  }
}
