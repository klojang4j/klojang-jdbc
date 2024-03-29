package org.klojang.jdbc;

import org.klojang.check.Check;
import org.klojang.convert.NumberMethods;

import java.sql.ResultSet;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static org.klojang.jdbc.x.Strings.QUERY;

/**
 * <p>Facilitates the processing of large query results in batches across multiple,
 * isolated requests. These would typically be HTTP requests, but any stateless
 * request-response mechanism might want to use the functionality offered by the
 * {@code BatchQuery} class. The query will be kept alive until all records have been
 * extracted from it. Once all records have been extracted, the associated JDBC resources
 * will be closed automatically. As soon as, and as long as there are any persistent
 * queries, a background thread will periodically check whether any of them have gone
 * stale and, if so, close and remove them. Once they have all been either fully processed
 * or forcibly terminated, the thread will itself be terminated (until a new persistent
 * query comes into existence).
 *
 * <p>This class is aimed at avoiding the cost of "deep scrolling", whereby with
 * each request the query is re-executed and "windowed" using the LIMIT clause
 * ({@code LIMIT 0, 1000} ... {@code LIMIT 1000, 1000} ... {@code LIMIT 2000, 1000} ...
 * etc.). This tends to be very expensive. How expensive exactly depends on many factors,
 * but with large tables, using {@code BatchQuery} to avoid deep scrolling may increase
 * performance by one or two orders of magnitude. The larger the table, and the more
 * complex the query, the more you will benefit from using the {@code BatchQuery} class.
 * Typically, it does not make much sense to use the {@code BatchQuery} class for result
 * sets containing fewer than a million or so rows.
 *
 * <p>Since the point is to keep the query alive across multiple requests, you may
 * have to suppress a (good) habit of always setting up try-with-resources blocks for
 * {@link AutoCloseable} objects (the JDBC {@link java.sql.Connection Connection} and the
 * {@link SQLQuery} in this case). This would <i>not</i> be a good way to get hold of a
 * {@code BatchQuery}:
 *
 * <blockquote><pre>{@code
 * try(Connection con = ...) {
 *   SQLSession session = SQL.staticSQL("SELECT * FROM PERSON").session(con);
 *   try(SQLQuery query = session.prepareQuery()) {
 *     QueryId queryId = BatchQuery.pin(query);
 *     BatchQuery<Person> batchQuery = new BatchQuery<>(queryId, Person.class);
 *   }
 * }
 * }</pre></blockquote>
 *
 * <p>Instead, simply write:
 *
 * <blockquote><pre>{@code
 * Connection con = ...;
 * SQLSession session = SQL.staticSQL("SELECT * FROM PERSON").session(con);
 * SQLQuery query = session.prepareQuery();
 * QueryId queryId = BatchQuery.pin(query);
 * BatchQuery<Person> batchQuery = new BatchQuery<>(queryId, Person.class);
 * }</pre></blockquote>
 *
 * @param <T> the type of the JavaBeans or records produced by the
 *       {@code BatchQuery}
 */
public final class BatchQuery<T> {

  /**
   * Registers the specified {@code SQLQuery} for batch processing and returns a
   * {@code QueryId}. The {@code QueryId} can be used to instantiate a {@code BatchQuery}
   * object, allowing it the identify and wrap itself around the query result. Equivalent
   * to {@link #pin(SQLQuery, Duration) pin(query, Duration.ofMinutes(5), true)}. In other
   * words, the client gets five minutes to process a batch before it must request the
   * {@linkplain #nextBatch(int) next batch}. After that, the query will be deemed stale
   * and the associated JDBC resources will be closed. Subsequent calls to
   * {@code nextBatch()} will cause a {@link DatabaseException}.
   *
   * @param query the {@code SQLQuery} to be registered for batch processing
   * @return a {@code QueryId}
   */
  public static QueryId pin(SQLQuery query) {
    return pin(query, Duration.ofMinutes(5), true);
  }

