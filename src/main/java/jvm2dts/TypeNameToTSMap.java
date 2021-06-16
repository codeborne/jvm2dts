package jvm2dts;

import java.net.URI;
import java.net.URL;
import java.time.*;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoPeriod;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneOffsetTransitionRule;
import java.util.*;

import static jvm2dts.NameConverter.getName;

public class TypeNameToTSMap {
  private static final HashMap<Class<?>, String> typeNameToTS = new HashMap<>();

  static {
    typeNameToTS.put(byte.class, "number");
    typeNameToTS.put(short.class, "number");
    typeNameToTS.put(int.class, "number");
    typeNameToTS.put(long.class, "number");
    typeNameToTS.put(float.class, "number");
    typeNameToTS.put(double.class, "number");

    typeNameToTS.put(Number.class, "number");
    typeNameToTS.put(Boolean.class, "boolean");
    typeNameToTS.put(String.class, "string");

    typeNameToTS.put(UUID.class, "string");
    typeNameToTS.put(URL.class, "string");
    typeNameToTS.put(URI.class, "string");
    typeNameToTS.put(Currency.class, "string");
    typeNameToTS.put(Locale.class, "string");

    typeNameToTS.put(Date.class, "Date|string");
    typeNameToTS.put(LocalDate.class, "Date|string");
    typeNameToTS.put(LocalTime.class, "Date|string");
    typeNameToTS.put(LocalDateTime.class, "Date|string");

    typeNameToTS.put(Month.class, "Date|string");
    typeNameToTS.put(MonthDay.class, "Date|string");
    typeNameToTS.put(Year.class, "Date|string");
    typeNameToTS.put(YearMonth.class, "Date|string");
    typeNameToTS.put(DayOfWeek.class, "string");

    typeNameToTS.put(Instant.class, "Date|string");
    typeNameToTS.put(Clock.class, "string");
    typeNameToTS.put(Period.class, "string");
    typeNameToTS.put(Duration.class, "string");

    typeNameToTS.put(ChronoLocalDate.class, "Date|string");
    typeNameToTS.put(ChronoLocalDateTime.class, "Date|string");
    typeNameToTS.put(ChronoPeriod.class, "string");

    typeNameToTS.put(OffsetDateTime.class, "Date|string");
    typeNameToTS.put(OffsetTime.class, "Date|string");

    typeNameToTS.put(ZonedDateTime.class, "Date|string");
    typeNameToTS.put(ZoneId.class, "string");
    typeNameToTS.put(ZoneOffset.class, "string");
    typeNameToTS.put(ZoneOffsetTransition.class, "string");
    typeNameToTS.put(ZoneOffsetTransitionRule.class, "string");

  }

  public static String getTSType(Class<?> javaType) {
    if (javaType == Object.class)
      return "any";
    return typeNameToTS.getOrDefault(javaType, typeNameToTS.getOrDefault(javaType.getSuperclass(), getName(javaType)));
  }
}
