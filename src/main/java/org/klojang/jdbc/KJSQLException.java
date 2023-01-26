package org.klojang.jdbc;

import org.klojang.check.Check;
import org.klojang.util.ExceptionMethods;
import org.klojang.util.exception.UncheckedException;

import java.sql.SQLException;

/**
 * A {@link RuntimeException} that may either wrap an {@link SQLException} or
 * indicate a Klojang-native error condition while processing or executing SQL.
 *
 * @author Ayco Holleman
 */
public class KJSQLException extends RuntimeException {

  static RuntimeException wrap(Throwable exc, SQL sql) {
    Check.notNull(exc);
    if (exc instanceof KJSQLException) {
      return (KJSQLException) exc;
    } else if (exc instanceof SQLException) {
      return new KJSQLException(sql, (SQLException) exc);
    } else if (exc.getClass() == UncheckedException.class) {
      // Make sure we can thoughtlessly wrap any exception we encounter
      return wrap(((UncheckedException) exc).unwrap(), sql);
    }
    return ExceptionMethods.uncheck(exc);
  }

  KJSQLException(String message, Object... msgArgs) {
    super(String.format(message, msgArgs));
  }

  KJSQLException(String message, SQLException cause) {
    super(message, cause);
  }

  KJSQLException(SQLException cause) {
    super(cause);
  }

  public KJSQLException(SQL sql, SQLException cause) {
    super(message(sql, cause), cause);
  }

  private static String message(SQL sql, SQLException cause) {
    if (sql == null) {
      return cause.getMessage();
    }
    return cause.getMessage() + " >>>> while executing: " + sql;
  }

}
