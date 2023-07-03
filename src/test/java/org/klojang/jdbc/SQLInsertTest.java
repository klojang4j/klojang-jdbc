package org.klojang.jdbc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.klojang.util.IOMethods;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//@Disabled
public class SQLInsertTest {

  private static final String DB_DIR = System.getProperty("user.home") + "/klojang-db-test";
  private static final ThreadLocal<Connection> MY_CON = new ThreadLocal<>();

  public static class Person {
    int id;
    String name;

    Person(String name) {
      this.name = name;
    }

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public SQLInsertTest() {}

  @BeforeEach
  public void before() throws IOException, SQLException {
    IOMethods.rm(DB_DIR);
    Files.createDirectories(Path.of(DB_DIR));
    Connection c = DriverManager.getConnection("jdbc:h2:" + DB_DIR + "/test");
    String sql = "CREATE LOCAL TEMPORARY TABLE TEST(ID INT AUTO_INCREMENT, NAME VARCHAR(255))";
    try (Statement stmt = c.createStatement()) {
      stmt.executeUpdate(sql);
    }
    MY_CON.set(c);
  }

  @AfterEach
  public void after() throws SQLException, IOException {
    if (MY_CON.get() != null) {
      MY_CON.get().close();
    }
    IOMethods.rm(DB_DIR);
  }

  /**
   * Baseline test that only uses JDBC and none of our own abstractions
   */
  @Test
  public void test00() throws SQLException {
    Connection con = MY_CON.get();
    String sql = "INSERT INTO TEST(NAME) VALUES(?)";
    try (PreparedStatement ps = con.prepareStatement(sql, RETURN_GENERATED_KEYS)) {
      ps.setString(1, "John");
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        assertEquals("ID", rs.getMetaData().getColumnLabel(1));
        assertTrue(rs.next());
        // Not really interested in the actual auto-generated value
      }
    }
  }

  @Test
  public void test01() {
    String s = "INSERT INTO TEST(NAME) VALUES(:name)";
    Map<String, Object> data = Collections.singletonMap("name", "John");
    SQLSession sql = SQL.basic(s).session();
    long id = Long.MIN_VALUE;
    try (SQLInsert insert = sql.prepareInsert(MY_CON.get())) {
      insert.bind(data);
      id = insert.executeAndGetID();
      assertTrue(id != Long.MIN_VALUE);
    }
  }

  @Test
  public void test02() {
    String s = "INSERT INTO TEST(NAME) VALUES(:name)";
    Person person = new Person("John");
    SQLSession sql = SQL.basic(s).session();
    long id = Long.MIN_VALUE;
    try (SQLInsert insert = sql.prepareInsert(MY_CON.get())) {
      insert.bind(person);
      id = insert.executeAndGetID();
      assertTrue(id != Long.MIN_VALUE);
    }
  }

  @Test
  public void test03() {
    String s = "INSERT INTO TEST(NAME) VALUES(:name)";
    Map<String, Object> data = new HashMap<>(Collections.singletonMap("name", "John"));
    SQLSession sql = SQL.basic(s).session();
    try (SQLInsert insert = sql.prepareInsert(MY_CON.get())) {
      insert.bind(data, "id");
      insert.execute();
      assertTrue(data.containsKey("id"));
    }
  }

  @Test
  public void test04() {
    String s = "INSERT INTO TEST(NAME) VALUES(:name)";
    Person person = new Person("John");
    person.setId(Integer.MIN_VALUE);
    SQLSession sql = SQL.basic(s).session();
    try (SQLInsert insert = sql.prepareInsert(MY_CON.get())) {
      insert.bind(person, "id");
      insert.execute();
      assertTrue(person.getId() != Integer.MIN_VALUE);
    }
  }

  @Test
  public void test05() {
    Person person = new Person("John");
    person.setId(Integer.MIN_VALUE);
    try (SQLInsert insert =
               SQLSession.prepareInsert().of(Person.class).into("TEST").excluding("id").prepare(
                     MY_CON.get())) {
      insert.bind(person, "id");
      insert.execute();
      assertTrue(person.getId() != Integer.MIN_VALUE);
    }
  }
}