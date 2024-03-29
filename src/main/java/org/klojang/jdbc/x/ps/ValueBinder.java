package org.klojang.jdbc.x.ps;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.function.Function;

import static org.klojang.jdbc.x.ps.PreparedStatementMethod.SET_STRING;

/**
 * Binds a single value to a PreparedStatement, possibly after first converting it to the
 * type required by the {@code PreparedStatement.setXXX()} method that is going to be
 * called.
 *
 * @param <INPUT_TYPE> the type of the value to bind (usually associated with a bean
 *       property, record component or map key)
 * @param <PARAM_TYPE> the type required the {@code PreparedStatement.setXXX()}
 *       method
 */
public final class ValueBinder<INPUT_TYPE, PARAM_TYPE> {

  public static final ValueBinder ANY_TO_STRING
        = new ValueBinder<>(SET_STRING, Object::toString);

  public static ValueBinder<? extends TemporalAccessor, String> dateTimeToString(
        DateTimeFormatter dtf) {
    return new ValueBinder<>(SET_STRING, dtf::format);
  }

  private final PreparedStatementMethod<PARAM_TYPE> setter;
  private final Adapter<INPUT_TYPE, PARAM_TYPE> adapter;

  public ValueBinder(PreparedStatementMethod<PARAM_TYPE> setter) {
    this.setter = setter;
    this.adapter = null;
  }

  public ValueBinder(PreparedStatementMethod<PARAM_TYPE> setter,
        Function<INPUT_TYPE, PARAM_TYPE> adapter) {
    this.setter = setter;
    this.adapter = (x, y) -> adapter.apply(x);
  }

  public ValueBinder(PreparedStatementMethod<PARAM_TYPE> setter,
        Adapter<INPUT_TYPE, PARAM_TYPE> adapter) {
    this.setter = setter;
    this.adapter = adapter;
  }

  boolean isAdaptive() { return adapter != null; }

  @SuppressWarnings("unchecked")
  PARAM_TYPE getParamValue(INPUT_TYPE beanValue) {
    return adapter == null ? (PARAM_TYPE) beanValue : adapter.adapt(beanValue,
          setter.getParamType());
  }

  void bind(PreparedStatement ps, int paramIndex, PARAM_TYPE value) throws Throwable {
    if (value == null) {
      ps.setNull(paramIndex, Types.OTHER);
    } else {
      setter.invoke(ps, paramIndex, value);
    }
  }
}
