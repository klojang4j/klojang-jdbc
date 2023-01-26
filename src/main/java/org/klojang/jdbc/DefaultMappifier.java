package org.klojang.jdbc;

import java.sql.ResultSet;
import java.util.*;

import org.klojang.check.Check;
import org.klojang.jdbc.x.rs.RowChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.klojang.check.CommonChecks.gt;
import static org.klojang.check.CommonChecks.no;
import static org.klojang.check.CommonExceptions.STATE;
import static org.klojang.check.CommonExceptions.illegalState;
import static org.klojang.jdbc.x.rs.RowChannel.toRow;

class DefaultMappifier implements ResultSetMappifier {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(DefaultMappifier.class);

  private class RowIterator implements Iterator<Row> {

    private DefaultMappifier dm;

    RowIterator(DefaultMappifier dm) {
      this.dm = dm;
    }

    @Override
    public boolean hasNext() {
      return !dm.empty;
    }

    @Override
    public Row next() {
      Check.on(STATE, dm.empty).is(no(), "No more rows in result set");
      return dm.mappify().get();
    }
  }

  private final ResultSet rs;
  private final RowChannel<?>[] channels;

  private boolean empty;

  DefaultMappifier(ResultSet rs, RowChannel<?>[] channels) {
    this.rs = rs;
    this.channels = channels;
  }

  @Override
  public Optional<Row> mappify() {
    if (empty) {
      return Optional.empty();
    }
    try {
      Optional<Row> row = Optional.of(toRow(rs, channels));
      empty = rs.next();
      return row;
    } catch (Throwable t) {
      throw KJSQLException.wrap(t, null);
    }
  }

  @Override
  public List<Row> mappify(int limit) {
    Check.that(limit, "limit").is(gt(), 0);
    if (empty) {
      return Collections.emptyList();
    }
    List<Row> all = new ArrayList<>(limit);
    int i = 0;
    try {
      do {
        all.add(toRow(rs, channels));
      } while (++i < limit && (empty = rs.next()));
    } catch (Throwable t) {
      throw KJSQLException.wrap(t, null);
    }
    return all;
  }

  @Override
  public List<Row> mappifyAll() {
    return mappifyAll(10);
  }

  @Override
  public List<Row> mappifyAll(int sizeEstimate) {
    Check.that(sizeEstimate, "sizeEstimate").is(gt(), 0);
    if (empty) {
      return Collections.emptyList();
    }
    List<Row> all = new ArrayList<>(sizeEstimate);
    try {
      do {
        all.add(toRow(rs, channels));
      } while (rs.next());
    } catch (Throwable t) {
      throw KJSQLException.wrap(t, null);
    }
    empty = true;
    return all;
  }

  @Override
  public boolean isEmpty() {
    return empty;
  }

  @Override
  public Iterator<Row> iterator() {
    return new RowIterator(this);
  }
}