  /**
   * Registers the specified {@code SQLQuery} for batch processing and returns a
   * {@code QueryId}. The {@code QueryId} can be used to instantiate a {@code BatchQuery}
   * object, allowing it the identify and wrap itself around the query result. Equivalent
   * to {@link #pin(SQLQuery, Duration, boolean) pin(query, stayAliveTime, true)}.
   *
   * @param query the {@code SQLQuery} to be registered for batch processing
   * @param stayAliveTime determines how long the query should be kept alive between
   *       requests for new batches. If the time interval between any two consecutive
   *       requests is longer than the specified duration, the query will be deemed stale
   *       and the associated JDBC resources will be closed
   * @return a {@code QueryId}
   */
  public static QueryId pin(SQLQuery query, Duration stayAliveTime) {
    return pin(query, stayAliveTime, true);
  }

  /**
   * Registers the specified {@code SQLQuery} for batch processing and returns a
   * {@code QueryId}. The {@code QueryId} can be used to instantiate a {@code BatchQuery}
   * object, allowing it the identify and wrap itself around a {@link ResultSet}.
   *
   * @param query the {@code SQLQuery} to be registered for batch processing
   * @param stayAliveTime determines how long the query should be kept alive between
   *       requests for new batches. If the time interval between any two consecutive
   *       requests is longer than the specified duration, the query will be deemed stale
   *       and the associated JDBC resources will be closed
   * @param closeConnection whether to close the JDBC connection once all records
   *       have been retrieved from the underlying {@link ResultSet} (which will anyhow be
   *       closed)
   * @return a {@code QueryId}
   */
  public static QueryId pin(SQLQuery query,
        Duration stayAliveTime,
        boolean closeConnection) {
    Check.notNull(query, QUERY);
    Check.notNull(stayAliveTime, "stayAliveTime");
    LiveQueryBroker broker = LiveQueryBroker.getInstance();
    return broker.register(query, stayAliveTime.getSeconds(), closeConnection);
  }

  /**
   * Terminates and unpins all {@link SQLQuery} objects. All {@code BatchQuery} objects
   * instantiated before calling this method will become unusable. Calls to
   * {@link #nextBatch(int) nextBatch()} will cause a {@link DatabaseException}.
   */
  public static void terminateAll() {
    LiveQueryBroker.getInstance().terminateAll();
  }


  private final QueryId queryId;
  private final ExtractorFactory<T> factory;

  /**
   * Instantiates a new {@code BatchQuery} object.
   *
   * @param queryId the ID of a pinned query
   * @param factory a factory for the {@link BeanExtractor} that will process the
   *       {@code ResultSet}
   */
  public BatchQuery(QueryId queryId, ExtractorFactory<T> factory) {
    this.queryId = queryId;
    this.factory = factory;
  }

  /**
   * Instantiates a new {@code BatchQuery} object. The {@code BatchQuery} will use the
   * same {@link SessionConfig} object as the {@link SQLQuery} identified by the specified
   * {@code QueryId}.
   *
   * @param queryId the ID of a pinned query
   * @param clazz the type of the JavaBeans or records produced by the
   *       {@code BatchQuery}
   */
  public BatchQuery(QueryId queryId, Class<T> clazz) {
    this.queryId = queryId;
    SQLQuery query = LiveQueryBroker.getInstance().getQuery(queryId);
    factory = query.getSession().getSQL().getBeanExtractorFactory(clazz);
  }

  /**
   * Instantiates a new {@code BatchQuery} object. The {@code BatchQuery} will use the
   * same {@link SessionConfig} object as the {@link SQLQuery} identified by the specified
   * {@code QueryId}.
   *
   * @param queryId the ID of a pinned query
   * @param clazz the type of the JavaBeans or records produced by the
   *       {@code BatchQuery}
   * @param beanSupplier the supplier of the JavaBeans
   */
  public BatchQuery(QueryId queryId, Class<T> clazz, Supplier<T> beanSupplier) {
    this.queryId = queryId;
    SQLQuery query = LiveQueryBroker.getInstance().getQuery(queryId);
    factory = query.getSession().getSQL().getBeanExtractorFactory(clazz, beanSupplier);
  }

