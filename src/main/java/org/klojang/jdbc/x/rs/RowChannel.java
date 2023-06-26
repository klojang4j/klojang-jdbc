package org.klojang.jdbc.x.rs;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.klojang.jdbc.Row;
import org.klojang.templates.NameMapper;
import org.klojang.util.ExceptionMethods;

/* Transports a single value from a ResultSet to a Map<String,Object> */
@SuppressWarnings("rawtypes")
public class RowChannel<COLUMN_TYPE> implements Channel<Row> {

  public static Row toRow(ResultSet rs, RowChannel[] channels) throws Throwable {
    Row row = new Row(channels.length);
    for (RowChannel channel : channels) {
      channel.copy(rs, row);
    }
    return row;
  }

  public static RowChannel[] createChannels(ResultSet rs, NameMapper mapper) {
    RsMethods methods = RsMethods.getInstance();
    try {
      ResultSetMetaData rsmd = rs.getMetaData();
      int sz = rsmd.getColumnCount();
      RowChannel[] transporters = new RowChannel[sz];
      for (int idx = 0; idx < sz; ++idx) {
        int jdbcIdx = idx + 1; // JDBC is one-based
        int sqlType = rsmd.getColumnType(jdbcIdx);
        ResultSetMethod<?> method = methods.getMethod(sqlType);
        String label = rsmd.getColumnLabel(jdbcIdx);
        String key = mapper.map(label);
        transporters[idx] = new RowChannel<>(method, jdbcIdx, sqlType, key);
      }
      return transporters;
    } catch (SQLException e) {
      throw ExceptionMethods.uncheck(e);
    }
  }

  private final ResultSetMethod<COLUMN_TYPE> method;
  private final int jdbcIdx;
  private final int sqlType;
  private final String key;

  private RowChannel(ResultSetMethod<COLUMN_TYPE> method, int jdbcIdx, int sqlType, String key) {
    this.method = method;
    this.jdbcIdx = jdbcIdx;
    this.sqlType = sqlType;
    this.key = key;
  }

  @Override
  public void copy(ResultSet rs, Row row) throws Throwable {
    row.addColumn(key, method.invoke(rs, jdbcIdx));
  }

  @Override
  public int getSqlType() {
    return sqlType;
  }
}
