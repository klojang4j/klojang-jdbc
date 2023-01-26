package org.klojang.jdbc;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.klojang.jdbc.NamedParameter;
import org.klojang.jdbc.SQL;
import org.klojang.util.collection.IntList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NamedParameterTest {

  @Test
  public void test00() {
    String s = "SELECT FOO FROM BAR WHERE FULL_NAME = :fullName";
    SQL sql = SQL.create(s);
    List<NamedParameter> params = sql.getParameters();
    assertEquals(1, params.size());
    assertEquals("fullName", params.get(0).getName());
    assertEquals(1, params.get(0).getIndices().size());
    assertEquals(1, params.get(0).getIndices().get(0));
    assertEquals("SELECT FOO FROM BAR WHERE FULL_NAME = ?", sql.getNormalizedSQL());
  }

  @Test
  public void test01() {
    String s =
        "SELECT FOO FROM BAR WHERE FULL_NAME = :name "
            + "AND LAST_NAME = :lastName OR LAST_NAME = :name "
            + "LIMIT :from,:to";
    SQL sql = SQL.create(s);
    Map<String, IntList> paramMap = sql.getParameterMap();
    assertEquals(4, paramMap.size());
    assertEquals(IntList.of(1, 3), paramMap.get("name"));
    assertEquals(IntList.of(2), paramMap.get("lastName"));
    assertEquals(IntList.of(4), paramMap.get("from"));
    assertEquals(IntList.of(5), paramMap.get("to"));
    List<NamedParameter> params = sql.getParameters();
    assertEquals(4, params.size());
    assertEquals("name", params.get(0).getName());
    assertEquals("lastName", params.get(1).getName());
    assertEquals("from", params.get(2).getName());
    assertEquals("to", params.get(3).getName());
    assertEquals(IntList.of(1, 3), params.get(0).getIndices());
  }
}
