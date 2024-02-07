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

import static org.klojang.check.CommonChecks.*;
import static org.klojang.check.CommonExceptions.STATE;
import static org.klojang.check.CommonExceptions.illegalState;
import static org.klojang.check.Tag.PROPERTIES;
import static org.klojang.jdbc.x.Strings.BEAN_CLASS;
import static org.klojang.util.CollectionMethods.implode;
import static org.klojang.util.ObjectMethods.ifNull;
import static org.klojang.util.ObjectMethods.isEmpty;
import static org.klojang.util.StringMethods.append;

/**
 * A {@code Builder} class for {@link SQLInsert} instances.
 */
public final class InsertBuilder {

  static final String PROPERTIES_ALREADY_SET = "properties to include/exclude can only be set once";
  static final String EMPTY_PROPERTY_NAME = "empty property name not allowed";
  static final String NO_SUCH_PROPERTY = "no such property in ${0}: ${arg}";

  private Class<?> beanClass;
  private String tableName;
  private String[] properties;
  private boolean exclude;

  private NameMapper mapper = NameMapper.AS_IS;
  private BindInfo bindInfo = new BindInfo() { };
  private boolean returnKeys = true;

  InsertBuilder() { }

  /**
   * Sets the type of the beans to be persisted.
   *
   * @param beanClass the type of the beans to be persisted
   * @return this {@code SQLInsertBuilder}
   */
  public InsertBuilder of(Class<?> beanClass) {
    this.beanClass = Check.notNull(beanClass).ok();
    return this;
  }

  /**
   * Sets the table name to insert the data into. If not specified, this defaults to the
   * simple name of the bean class.
   *
   * @param tableName the table name to insert the data into
   * @return this {@code SQLInsertBuilder}
   */
  public InsertBuilder into(String tableName) {
    this.tableName = Check.that(tableName).isNot(empty()).ok();
    return this;
  }

  /**
   * Sets the properties (and corresponding columns) to exclude from the INSERT statement.
   * You would most likely at least want to exclude the property corresponding to the
   * auto-generated key column. It is not allowed to call this method more than once, or
   * to call both this method and the {@link #including(String...)} method.
   *
   * @param properties the properties (and corresponding columns) to exclude from
   *       the INSERT statement
   * @return this {@code SQLInsertBuilder}
   */
  public InsertBuilder excluding(String... properties) {
    Check.that(this.properties).is(NULL(), illegalState(PROPERTIES_ALREADY_SET));
    this.properties = Check.notNull(properties, PROPERTIES).ok();
    this.exclude = true;
    return this;
  }

  /**
   * Sets the properties (and corresponding columns) to include in the INSERT statement.
   * It is not allowed to call this method more than once, or to call both this method and
   * the {@link #excluding(String...)} method.
   *
   * @param properties the properties (and corresponding columns) to include in the
   *       INSERT statement
   * @return this {@code SQLInsertBuilder}
   */
  public InsertBuilder including(String... properties) {
    Check.that(this.properties).is(NULL(), illegalState(PROPERTIES_ALREADY_SET));
    this.properties = Check.notNull(properties, PROPERTIES).ok();
    this.exclude = false;
    return this;
  }

  /**
   * Specifies whether auto-generated keys should be retrieved from the database. By
   * default this is the case. Therefore, if the INSERT statement does not cause any keys
   * to be generated by the database, do call this method to disable this feature.
   * Otherwise the JDBC driver may throw an {@link java.sql.SQLException SQLException}.
   *
   * @param b whether auto-generated keys should be retrieved from the database
   * @return this {@code SQLInsertBuilder}
   */
  public InsertBuilder retrieveKeys(boolean b) {
    this.returnKeys = b;
    return this;
  }

  /**
   * Sets the property-to-column mapper to be used when mapping bean properties to column
   * names. Beware of the direction of the mappings: <i>from</i> bean properties
   * <i>to</i> column names.
   *
   * @param propertyToColumnMapper the property-to-column mapper
   * @return this {@code SQLInsertBuilder}
   */
  public InsertBuilder withMapper(NameMapper propertyToColumnMapper) {
    this.mapper = Check.notNull(propertyToColumnMapper).ok();
    return this;
  }

  /**
   * Sets the {@link BindInfo} object to be used to fine-tune the binding process.
   *
   * @param bindInfo the {@code BindInfo} object to be used to fine-tune the binding
   *       process
   * @return this {@code SQLInsertBuilder}
   */
  public InsertBuilder withBindInfo(BindInfo bindInfo) {
    this.bindInfo = Check.notNull(bindInfo).ok();
    return this;
  }

  /**
   * Returns an {@code SQLInsert} instance that allows you to execute the INSERT statement.
   *
   * @param con the JDBC {@code Connection} to use for the INSERT statement
   * @return a {@code SQLInsert} instance that allows you to execute the INSERT statement
   */
  public SQLInsert prepare(Connection con) {
    Check.notNull(con);
    Check.on(STATE, beanClass, BEAN_CLASS).is(notNull());
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
    String cols = implode(props, mapper::map, ",");
    String params = implode(props, s -> ":" + s, ",");
    String table = ifNull(tableName, beanClass.getSimpleName());
    StringBuilder sb = new StringBuilder(100);
    append(sb, "INSERT INTO ", table, " (", cols, ") VALUES(", params, ")");
    SQLSession sql = SQL.simple(sb.toString(), bindInfo).session(con);
    return sql.prepareInsert(returnKeys);
  }

  private ObjectCheck<String, IllegalStateException> checkProperty(Set<String> props,
        String prop) {
    return Check.on(STATE, prop)
          .isNot(emptyString(), EMPTY_PROPERTY_NAME)
          .is(in(), props, NO_SUCH_PROPERTY, beanClass.getSimpleName());
  }
}
