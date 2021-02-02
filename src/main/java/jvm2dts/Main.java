package jvm2dts;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.System.*;

public class Main {

  static class Args {
    @Parameter
    private List<String> packages = new ArrayList<>();

    @Parameter(names = {"-e", "-exclude"}, description = "Excludes classes in the generation matching a Java RegExp pattern")
    private String excludeRegex;

    @Parameter(names = {"-c", "-cast"}, description = "Comma-separated key=value map to make classnames matching key into value")
    private String cast;

    @Parameter(names = {"-h", "-help"}, help = true)
    private boolean help;

  }

  public static void main(String[] args) {
    Args parsedArgs = new Args();
    JCommander jc = JCommander.newBuilder()
      .addObject(parsedArgs)
      .build();
    jc.parse(args);
    if (args.length < 1 || parsedArgs.help) {
      err.println("Example: java -classpath path/to/package " + Main.class.getName() + " -exclude regexp -cast MyClass=number,AnotherClass=string package");
      jc.usage();
      exit(0);
    }


    for (int i = 0; i < parsedArgs.packages.size(); i++) {

      String packageName = parsedArgs.packages.get(i);
      Converter converter = new Converter();

      URL packageUrl = Main.class.getClassLoader().getResource(packageName.replace('.', '/'));
      if (packageUrl == null) {
        err.println("Cannot load " + packageName + " using ClassLoader, is it missing from classpath?");
        exit(2);
      }

      if (packageUrl.getProtocol().equals("file")) {
        try {
          final String exclude = parsedArgs.excludeRegex;
          final Map<String, String> castMap = new HashMap<>();

          if (parsedArgs.cast != null) {
            String[] kvpairs = parsedArgs.cast.split(",");
            for (String pair : kvpairs) {
              castMap.put(pair.split("=")[0], pair.split("=")[1]);
            }
          }

          Files.walk(Paths.get(packageUrl.getPath())).filter(Files::isRegularFile)
            .filter(path -> {
                String pathString = path.getFileName().toString();
                if (pathString.endsWith(".class")) {
                  if (exclude == null) return true;

                  String classString = pathString.substring(0, pathString.lastIndexOf('.'));
                  if (classString.matches(exclude)) return false;
                  return !classString.matches(".*\\$\\d+$");
                }
                return false;
              }
            )
            .map(path -> packageName + "." + path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf('.')))
            .sorted().forEach(className -> {
            try {
              String converted = converter.convert(Class.forName(className), castMap);
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
  }
}