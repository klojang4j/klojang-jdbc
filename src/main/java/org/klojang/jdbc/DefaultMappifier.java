package org.klojang.jdbc;

import org.klojang.check.Check;
import org.klojang.jdbc.x.rs.KeyWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.*;

import static org.klojang.check.CommonChecks.gt;
import static org.klojang.check.CommonChecks.no;
import static org.klojang.check.CommonExceptions.STATE;
import static org.klojang.jdbc.x.rs.KeyWriter.toMap;

final class DefaultMappifier implements ResultSetMappifier {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(DefaultMappifier.class);

  private static class MapIterator implements Iterator<Map<String, Object>> {

    private final DefaultMappifier dm;

    MapIterator(DefaultMappifier dm) {
      this.dm = dm;
    }

    @Override
    public boolean hasNext() {
      return !dm.empty;
    }

    @Override
    public Map<String, Object> next() {
      Check.on(STATE, dm.empty).is(no(), "no more rows in result set");
      return dm.mappify().get();
    }
  }

  private final ResultSet rs;
  private final KeyWriter<?>[] writers;

  private boolean empty;

  DefaultMappifier(ResultSet rs, KeyWriter<?>[] writers) {
    this.rs = rs;
    this.writers = writers;
  }

  @Override
  public Optional<Map<String, Object>> mappify() {
    if (empty) {
      return Optional.empty();
    }
    try {
      Optional<Map<String, Object>> row = Optional.of(toMap(rs, writers));
      empty = !rs.next();
      return row;
    } catch (Throwable t) {
      throw new KlojangSQLException(t);
    }
  }

  @Override
  public List<Map<String, Object>> mappify(int limit) {
    Check.that(limit, "limit").is(gt(), 0);
    if (empty) {
      return Collections.emptyList();
    }
    List<Map<String, Object>> all = new ArrayList<>(limit);
    int i = 0;
    try {
      do {
        all.add(toMap(rs, writers));
      } while (++i < limit && (empty = !rs.next()));
    } catch (Throwable t) {
      throw new KlojangSQLException(t);
    }
    return all;
  }

  @Override
  public List<Map<String, Object>> mappifyAll() {
    return mappifyAll(10);
  }

  @Override
  public List<Map<String, Object>> mappifyAll(int sizeEstimate) {
    Check.that(sizeEstimate, "sizeEstimate").is(gt(), 0);
    if (empty) {
      return Collections.emptyList();
    }
    List<Map<String, Object>> all = new ArrayList<>(sizeEstimate);
    try {
      do {
        all.add(toMap(rs, writers));
      } while (rs.next());
    } catch (Throwable t) {
      throw new KlojangSQLException(t);
    }
    empty = true;
    return all;
  }

  @Override
  public boolean isEmpty() {
    return empty;
  }

  @Override
  public Iterator<Map<String, Object>> iterator() {
    return new MapIterator(this);
  }
}
