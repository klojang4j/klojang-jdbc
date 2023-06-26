package org.klojang.jdbc;

import org.klojang.check.Check;
import org.klojang.check.ObjectCheck;
import org.klojang.invoke.Getter;
import org.klojang.invoke.GetterFactory;
import org.klojang.templates.NameMapper;

import java.sql.Connection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.joining;
import static org.klojang.check.CommonChecks.*;
import static org.klojang.check.CommonExceptions.STATE;
import static org.klojang.util.ObjectMethods.ifNull;
import static org.klojang.util.ObjectMethods.isEmpty;
import static org.klojang.util.StringMethods.append;

public class SQLInsertBuilder {

  private Class<?> beanClass;
  private String tableName;
  private String[] properties;
  private boolean exclude;
  private NameMapper mapper = NameMapper.AS_IS;
  private BindInfo bindInfo = new BindInfo() {};

  SQLInsertBuilder() {}

  public SQLInsertBuilder of(Class<?> beanClass) {
    this.beanClass = Check.notNull(beanClass).ok();
    return this;
  }

  public SQLInsertBuilder into(String tableName) {
    this.tableName = Check.that(tableName).isNot(empty()).ok();
    return this;
  }

  public SQLInsertBuilder excluding(String... properties) {
    this.properties = Check.notNull(properties, "properties").ok();
    this.exclude = true;
    return this;
  }

  public SQLInsertBuilder including(String... properties) {
    this.properties = Check.notNull(properties, "properties").ok();
    this.exclude = false;
    return this;
  }

  /**
   * Sets the {@code NameMapper} to be used when mapping bean properties to column names.
   * Beware of the direction of the mappings: <i>from</i> bean properties <i>to</i> column
   * names.
   *
   * @param propertyToColumnMapper
   * @return
   */
  public SQLInsertBuilder withMapper(NameMapper propertyToColumnMapper) {
    this.mapper = Check.notNull(propertyToColumnMapper).ok();
    return this;
  }

  public SQLInsertBuilder withBindInfo(BindInfo bindInfo) {
    this.bindInfo = Check.notNull(bindInfo).ok();
    return this;
  }

  public SQLInsert prepare(Connection con) {
    Check.notNull(con);
    Check.on(STATE, beanClass, "beanClass").is(notNull());
    Map<String, Getter> getters = GetterFactory.INSTANCE.getGetters(beanClass, true);
    Set<String> props = getters.keySet();
    if (!isEmpty(properties)) {
      if (exclude) {
        props = new LinkedHashSet<>(props);
        for (String prop : properties) {
          checkProperty(props, prop).then(props::remove);
        }
      } else {
        Set<String> tmp = new LinkedHashSet<>(properties.length);
        for (String prop : properties) {
          checkProperty(props, prop).then(tmp::add);
        }
        props = tmp;
      }
    }
    String cols = props.stream().map(mapper::map).collect(joining(","));
    String params = props.stream().map(s -> ":" + s).collect(joining(","));
    String table = ifNull(tableName, beanClass.getSimpleName());
    StringBuilder sb = new StringBuilder(100);
    append(sb, "INSERT INTO ", table, " (", cols, ") VALUES(", params, ")");
    SQL sql = SQL.parametrized(sb.toString(), bindInfo);
    return sql.prepareInsert(con);
  }

  private ObjectCheck<String, IllegalStateException> checkProperty(Set<String> props,
        String prop) {
    return Check.on(STATE, prop)
          .isNot(empty(), "Empty property name not allowed")
          .is(in(), props, "No such property in %s: %s", beanClass.getSimpleName(), prop);
  }
}
