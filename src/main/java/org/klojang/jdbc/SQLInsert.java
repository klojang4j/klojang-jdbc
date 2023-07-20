package org.klojang.jdbc;

import org.klojang.check.Check;
import org.klojang.jdbc.x.JDBC;
import org.klojang.jdbc.x.sql.SQLInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.klojang.check.CommonChecks.empty;
import static org.klojang.check.CommonChecks.yes;
import static org.klojang.check.CommonExceptions.illegalState;

/**
 * Facilitates the execution of SQL INSERT statements.
 */
public final class SQLInsert extends SQLStatement<SQLInsert> {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(SQLInsert.class);

  private static final String DIRTY_INSTANCE =
        "insertAll() not allowed on dirty instance; call reset() first";
  private static final String ID_PROPERTY_NOT_ALLOWED =
        "specifying an ID property is pointless when retrieval of auto-generated keys is suppressed";
  private static final String ID_KEY_NOT_ALLOWED =
        "specifying an ID key is pointless when retrieval of auto-generated keys is suppressed";
  private static final String KEY_RETRIEVAL_DISABLED =
        "retrieval of auto-generated keys is suppressed for this SQLInsert";

  private final List<String> idProperties = new ArrayList<>(5);

  private final boolean retrieveAutoKeys;

  SQLInsert(PreparedStatement ps,
        AbstractSQLSession sql,
        SQLInfo sqlInfo,
        boolean retrieveAutoKeys) {
    super(ps, sql, sqlInfo);
    this.retrieveAutoKeys = retrieveAutoKeys;
  }

  /**
   * Binds the values in the specified JavaBean to the parameters within the SQL
   * statement. Bean properties that do not correspond to named parameters will be
   * ignored. The effect of passing anything other than a proper JavaBean (e.g. an
   * {@code Integer}, {@code String} or array) is undefined.
   *
   * @param bean the bean whose values to bind to the named parameters within the SQL
   * statement
   * @return this {@code SQLInsert} instance
   */
  @Override
  public SQLInsert bind(Object bean) {
    super.bind(bean);
    idProperties.add(null);
    return this;
  }

  /**
   * <p>Binds the values in the specified JavaBean to the parameters within the SQL
   * statement. Bean properties that do not correspond to named parameters will be
   * ignored. The effect of passing anything other than a proper JavaBean (e.g. an
   * {@code Integer}, {@code String} or array) is undefined. The {@code idProperty}
   * argument must be the name of the property corresponding to the primary key. The
   * generated value for that column will be bound back into the bean. Therefore, make
   * sure the bean is modifiable.
   *
   * <p><b><i>Klojang JDBC</i> does not support table definitions that generate keys
   * for multiple columns.</b>
   *
   * @param bean the bean whose values to bind to the named parameters within the SQL
   * statement
   * @param idProperty the name of the property representing the auto-generated primary
   * key.
   * @return this {@code SQLInsert} instance
   */
  public SQLInsert bind(Object bean, String idProperty) {
    super.bind(bean);
    Check.that(retrieveAutoKeys).is(yes(), ID_PROPERTY_NOT_ALLOWED);
    Check.notNull(idProperty, " ID Property").then(idProperties::add);
    return this;
  }

  /**
   * Binds the values in the specified map to the parameters within the SQL statement.
   * Keys that do not correspond to named parameters will be ignored.
   *
   * @param map the map whose values to bind to the named parameters within the SQL
   * statement
   * @return this {@code SQLInsert} instance
   */
  @Override
  public SQLInsert bind(Map<String, Object> map) {
    super.bind(map);
    idProperties.add(null);
    return this;
  }

  /**
   * Binds the values in the specified map to the parameters within the SQL statement.
   * Keys that do not correspond to named parameters will be ignored. The {@code idKey}
   * argument must be the name of the map key corresponding to the table's primary key.
   * The generated value for that column will be bound back into the map. Therefore, make
   * sure the map is modifiable.
   *
   * <p><b><i>Klojang JDBC</i> does not support table definitions that generate keys
   * for multiple columns.</b>
   *
   * @param map the map whose values to bind to the named parameters within the SQL
   * statement
   * @param idKey the name of the map key representing the auto-generated primary key.
   * @return this {@code SQLInsert} instance
   */
  public SQLInsert bind(Map<String, ?> map, String idKey) {
    super.bind(map);
    Check.that(retrieveAutoKeys).is(yes(), ID_KEY_NOT_ALLOWED);
    Check.notNull(idKey, "ID Key").then(idProperties::add);
    return this;
  }

  /**
   * Executes the INSERT statement. If the {@code SQLInsert} was configured to
   * {@linkplain SQLSession#prepareInsert(boolean) retrieve auto-generated keys}, this
   * method returns the auto-generated key, else it returns -1. If the database generated
   * keys for multiple columns, a {@link KlojangSQLException} is thrown, as this is not
   * supported by <i>Klojang JDBC</i>.
   *
   * @return the key generated by the database or -1 if retrieval of auto-generated keys
   * was suppressed.
   */
  public long execute() {
    try {
      applyBindings(ps);
      ps.executeUpdate();
      return retrieveAutoKeys ? JDBC.getGeneratedKeys(ps, 1)[0] : -1;
    } catch (Throwable t) {
      throw KlojangSQLException.wrap(t, sqlInfo);
    } finally {
      reset();
    }
  }

