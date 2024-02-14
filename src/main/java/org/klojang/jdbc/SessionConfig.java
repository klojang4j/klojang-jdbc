package org.klojang.jdbc;

import org.klojang.jdbc.x.Utils;
import org.klojang.templates.NameMapper;
import org.klojang.templates.name.CamelCaseToSnakeUpperCase;
import org.klojang.templates.name.SnakeCaseToCamelCase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static org.klojang.templates.name.CamelCaseToSnakeUpperCase.camelCaseToSnakeUpperCase;
import static org.klojang.templates.name.SnakeCaseToCamelCase.snakeCaseToCamelCase;

/**
 * {@code SessionConfig} objects allow you to fine-tune or modify various aspects of how
 * <i>Klojang JDBC</i> processes and executes SQL. {@code SessionConfig} is an interface
 * that provides default implementations for all its methods. The default implementations
 * specify <i>Klojang JDBC's</i> default behaviour. You might want to implement
 * {@code SessionConfig} through an anonymous class:
 *
 * <blockquote><pre>{@code
 * SessionConfig config = new SessionConfig() {
 *   public boolean saveEnumAsString(Class<?> beanType, String enumProperty Class<?> enumType) {
 *     return true;
 *   }
 * };
 * }</pre></blockquote>
 *
 * <p>Alternatively, you could use the provided "withers" to modify how <i>Klojang
 * JDBC</i> operates:
 *
 * <blockquote><pre>{@code
 * SessionConfig config = SessionConfig.getDefaultConfig().withSaveAllEnumsAsStrings();
 * }</pre></blockquote>
 *
 * <p>It does pay, however, to store your final {@code SessionConfig} object in a
 * {@code public static final} field, and use it wherever appropriate. Otherwise, do
 * implement {@code SessionConfig} using a regular class and make sure to override the
 * {@code equals()} and {@code hashCode()} methods.
 *
 * @author Ayco Holleman
 */
public interface SessionConfig {

  /**
   * Returns a {@code SessionConfig} instance which does not override any of the defaults
   * provided by the {@code SessionConfig} instance.
   *
   * @return a {@code SessionConfig} instance which does not override any of the defaults
   *       provided by the {@code SessionConfig} instance
   */
  static SessionConfig getDefaultConfig() { return Utils.DEFAULT_CONFIG; }

  /**
   * A {@code CustomBinder} gives you full control over how values are bound to a
   * {@link PreparedStatement}. It hands you the underlying {@link PreparedStatement} and
   * lets you do the binding yourself. Of course, since you are now in control of the
   * {@code PreparedStatement}, you can do anything you like with it, including closing
   * it. <i>Klojang JDBC</i> will not be resistant against such behaviour. A
   * {@code CustomBinder} can be used, for example, to apply last-minute transformations
   * to the value that is about to be bound, or to serialize it in a bespoke way, or to
   * map it to a non-standard SQL datatype. When binding {@code Map} values using
   * {@link SQLStatement#bind(Map)}, custom binders will only kick in for non-{@code null}
   * values, because Java's type erase feature prevents the type of the values from being
   * established beforehand. When binding values in a JavaBean or {@code record}, custom
   * binders will kick in even for {@code null} values.
   */
  @FunctionalInterface
  interface CustomBinder {
    /**
     * Sets the value of the designated parameter using the given value.
     *
     * @param preparedStatement the {@code PreparedStatement} to bind the value to
     * @param paramIndex the first parameter is 1, the second is 2, ...
     * @param value the value to be bound
     * @throws SQLException if parameterIndex does not correspond to a parameter
     *       marker in the SQL statement, or if a database access error occurs
     */
    void bind(PreparedStatement preparedStatement, int paramIndex, Object value)
          throws SQLException;
  }

  /**
   * A {@code CustomReader} gives you full control over how values are extracted from a
   * {@link ResultSet}. It hands you the underlying {@code ResultSet} and lets you extract
   * values from it yourself. If the destination of the values is a JavaBean or
   * {@code record}, it is your responsibility to ensure that the value returned from
   * {@link #getValue(ResultSet, int)} can be assigned to the bean property or record
   * component for which it is destined, or a {@link ClassCastException} will ensue.
   */
  @FunctionalInterface
  interface CustomReader {
    /**
     * Retrieves the value of the designated column in the current row of the specified
     * {@link ResultSet}.
     *
     * @param resultSet the {@code ResultSet}
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value
     * @throws SQLException if the columnIndex is not valid or if a database access
     *       error occurs
     */
    Object getValue(ResultSet resultSet, int columnIndex) throws SQLException;
  }

