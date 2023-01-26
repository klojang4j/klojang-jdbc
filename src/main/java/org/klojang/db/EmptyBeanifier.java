package org.klojang.db;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

class EmptyBeanifier<T> implements ResultSetBeanifier<T> {

  @SuppressWarnings("rawtypes")
  static final EmptyBeanifier INSTANCE = new EmptyBeanifier();

  private EmptyBeanifier() {}

  @Override
  public Optional<T> beanify() {
    return Optional.empty();
  }

  @Override
  public List<T> beanify(int limit) {
    return Collections.emptyList();
  }

  @Override
  public List<T> beanifyAll() {
    return Collections.emptyList();
  }

  @Override
  public List<T> beanifyAll(int sizeEstimate) {
    return Collections.emptyList();
  }

  @Override
  public boolean isEmpty() {
    return true;
  }

  @Override
  public Iterator<T> iterator() {
    return Collections.emptyIterator();
  }
}
