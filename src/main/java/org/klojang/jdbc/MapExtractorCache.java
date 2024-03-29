package org.klojang.jdbc;

import org.klojang.jdbc.x.rs.KeyWriter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.klojang.jdbc.x.rs.KeyWriter.createWriters;

/**
 * Not currently used, because in the end we thought it would make the extractor retrieval
 * mechanism too expensive. We keep it around though, because it is both more elegant and
 * more idiot-proof than what we currently have. Maybe we change our minds.
 */
@SuppressWarnings({"unused"})
final class MapExtractorCache {

  private record MapExtractorId(SessionConfig config, ResultSetId resultSetId) { }

  private final Map<MapExtractorId, KeyWriter<?>[]> cache = new HashMap<>();

  MapExtractor getExtractor(SessionConfig config, ResultSet rs)
        throws SQLException {
    ResultSetId resultSetId = new ResultSetId(rs);
    MapExtractorId extractorId = new MapExtractorId(config, resultSetId);
    KeyWriter<?>[] writers = cache.get(extractorId);
    if (writers == null) {
      writers = createWriters(rs, config);
      cache.put(extractorId, writers);
    }
    return new DefaultMapExtractor(rs, writers);
  }


}
