package org.klojang.db;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

class EmptyMappifier implements ResultSetMappifier {

  static final EmptyMappifier INSTANCE = new EmptyMappifier();

  @Override
  public Optional<Row> mappify() {
    return Optional.empty();
  }

  @Override
  public List<Row> mappify(int limit) {
    return Collections.emptyList();
  }

  @Override
  public List<Row> mappifyAll() {
    return Collections.emptyList();
  }

  @Override
  public List<Row> mappifyAll(int sizeEstimate) {
    return Collections.emptyList();
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public Iterator<Row> iterator() {
    return Collections.emptyIterator();
  }
}
