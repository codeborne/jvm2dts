package jvm2dts;

import java.util.logging.Logger;

public interface ToTypeScriptConverter {
  Logger logger = Logger.getLogger(ToTypeScriptConverter.class.getName());

  String convert(Class<?> clazz);

}
