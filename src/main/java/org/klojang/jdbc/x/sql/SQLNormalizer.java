package org.klojang.jdbc.x.sql;

import org.klojang.check.Check;
import org.klojang.jdbc.KlojangSQLException;
import org.klojang.util.CollectionMethods;
import org.klojang.util.MutableInt;
import org.klojang.util.collection.IntArrayList;
import org.klojang.util.collection.IntList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.klojang.check.Check.fail;
import static org.klojang.check.CommonChecks.ne;
import static org.klojang.check.CommonProperties.strlen;
import static org.klojang.util.CollectionMethods.collectionToList;

/*
 * Extracts named parameters from a SQL query string and replaces them with positional
 * parameters (question marks).
 */
final class SQLNormalizer {

  private static final String ERR_ADJACENT_PARAMS =
      "adjacent parameters at positions ${0} and ${1} cannot yield valid SQL";
  private static final String ERR_EMPTY_NAME =
      "zero-length parameter name at position ${0}";

  private static final char QUOTE = '\'';
  private static final char COLON = ':';
  private static final char BACKSLASH = '\\';

  private final String normalized;
  private final Map<String, IntList> positions;
  private final List<NamedParameter> params;

  SQLNormalizer(String sql) {
    final StringBuilder normalized = new StringBuilder(sql.length());
    final Map<String, IntList> positions = new LinkedHashMap<>();
    final MutableInt counter = new MutableInt(); // parameter counter
    final StringBuilder name = new StringBuilder(); // parameter name
    int position = -1; // parameter start position
    boolean insideString = false;
    boolean escaped = false;
    for (int i = 0; i < sql.length(); ++i) {
      char c = sql.charAt(i);
      if (insideString) {
        normalized.append(c);
        if (c == QUOTE) {
          if (!escaped) {
            insideString = false;
          }
        } else if (c == BACKSLASH) {
          escaped = true;
        } else {
          escaped = false;
        }
      } else if (position != -1) { // we are assembling a parameter name
        if (isParamChar(c)) {
          name.append(c);
          if (i == sql.length() - 1) {
            addParam(name, position, positions, counter);
          }
        } else {
          addParam(name, position, positions, counter);
          normalized.append(c);
          position = -1;
          if (c == QUOTE) {
            insideString = true;
          } else if (c == COLON) {
            fail(KlojangSQLException::new, ERR_ADJACENT_PARAMS, position, i);
          }
        }
      } else if (c == COLON) {
        normalized.append('?');
        position = i;
        name.setLength(0);
      } else {
        normalized.append(c);
        if (c == QUOTE) {
          insideString = true;
        }
      }
    }
    this.normalized = normalized.toString();
    this.positions = CollectionMethods.freeze(positions, IntList::copyOf);
    this.params = collectionToList(positions.entrySet(), this::toNamedParam);
  }

  /*
   * Returns SQL in which all named parameters have been replaced with question marks
   * (i.e. standard JDBC positional parameters).
   */
  public String getNormalizedSQL() {
    return normalized;
  }

  public List<NamedParameter> getParameters() {
    return params;
  }

  public Map<String, IntList> getParameterPositions() {
    return positions;
  }

  private static boolean isParamChar(char c) {
    return Character.isLetterOrDigit(c) || c == '_';
  }

  private static void addParam(
      StringBuilder name,
      int position,
      Map<String, IntList> positions,
      MutableInt counter) {
    Check.on(KlojangSQLException::new, name)
        .has(strlen(), ne(), 0, ERR_EMPTY_NAME, position);
    positions
        .computeIfAbsent(name.toString(), k -> new IntArrayList())
        .add(counter.ppi());
  }

  private NamedParameter toNamedParam(Entry<String, IntList> e) {
    return new NamedParameter(e.getKey(), IntList.copyOf(e.getValue()));
  }

}
