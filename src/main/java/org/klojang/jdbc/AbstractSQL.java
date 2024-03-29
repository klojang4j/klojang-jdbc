package org.klojang.jdbc;

import org.klojang.jdbc.x.ps.BeanBinder;
import org.klojang.jdbc.x.ps.MapBinder;
import org.klojang.jdbc.x.sql.ParameterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

abstract sealed class AbstractSQL implements SQL
      permits SimpleSQL, SQLTemplate, SQLSkeleton {

  @SuppressWarnings({"unused"})
  private static final Logger LOG = LoggerFactory.getLogger(AbstractSQLSession.class);

  private final String unparsed;
  private final SessionConfig config;

  @SuppressWarnings("rawtypes")
  private final Map<Class, BeanBinder> binders;
  @SuppressWarnings("rawtypes")
  private final Map<Class, BeanExtractorFactory> factories;

  private MapExtractorFactory mapExtractorFactory;

  AbstractSQL(String sql, SessionConfig config) {
    this.unparsed = sql;
    this.config = config;
    // These maps are unlikely to grow beyond one or two entries (you can't extract
    // _that_ many beans from a single row).
    binders = new HashMap<>();
    factories = new HashMap<>();
  }

  // Returns the original, user-provided SQL string, with any named parameters and
  // template variables still present
  final String unparsed() {
    return unparsed;
  }

  final SessionConfig config() {
    return config;
  }

  @SuppressWarnings("unchecked")
  final <T> BeanBinder<T> getBeanBinder(ParameterInfo paramInfo, Class<T> clazz) {
    return binders.computeIfAbsent(clazz,
          k -> new BeanBinder<>(clazz, paramInfo.parameters(), config));
  }

  final MapBinder getMapBinder(ParameterInfo paramInfo) {
    return new MapBinder(paramInfo.parameters(), config);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  final <T> BeanExtractorFactory<T> getBeanExtractorFactory(Class<T> clazz) {
    return factories.computeIfAbsent(clazz, k -> new BeanExtractorFactory<>(k, config));
  }

  @SuppressWarnings("unchecked")
  final <T> BeanExtractorFactory<T> getBeanExtractorFactory(Class<T> clazz,
        Supplier<T> supplier) {
    return factories.computeIfAbsent(clazz,
          k -> new BeanExtractorFactory<>(clazz, supplier, config));
  }

  final MapExtractorFactory getMapExtractorFactory() {
    if (mapExtractorFactory == null) {
      mapExtractorFactory = new MapExtractorFactory(config);
    }
    return mapExtractorFactory;
  }

}
