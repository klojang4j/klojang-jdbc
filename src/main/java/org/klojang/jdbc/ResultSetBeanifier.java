package org.klojang.jdbc;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

/**
 * <p>Converts the rows in a JDBC {@link ResultSet} into JavaBeans. Instances are
 * obtained via
 * {@link BeanifierFactory#getBeanifier(ResultSet) BeanifierFactory.getBeanifier()}.
 * {@code ResultSetBeanifier} is an extension of the {@link Iterable} interface, enabling
 * you to conveniently extract the JavaBeans in a {@code forEach} loop:
 *
 * <blockquote><pre>{@code
 * ResultSet rs = ...;
 * BeanifierFactory factory = new BeanifierFactory(Employee.class);
 * for(Employee emp : factory.getBeanifier(rs)) {
 *   // do stuff ...
 * }
 * }</pre></blockquote>
 *
 * <p>Contrary to the {@link SQLQuery} class, which serves a similar purpose, a
 * {@code ResultSetBeanifier} is agnostic about, and has no opinion on where you got the
 * {@link ResultSet} from. In that sense it is closer to the JDBC metal than the
 * {@code SQLQuery} class.
 *
 * @param <T> the type of the JavaBeans produced by the {@code ResultSetBeanifier}
 * @author Ayco Holleman
 */
public sealed interface ResultSetBeanifier<T> extends Iterable<T>
    permits DefaultBeanifier, EmptyBeanifier {

  /**
   * Converts the current row within the {@code ResultSet} into a JavaBean. If the
   * {@code ResultSet} is empty, or if there are no more rows in the {@code ResultSet}, an
   * empty {@code Optional} is returned. You can simply keep calling {@code beanify()} to
   * successively beanify all rows in the result set. <b>Do not call
   * {@link ResultSet#next()} yourself.</b>
   *
   * @return an {@code Optional} containing the JavaBean or an empty {@code Optional} if
   *     the {@code ResultSet} contained no (more) rows
   */
  Optional<T> beanify();

  /**
   * Converts at most {@code limit} rows from the {@code ResultSet} into JavaBeans. If the
   * {@code ResultSet} is empty, an empty {@code List} is returned.
   *
   * @param limit the maximum number of rows to extract and convert
   * @return a {@code List} of JavaBeans or an empty {@code List} if the {@code ResultSet}
   *     contained no (more) rows
   */
  List<T> beanify(int limit);

  /**
   * Converts all remaining rows in the {@code ResultSet} into JavaBeans.
   *
   * @return a {@code List} of JavaBeans or an empty {@code List} if the {@code ResultSet}
   *     contained no (more) rows
   */
  List<T> beanifyAll();

  /**
   * Converts all remaining rows n the specified {@code ResultSet} into JavaBeans.
   *
   * @param sizeEstimate an estimate of the size of the resulting {@code List}.
   * @return a {@code List} of JavaBeans or an empty {@code List} if the {@code ResultSet}
   *     contained no (more) rows
   */
  List<T> beanifyAll(int sizeEstimate);

  /**
   * Returns {@code true} if the end of the {@code ResultSet} has been reached;
   * {@code false} otherwise.
   *
   * @return whether the end of the {@code ResultSet} has been reached
   */
  boolean isEmpty();

}