  /**
   * Instantiates a new {@code BatchQuery} object that will produce lists of
   * {@code Map<String, Object>} pseudo-objects. This requires that {@code <T>} (the type
   * argument for the {@code BatchQuery} variable) really is
   * {@code <Map<String, Object>>}. Otherwise a {@link ClassCastException} will follow.
   * The {@code BatchQuery} will use the same {@link SessionConfig} object as the
   * {@link SQLQuery} identified by the specified {@code QueryId}.
   *
   * @param queryId the ID of a pinned query
   */
  @SuppressWarnings("unchecked")
  public BatchQuery(QueryId queryId) {
    this.queryId = queryId;
    SQLQuery query = LiveQueryBroker.getInstance().getQuery(queryId);
    factory = (ExtractorFactory<T>) query.getSession().getSQL().getMapExtractorFactory();
  }

  /**
   * Retrieves the next batch of records from the query result and converts them into
   * instances of type {@code <T>}. A {@link DatabaseException} is thrown if it has taken
   * too long for this method to be called since the previous time it was called (too long
   * as defined by the {@code stayAliveTime} argument passed to the
   * {@link #pin(SQLQuery, Duration, boolean) pin()} method).
   *
   * @param batchSize the number of records to retrieve
   * @return the next batch of records, converted into instances of type {@code <T>}
   */
  public List<T> nextBatch(int batchSize) {
    ResultSet rs = LiveQueryBroker.getInstance().getResultSet(queryId);
    BeanExtractor<T> extractor = factory.getExtractor(rs);
    List<T> beans = extractor.extract(batchSize);
    if (extractor.isEmpty()) {
      terminate();
    }
    return beans;
  }

  /**
   * Terminates and unpins the {@link SQLQuery} object associated with this
   * {@code BatchQuery}. Subsequent calls to {@link #nextBatch(int) nextBatch()} will
   * cause a {@link DatabaseException}. You do not need to call this method if you process
   * the query result all the way to the last row, but you might want to call it in
   * exceptional situations where you need to abort the query. Note that even if you do
   * not call this method, the {@link SQLQuery} will be terminated and unpinned, not too
   * long after requests for new batches have stopped coming in.
   */
  public void terminate() {
    LiveQueryBroker.getInstance().terminate(queryId);
  }

  /**
   * Functions as an identifier for a persistent query. A {@code QueryId} (or rather it
   * string representation) is meant to be ping-ponged back and forth between client and
   * server, for example via a URL query parameter and response header, respectively. On
   * the server side it is used to instantiate a {@code BatchQuery} object, allowing it to
   * identify and wrap itself around the query result.
   */
  public static final class QueryId {

    private final int id;

    private QueryId(int id) { this.id = id; }

    /**
     * Creates a {@code QueryId} from the specified string representation
     *
     * @param id the string representation of a {@code QueryId}
     * @return a {@code QueryId} from the specified string representation
     */
    public static QueryId of(String id) {
      Check.notNull(id);
      return new QueryId(NumberMethods.parseInt(id));
    }

    /**
     * Returns the hash code of this {@code QueryId}.
     *
     * @return the hash code of this {@code QueryId}
     */
    public int hashCode() { return id; }

    /**
     * Determines whether this {@code QueryId} equals the specified object.
     *
     * @param obj the object to compare this {@code QueryId} with
     * @return whether this {@code QueryId} equals the specified object
     */
    @Override
    public boolean equals(Object obj) {
      return this == obj || (obj instanceof QueryId qid && id == qid.id);
    }

    /**
     * Returns the string representation of this {@code QueryId}.
     *
     * @return the string representation of this {@code QueryId}
     */
    public String toString() { return String.valueOf(id); }
  }

}
