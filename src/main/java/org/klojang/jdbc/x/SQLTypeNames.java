package org.klojang.jdbc.x;

import org.klojang.check.Check;
import org.klojang.util.ExceptionMethods;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.sql.Types;
import java.util.Map;
import java.util.TreeMap;

import static org.klojang.check.CommonChecks.keyIn;

public class SQLTypeNames {

  private static final SQLTypeNames instance = new SQLTypeNames();

  public static String getTypeName(int sqlType) {
    return Check.that((Integer) sqlType)
          .is(keyIn(), instance.map, "no such constant in java.sql.Types: %d", sqlType)
          .ok(instance.map::get);
  }

  public static void printAll(PrintStream out) {
    Check.notNull(out);
    instance.map.forEach((k, v) -> out.printf("%5d : %s%n", k, v));
  }

  private Map<Integer, String> map;

  private SQLTypeNames() {
    Class<Types> clazz = Types.class;
    Field[] fields = clazz.getDeclaredFields();
    map = new TreeMap<>();
    try {
      for (Field f : fields) {
        map.put(f.getInt(null), f.getName());
      }
    } catch (Exception e) {
      throw ExceptionMethods.uncheck(e);
    }
  }
}
