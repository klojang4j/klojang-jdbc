package org.klojang.jdbc.x.rs;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.klojang.check.Check;
import org.klojang.jdbc.SQLTypeNames;

import static java.sql.Types.*;
import static org.klojang.check.CommonChecks.keyIn;

class RsMethods {

  private static RsMethods INSTANCE;

  static RsMethods getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new RsMethods();
    }
    return INSTANCE;
  }

  private final Map<Integer, ResultSetMethod<?>> cache;

  private RsMethods() {
    cache = createCache();
  }

  @SuppressWarnings("unchecked")
  <T> ResultSetMethod<T> getMethod(Integer sqlType) {
    // This implicitly checks that the specified int is one of the
    // static final int constants in the java.sql.Types class
    String typeName = SQLTypeNames.getTypeName(sqlType);
    Check.that(sqlType).is(keyIn(), cache, "Unsupported SQL type: %s", typeName);
    return (ResultSetMethod<T>) cache.get(sqlType);
  }

  private static Map<Integer, ResultSetMethod<?>> createCache() {
    Map<Integer, ResultSetMethod<?>> tmp = new HashMap<>();
    tmp.put(VARCHAR, ResultSetMethod.GET_STRING);
    tmp.put(LONGVARCHAR, ResultSetMethod.GET_STRING);
    tmp.put(NVARCHAR, ResultSetMethod.GET_STRING);
    tmp.put(LONGNVARCHAR, ResultSetMethod.GET_STRING);
    tmp.put(CHAR, ResultSetMethod.GET_STRING);
    tmp.put(CLOB, ResultSetMethod.GET_STRING);

    tmp.put(INTEGER, ResultSetMethod.GET_INT);
    tmp.put(SMALLINT, ResultSetMethod.GET_SHORT);
    tmp.put(TINYINT, ResultSetMethod.GET_BYTE);
    tmp.put(BIT, ResultSetMethod.GET_BYTE);
    tmp.put(DOUBLE, ResultSetMethod.GET_DOUBLE);
    tmp.put(REAL, ResultSetMethod.GET_DOUBLE);
    tmp.put(FLOAT, ResultSetMethod.GET_FLOAT);
    tmp.put(BIGINT, ResultSetMethod.GET_LONG);

    tmp.put(BOOLEAN, ResultSetMethod.GET_BOOLEAN);

    tmp.put(DATE, ResultSetMethod.GET_DATE);
    tmp.put(TIME, ResultSetMethod.GET_TIME);

    tmp.put(TIMESTAMP, ResultSetMethod.objectGetter(LocalDateTime.class));
    tmp.put(TIMESTAMP_WITH_TIMEZONE, ResultSetMethod.objectGetter(OffsetDateTime.class));

    tmp.put(NUMERIC, ResultSetMethod.GET_BIG_DECIMAL);
    tmp.put(DECIMAL, ResultSetMethod.GET_BIG_DECIMAL);

    tmp.put(ARRAY, ResultSetMethod.objectGetter(Object[].class));
    return Map.copyOf(tmp);
  }
}
