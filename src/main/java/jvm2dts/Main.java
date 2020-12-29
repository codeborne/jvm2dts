package jvm2dts;

import java.io.File;
import java.net.URL;
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
        Arrays.stream(new File(packageUrl.getPath()).listFiles())
          .filter(f -> {
              if (f.getName().endsWith(".class")) {
                return finalExclude == null ||
                  !f.getName()
                    .substring(0, f.getName().lastIndexOf('.'))
                    .matches(finalExclude);
              }
              return false;
            }
          )
          .map(f -> packageName + "." + f.getName().substring(0, f.getName().lastIndexOf('.')))
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
      } else {
        err.println("Cannot load " + packageUrl + ": unsupported protocol");
        exit(3);
      }
    }
  }
}