  /**
   * Allows you to specify a {@code CustomBinder} for a given Java type. The default
   * implementation returns {@code null}, meaning you leave it to <i>Klojang JDBC</i> to
   * bind values to the underlying {@link PreparedStatement}. You may ignore any argument
   * that you don't need in order to determine whether to use a {@code CustomBinder}.
   *
   * @param beanType the type of the JavaBean, {@code record}, or {@code Map}
   *       containing the values for which to specify a {@code CustomBinder}
   * @param propertyName the name of the bean property, record component, or map key
   *       for which to specify a {@code CustomBinder}
   * @param propertyType the type of the values for which to specify a
   *       {@code CustomBinder}
   * @return a {@code CustomBinder} for any combination of the provided arguments, or
   *       {@code null} if you do not require a {@code CustomBinder} for any combination
   *       of the provided arguments
   */
  default CustomBinder getCustomBinder(Class<?> beanType,
        String propertyName,
        Class<?> propertyType) {
    return null;
  }

  /**
   * Allows you to specify a {@code CustomReader} for a given Java type and/or SQL
   * datatype. The default implementation returns {@code null}, meaning you leave it to
   * <i>Klojang JDBC</i> to extract values to the underlying {@link ResultSet}. You may
   * ignore any argument that you don't need in order to determine whether to use a
   * {@code CustomReader}.
   *
   * @param beanType the type of the JavaBean, {@code record}, or {@code Map} that
   *       will receive the value from the {@code ResultSet}. Note that when writing to a
   *       {@code Map} (using a {@link MapExtractor}), this argument will always be
   *       {@code HashMap.class} because that happens to be the type of {@code Map} that a
   *       {@link MapExtractor} creates.
   * @param propertyName the name of the bean property, record component, or map key
   *       that will receive the value from the {@code ResultSet}
   * @param propertyType the type of the values for which to specify a
   *       {@code CustomReader}. Note that when writing to a {@code Map} (using a
   *       {@link MapExtractor}), this argument will always be {@code Object.class}
   *       because we don't know the runtime type yet of the values when the
   *       {@code MapExtractor} is configured, and Java's type erase feature prevents us
   *       from being any more specific beforehand.
   * @param sqlType the SQL datatype of the column for which to specify a
   *       {@code CustomReader}. Must be one of the constants of the
   *       {@link java.sql.Types java.sql.Types}, like
   *       {@link java.sql.Types#VARCHAR Types.VARCHAR} or
   *       {@link java.sql.Types#TIMESTAMP Types.TIMESTAMP}}
   * @return a {@code CustomReader} for any combination of the provided arguments, or
   *       {@code null} if you do not require a {@code CustomReader} for any combination
   *       of the provided arguments
   */
  default CustomReader getCustomReader(Class<?> beanType,
        String propertyName,
        Class<?> propertyType,
        int sqlType) {
    return null;
  }

  /**
   * Specifies the storage type (the SQL datatype) for a value. This method will only be
   * evaluated if {@link #getCustomBinder(Class, String, Class) getCustomBinder()}
   * returned {@code null}. The return value must either be one of the constants in the
   * {@link java.sql.Types java.sql.Types} class (like
   * {@link java.sql.Types#VARCHAR Types.VARCHAR}) or {@code null}. Returning {@code null}
   * means you leave it to <i>Klojang JDBC</i> to figure out the SQL datatype. The default
   * implementation returns {@code null}. You may ignore any argument that you don't need
   * in order to determine the SQL datatype. For example, in many cases the type of the
   * value (provided via the {@code propertyType} argument) is all you need to know in
   * order to determine the corresponding SQL datatype.
   *
   * @param beanType the type of the JavaBean, {@code record}, or {@code Map}
   *       containing the value
   * @param propertyName the name of the bean property, record component, or map key
   *       for which to specify the SQL datatype.
   * @param propertyType the type of the value whose SQL datatype to determine
   * @return one of the class constants of the {@link java.sql.Types java.sql.Types} class
   *       or {@code null}
   */
  default Integer getSqlType(Class<?> beanType,
        String propertyName,
        Class<?> propertyType) {
    return null;
  }

