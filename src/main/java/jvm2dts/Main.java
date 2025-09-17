package jvm2dts;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.System.err;
import static java.lang.System.out;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

public class Main {
  static class Args {
    @Parameter
    private List<String> packages = new ArrayList<>();

    @Parameter(names = {"-e", "-exclude"}, description = "Excludes classes in the generation with qualified name matching a RegExp")
    private String excludeRegex;

    @Parameter(names = {"-c", "-cast"}, description = "Comma-separated key=value map to make classnames matching the key into specified value")
    private String cast;

    @Parameter(names = {"-d", "-data-only"}, description = "Process only data classes (which have implemented equals), but also enums & interfaces")
    private boolean dataOnly;

    @Parameter(names = {"-annotated"}, description = "Process only annotated classes with comma-separated annotations (note that lombok annotations are not visible in class files), but also enums & interfaces")
    private String withAnnotations;

    @Parameter(names = {"-classesDir"}, description = "Recursively look for classes from a location")
    private String classesDir;

    @Parameter(names = {"-excludeDir"}, description = "Comma-separated list to filter out package names when using classesDir")
    private String excludeDirs;

    @Parameter(names = {"-h", "-help"}, help = true)
    private boolean help;
  }

  public static void main(String[] args) throws Exception {
    var parsedArgs = new Args();
    var jc = JCommander.newBuilder().addObject(parsedArgs).build();
    jc.parse(args);
    if (args.length < 1 || parsedArgs.help) {
      err.println("Example: java -classpath path/to/package " + Main.class.getName() + " -exclude MyRegExp -cast MyClass=number,AnotherClass=string package1 package2 package3");
      jc.usage();
      return;
    }

    var packages = new LinkedHashSet<>(parsedArgs.packages);
    var withAnnotations = parsedArgs.withAnnotations != null ? stream(parsedArgs.withAnnotations.split(",")).collect(toSet()) : null;
    var basePath = Paths.get(parsedArgs.classesDir);

    var customTypes = new HashMap<Class<?>, String>();
    if (parsedArgs.cast != null) {
      var kvpairs = parsedArgs.cast.split(",");
      for (var pair : kvpairs) {
        customTypes.put(Class.forName(pair.split("=")[0]), pair.split("=")[1]);
      }
    }
    var converter = new Converter(new TypeMapper(customTypes));

    try {
      var exclude = parsedArgs.excludeRegex;
      Files.walk(basePath)
        .filter(Files::isRegularFile)
        .filter(path -> path.getFileName().toString().endsWith(".class"))
        .map(path -> toClassName(path, basePath))
        .filter(className -> {
          if (exclude == null) return true;
          return !className.matches(exclude);
        })
        .filter(className -> isInPackage(className, packages))
        .sorted()
        .forEach(className -> processClass(className, converter, parsedArgs.dataOnly, withAnnotations));
    } catch (IOException e) {
      err.println("// Could not access classes in " + basePath + ": " + e);
    }
  }

  private static String toClassName(Path path, Path basePath) {
    var pathString = path.toString();
    var baseString = basePath.toString();
    var relativePath = pathString.substring(baseString.length() + 1);
    return relativePath.substring(0, relativePath.length() - ".class".length()).replace('/', '.');
  }

  private static boolean isInPackage(String className, Set<String> packages) {
    if (packages.isEmpty()) return true;
    var lastDot = className.lastIndexOf('.');
    return lastDot != -1 && packages.contains(className.substring(0, lastDot));
  }

  private static void processClass(String className, Converter converter, boolean dataOnly, Set<String> withAnnotations) {
    try {
      Class<?> clazz = Class.forName(className);
      if ((dataOnly && !isData(clazz) || withAnnotations != null && !isAnnotated(clazz, withAnnotations)) &&
        !clazz.isEnum() && !clazz.isInterface()) {
        return;
      }
      var converted = converter.convert(clazz);
      if (converted != null) {
        out.println("// " + className);
        out.println("export " + converted);
      }
    } catch (Throwable e) {
      err.println("// Failed to load class " + className + ": " + e.getMessage());
    }
  }

  private static boolean isData(Class<?> clazz) {
    try {
      clazz.getDeclaredMethod("equals", Object.class);
      return true;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  private static boolean isAnnotated(Class<?> clazz, Set<String> annotations) {
    return stream(clazz.getAnnotations()).anyMatch(a -> annotations.contains(a.annotationType().getName()));
  }
}
