package jvm2dts;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class Main {

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      System.err.println("Usage: " + Main.class.getPackage().getName() + " [package]");
      throw new IllegalStateException("Not enough arguments");
    }
    String packageArg = args[0];

    Converter converter = new Converter();
    URL packageUrl = Main.class.getClassLoader().getResource(packageArg);
    if (packageUrl.getProtocol().equals("file")) {
      for (File file : new File(packageUrl.getPath()).listFiles()) {
        URL[] urls = new URL[]{file.toURI().toURL()};
        ClassLoader cl = new URLClassLoader(urls);

        System.out.println(file.toString());
        System.out.println(converter.convert(Class.forName(file.getName())));
      }
    }
  }
}