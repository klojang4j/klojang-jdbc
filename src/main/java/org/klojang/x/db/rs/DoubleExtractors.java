package org.klojang.x.db.rs;

import org.klojang.convert.NumberMethods;

import static java.sql.Types.*;
import static org.klojang.x.db.rs.RsMethod.*;

/*
 * Extracts various types of column values from a ResultSet and converts them to double values.
 */
class DoubleExtractors extends ExtractorLookup<Double> {

  DoubleExtractors() {
    addMultiple(new RsExtractor<>(GET_DOUBLE), FLOAT, DOUBLE);
    // We don't really need to add the conversion functions (like  Integer::doubleValue)
    // because the compiler can figure this out by itself. But we like to be explicit.
    add(INTEGER, new RsExtractor<>(GET_INT, Integer::doubleValue));
    add(SMALLINT, new RsExtractor<>(GET_SHORT, Short::doubleValue));
    add(TINYINT, new RsExtractor<>(GET_BYTE, Byte::doubleValue));
    add(REAL, new RsExtractor<>(GET_FLOAT, Float::doubleValue));
    add(BIGINT, new RsExtractor<>(GET_LONG, Long::doubleValue));
    add(BOOLEAN, new RsExtractor<>(GET_BOOLEAN, x -> x ? 1.0 : 0));
    addMultiple(new RsExtractor<>(GET_BIG_DECIMAL, NumberMethods::convert),
        NUMERIC,
        DECIMAL);
    add(VARCHAR, new RsExtractor<>(GET_STRING, NumberMethods::parse));
  }

}
