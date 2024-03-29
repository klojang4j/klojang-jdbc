package org.klojang.jdbc;

import org.klojang.check.Check;
import org.klojang.jdbc.x.JDBC;
import org.klojang.jdbc.x.sql.ParamExtractor;
import org.klojang.jdbc.x.sql.ParameterInfo;
import org.klojang.templates.RenderSession;
import org.klojang.util.ArrayMethods;
import org.klojang.util.CollectionMethods;

import java.sql.Connection;
import java.util.Collection;

import static org.klojang.check.Tag.PATH;
import static org.klojang.jdbc.x.Strings.IDENTIFIER;
import static org.klojang.jdbc.x.Strings.VAR_NAME;

final class SQLSkeletonSession extends DynamicSQLSession {

  SQLSkeletonSession(Connection con, AbstractSQL sql, RenderSession session) {
    super(con, sql, session);
  }

  public SQLSession setNested(String path, Object value) {
    Check.notNull(path, PATH);
    if (value instanceof Collection<?> c) {
      session.setPath(path, i -> CollectionMethods.implode(c));
    } else if (value.getClass().isArray()) {
      session.setPath(path, i -> ArrayMethods.implodeAny(value));
    } else {
      session.setPath(path, i -> value);
    }
    return this;
  }

  public SQLSession setNestedValue(String path, Object value) {
    Check.notNull(path, PATH);
    switch (value) {
      case Collection<?> x -> {
        String val = CollectionMethods.implode(x, this::quoteValue, ",");
        session.setPath(path, i -> val);
      }
      case Object[] x -> {
        String val = ArrayMethods.implode(x, this::quoteValue, ",");
        session.setPath(path, i -> val);
      }
      case int[] x -> {
        String val = ArrayMethods.implodeInts(x, ",");
        session.setPath(path, i -> val);
      }
      default -> {
        if (value.getClass().isArray()) {
          String val = ArrayMethods.implodeAny(value, this::quoteValue, ",");
          session.setPath(path, i -> val);
        } else {
          session.setPath(path, i -> quoteValue(value));
        }
      }
    }
    return this;
  }

  public SQLSession setNestedIdentifier(String varName, String identifier) {
    Check.notNull(varName, VAR_NAME);
    Check.notNull(identifier, IDENTIFIER);
    session.setPath(varName, i -> quoteIdentifier(identifier));
    return this;
  }

  @Override
  public SQLQuery prepareQuery() {
    close();
    var paramInfo = getParamInfo();
    var stmt = JDBC.getPreparedStatement(con, paramInfo);
    return new SQLQuery(stmt, this, paramInfo);
  }

  @Override
  public SQLInsert prepareInsert(boolean retrieveKeys) {
    close();
    var paramInfo = getParamInfo();
    var stmt = JDBC.getPreparedStatement(con, paramInfo, retrieveKeys);
    return new SQLInsert(stmt, this, paramInfo, retrieveKeys);
  }

  @Override
  public SQLUpdate prepareUpdate() {
    close();
    var paramInfo = getParamInfo();
    var stmt = JDBC.getPreparedStatement(con, paramInfo);
    return new SQLUpdate(stmt, this, paramInfo);
  }

  private ParameterInfo getParamInfo() {
    return new ParameterInfo(new ParamExtractor(session.render()));
  }

}
