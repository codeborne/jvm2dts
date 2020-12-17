package jvm2dts;

import java.util.logging.Logger;

public interface AbstractConverter {
  Logger logger = Logger.getLogger(AbstractConverter.class.getName());

  default String convert(Class<?> clazz) {
    return "";
  }
}