  /**
   * Whether to save enums as strings (by calling their {@code toString()} method) or as
   * ints (by calling their {@code ordinal()} method). This method will only be evaluated
   * if {@link #getSqlType(Class, String, Class) getSqlType()} returned {@code null}. The
   * default implementation returns {@code false}, meaning that by default <i>Klojang
   * JDBC</i> will save enums as ints. More precisely: <i>Klojang JDBC</i> will bind
   * {@code enum} types using {@code preparedStatement.setInt(myEnum.ordinal())}. The
   * target column could still be a VARCHAR column. Whichever option you choose, the
   * reverse process &#8212; converting {@code ResultSet} values to enums &#8212; will
   * always work correctly. It will never require extra configuration. You may ignore any
   * argument that you don't need in order to determine the storage type for enums. To
   * save <i>all</i> enums in your application as strings, ignore all arguments and simply
   * return {@code true} straight away.
   *
   * @param beanType the type of the JavaBean, {@code record}, or {@code Map}
   *       containing the value
   * @param enumProperty the name of the bean property, record component, or map key
   *       for which to specify the SQL datatype.
   * @param enumType the type of the {@code enum} value whose SQL datatype to
   *       determine
   * @return whether to bind enums as strings ({@code true}) or as ints ({@code false})
   */
  default boolean saveEnumAsString(Class<?> beanType,
        String enumProperty,
        Class<? extends Enum<?>> enumType) {
    return false;
  }

  /**
   * Specifies the {@link NameMapper} to be used for mapping bean properties, record
   * components, or map keys to column names. The default implementation returns an
   * instance of {@link CamelCaseToSnakeUpperCase}, which would map
   * {@code "camelCaseToSnakeUpperCase"} to {@code "CAMEL_CASE_TO_SNAKE_UPPER_CASE"}. (It
   * would also map {@code "WordCase"} a.k.a. {@code "PascalCase"} to {@code "WORD_CASE"}
   * and {@code "PASCAL_CASE"}, respectively, since all characters end up in upper case
   * anyway.)
   *
   * @return the {@link NameMapper} to be used for mapping bean properties, record
   *       components, or map keys to column names
   */
  default NameMapper getPropertyToColumnMapper() {
    return camelCaseToSnakeUpperCase();
  }

  /**
   * Specifies the {@link NameMapper} to be used for mapping column names to bean
   * properties, record components, or map keys. The default implementation returns an
   * instance of {@link SnakeCaseToCamelCase}, which would map
   * {@code "SNAKE_CASE_TO_CAMEL_CASE}" to {@code "snakeCaseToCamelCase"}. (It would also
   * map {@code "snake_case_to_camel_case"} to {@code "SNAKE_CASE_TO_CAMEL_CASE}" because
   * the casing of the input string is irrelevant for this {@code NameMapper}.)
   *
   * @return the {@link NameMapper} to be used for mapping column names to bean
   *       properties, record components, or map keys
   */
  default NameMapper getColumnToPropertyMapper() {
    return snakeCaseToCamelCase();
  }

  /**
   * Returns a new instance that is equal to this instance except with the
   * property-to-column mapper set to the specified {@code NameMapper}.
   *
   * @param mapper the {@code NameMapper} to be used for mapping bean properties,
   *       record components, or map keys to column names
   * @return a new instance that is equal to this instance except with the
   *       property-to-column mapper set to the specified {@code NameMapper}.
   */
  default SessionConfig withPropertyToColumnMapper(NameMapper mapper) {
    return new SessionConfig() {
      public NameMapper getPropertyToColumnMapper() { return mapper; }
    };
  }

  /**
   * Returns a new instance that is equal to this instance except with the
   * column-to-property mapper set to the specified {@code NameMapper}.
   *
   * @param mapper the {@code NameMapper} to be used for mapping column names to
   *       bean properties, record components, or map keys
   * @return a new instance that is equal to this instance except with the
   *       column-to-property mapper set to the specified {@code NameMapper}.
   */
  default SessionConfig withColumnToPropertyMapper(NameMapper mapper) {
    return new SessionConfig() {
      public NameMapper getColumnToPropertyMapper() { return mapper; }
    };
  }

  /**
   * Returns a new instance that is equal to this instance except that <i>all</i> enums
   * will be persisted by calling {@code toString()} on them.
   *
   * @return a new instance that is equal to this instance except that <i>all</i> enums
   *       will be persisted by calling {@code toString()} on them.
   */
  default SessionConfig withSaveAllEnumsAsStrings() {
    return new SessionConfig() {
      public boolean saveEnumAsString(Class<?> beanType,
            String enumProperty,
            Class<? extends Enum<?>> enumType) {
        return true;
      }
    };
  }

}
