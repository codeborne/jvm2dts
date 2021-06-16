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

import static jvm2dts.NameConverter.convertName;

public class TypeMapper {
  private Map<Class<?>, String> map = new HashMap<>();

  public TypeMapper(Map<Class<?>, String> customTypes) {
    map.put(byte.class, "number");
    map.put(short.class, "number");
    map.put(int.class, "number");
    map.put(long.class, "number");
    map.put(float.class, "number");
    map.put(double.class, "number");

    map.put(Number.class, "number");
    map.put(Boolean.class, "boolean");
    map.put(String.class, "string");

    map.put(UUID.class, "string");
    map.put(URL.class, "string");
    map.put(URI.class, "string");
    map.put(Currency.class, "string");
    map.put(Locale.class, "string");

    map.put(Date.class, "Date|string");
    map.put(LocalDate.class, "Date|string");
    map.put(LocalTime.class, "Date|string");
    map.put(LocalDateTime.class, "Date|string");

    map.put(Month.class, "Date|string");
    map.put(MonthDay.class, "Date|string");
    map.put(Year.class, "Date|string");
    map.put(YearMonth.class, "Date|string");
    map.put(DayOfWeek.class, "string");

    map.put(Instant.class, "Date|string");
    map.put(Clock.class, "string");
    map.put(Period.class, "string");
    map.put(Duration.class, "string");

    map.put(ChronoLocalDate.class, "Date|string");
    map.put(ChronoLocalDateTime.class, "Date|string");
    map.put(ChronoPeriod.class, "string");

    map.put(OffsetDateTime.class, "Date|string");
    map.put(OffsetTime.class, "Date|string");

    map.put(ZonedDateTime.class, "Date|string");
    map.put(ZoneId.class, "string");
    map.put(ZoneOffset.class, "string");
    map.put(ZoneOffsetTransition.class, "string");
    map.put(ZoneOffsetTransitionRule.class, "string");

    map.putAll(customTypes);
  }

  public String getTSType(Class<?> type) {
    if (type == Object.class) return "any";
    return map.getOrDefault(type, map.getOrDefault(type.getSuperclass(), convertName(type)));
  }
}
