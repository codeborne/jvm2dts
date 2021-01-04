package jvm2dts;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static java.lang.System.*;

public class Main {

  public static void main(String[] args) {
    if (args.length < 1) {
      err.println("Usage: java -classpath path/to/package " + Main.class.getName() + "[-exclude regexp] <package>");
      exit(1);
    }

    String exclude = null;
    if (args[0].equals("-exclude") && args.length > 1) {
      exclude = args[1];
    }

    for (int i = 0; i < args.length; i++) {
      if (exclude != null && i < 2)
        continue;

      String packageName = args[i];
      Converter converter = new Converter();

      URL packageUrl = Main.class.getClassLoader().getResource(packageName.replace('.', '/'));
      if (packageUrl == null) {
        err.println("Cannot load " + packageName + " using ClassLoader, missing from classpath?");
        exit(2);
      }

      if (packageUrl.getProtocol().equals("file")) {
        final String finalExclude = exclude;
        try {
          Files.walk(Paths.get(packageUrl.getPath())).filter(Files::isRegularFile)
            .filter(path -> {
                if (path.toString().endsWith(".class")) {
                  return finalExclude == null ||
                    !path.toString()
                      .substring(0, path.toString().lastIndexOf('.'))
                      .matches(finalExclude);
                }
                return false;
              }
            )
            .map(path -> packageName + "." + path.toString().substring(0, path.toString().lastIndexOf('.')))
            .sorted().forEach(className -> {
            try {
              String converted = converter.convert(Class.forName(className));
              if (!converted.isEmpty()) {
                out.print("// ");
                out.println(className);
                out.println(converted);
              }
            } catch (Throwable e) {
              err.println("Failed to load: " + e);
            }
          });
        } catch (IOException e) {
          err.println("Could not access package " + packageName + ": " + e);
        }
      } else {
        err.println("Cannot load " + packageUrl + ": unsupported protocol");
        exit(3);
      }
    }
  }
}