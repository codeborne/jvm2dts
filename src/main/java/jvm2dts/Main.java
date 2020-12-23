package jvm2dts;

import java.io.File;
import java.net.URL;

import static java.lang.System.*;

public class Main {

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      err.println("Usage: java -classpath program:path/to/package " + Main.class.getName() + " <package>");
      exit(1);
    }

    String packageName = args[0];
    Converter converter = new Converter();

    URL packageUrl = Main.class.getClassLoader().getResource(packageName);
    if (packageUrl == null) {
      err.println("Cannot load " + packageName + " using ClassLoader, missing from classpath?");
      exit(2);
    }

    if (packageUrl.getProtocol().equals("file")) {
      for (File file : new File(packageUrl.getPath()).listFiles(f -> f.getName().endsWith(".class"))) {

        String path = file.getPath();
        path = path.replaceFirst(".*/" + packageName, packageName).split("\\.")[0].replace("/", ".");

        try {
          out.println(converter.convert(Class.forName(path)));
        } catch (ClassNotFoundException e) {
          err.println("Failed to load: " + e);
        }
      }
    } else {
      err.println("Cannot load " + packageUrl + ": unsupported protocol");
      exit(3);
    }
  }
}