  /**
   * Executes the INSERT statement. Any JavaBean that was bound using
   * {@link #bind(Object, String) bind(bean, idProperty)} will have its ID property set to
   * the key generated by the database. JavaBeans that were bound using
   * {@link #bind(Object) bind(bean)} will remain unmodified. The same applies <i>mutatis
   * mutandis</i> for {@code Map} objects. An {@link IllegalStateException} will be thrown
   * if the {@code SQLInsert} was configured to
   * {@linkplain SQLSession#prepareInsert(boolean) suppress the retrieval of
   * auto-generated keys}.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public long executeAndSetID() {
    Check.that(retrieveAutoKeys).is(yes(), illegalState(KEY_RETRIEVAL_DISABLED));
    try {
      applyBindings(ps);
      ps.executeUpdate();
      long dbKey = JDBC.getGeneratedKeys(ps, 1)[0];
      for (int i = 0; i < idProperties.size(); ++i) {
        String idProperty = idProperties.get(i);
        if (idProperty != null) {
          Object obj = bindings.get(i);
          if (obj instanceof Map map) {
            map.put(idProperty, dbKey);
          } else {
            JDBC.setID(obj, idProperty, dbKey);
          }
        }
      }
      return dbKey;
    } catch (Throwable t) {
      throw KlojangSQLException.wrap(t, sqlInfo);
    } finally {
      reset();
    }
  }

  /**
   * Saves the specified JavaBeans to the database. This method combines the binding and
   * execution phase. Therefore it must be called on a "fresh" instance. That is, it must
   * not contain any bound values, beans or maps yet &#8212; which would be the case if
   * the instance has just been created, or you have just executed the INSERT statement
   * (which resets the instance), or you have explicitly called {@link #reset()}.
   *
   * <p>For large batches consider using the {@link SQLBatchInsert} class as it is
   * likely to be more performant.
   *
   * @param beans the beans to save
   * @param <U> the type of the beans
   */
  public <U> void insertBatch(Collection<U> beans) {
    Check.that(bindings).is(empty(), illegalState(DIRTY_INSTANCE));
    try {
      for (U bean : beans) {
        bind(bean);
        applyBindings(ps);
        ps.executeUpdate();
        reset();
      }
    } catch (Throwable t) {
      throw KlojangSQLException.wrap(t, sqlInfo);
    }
  }

  /**
   * Saves the specified JavaBeans to the database and returns the keys generated by the
   * database. This method combines the binding and execution phase. Therefore it must be
   * called on a "fresh" instance. That is, it must not contain any bound values, beans or
   * maps yet &#8212;. This will be the case if the instance has just been created or if
   * you just executed the INSERT statement (which resets the instance) or if you just
   * called {@link #reset()}.
   *
   * <p>For large batches consider using the {@link SQLBatchInsert} class as it is
   * likely to be more performant.
   *
   * @param beans the beans to save
   * @param <U> the type of the beans
   * @return the keys generated by the database
   */
  public <U> long[] insertBatchAndGetIDs(Collection<U> beans) {
    Check.that(retrieveAutoKeys).is(yes(), illegalState(KEY_RETRIEVAL_DISABLED));
    Check.that(bindings).is(empty(), illegalState(DIRTY_INSTANCE));
    long[] keys = new long[beans.size()];
    try {
      int i = 0;
      for (U bean : beans) {
        bind(bean);
        applyBindings(ps);
        ps.executeUpdate();
        keys[i++] = JDBC.getGeneratedKeys(ps, 1)[0];
        reset();
      }
    } catch (Throwable t) {
      throw KlojangSQLException.wrap(t, sqlInfo);
    }
    return keys;
  }

  /**
   * Saves the specified JavaBeans to the database and sets the specified ID property in
   * each of them to the key generated by the database. This method combines the binding
   * and execution phase. Therefore it must be called on a "fresh" instance. That is, it
   * must not contain any bound values, beans or maps yet &#8212; which would be the case
   * if the instance has just been created, or you have just executed the INSERT statement
   * (which resets the instance), or you have explicitly called {@link #reset()}.
   *
   * @param <U> the type of the beans
   * @param idProperty the name of the property corresponding to the primary key
   * @param beans the beans to save
   */
  public <U> void insertBatchAndSetIDs(String idProperty, Collection<U> beans) {
    Check.that(retrieveAutoKeys).is(yes(), illegalState(KEY_RETRIEVAL_DISABLED));
    Check.that(bindings).is(empty(), illegalState(DIRTY_INSTANCE));
    try {
      for (U bean : beans) {
        bind(bean, idProperty).exec();
        long key = JDBC.getGeneratedKeys(ps, 1)[0];
        JDBC.setID(bean, idProperty, key);
        reset();
      }
    } catch (Throwable t) {
      throw KlojangSQLException.wrap(t, sqlInfo);
    }
  }

  @Override
  void initialize() {
    idProperties.clear();
    try {
      ps.clearParameters();
    } catch (SQLException e) {
      throw KlojangSQLException.wrap(e, sqlInfo);
    }
  }

  private void exec() throws Throwable {
    applyBindings(ps);
    ps.executeUpdate();
  }

}
