package jvm2dts;

import java.io.File;
import java.net.URL;

public class Main {

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      System.err.println("Usage: java -classpath program:path/to/package " + Main.class.getName() + " <package>");
      System.exit(1);
    }

    String packageName = args[0];
    Converter converter = new Converter();

    URL packageUrl = Main.class.getClassLoader().getResource(packageName);

    if (packageUrl.getProtocol().equals("file"))
      for (File file : new File(packageUrl.toURI()).listFiles()) {

        String path = file.getPath();
        path = path.replaceFirst(".*/" + packageName, packageName).split("\\.")[0].replace("/", ".");

        try {
          System.out.println(converter.convert(Class.forName(path)));
        } catch (ClassNotFoundException e) {
          System.err.println("Failed to load: " + e);
        }
      }
  }
}