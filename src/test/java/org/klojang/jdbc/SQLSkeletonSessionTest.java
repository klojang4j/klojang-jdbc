package org.klojang.jdbc;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.klojang.invoke.BeanReader;
import org.klojang.invoke.BeanValueTransformer;
import org.klojang.util.IOMethods;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.klojang.invoke.IncludeExclude.EXCLUDE;

public class SQLSkeletonSessionTest {

  private static final String DB_DIR = System.getProperty("user.home") + "/klojang-db-template-session-test";
  private static final ThreadLocal<Connection> MY_CON = new ThreadLocal<>();

  public record Person(Integer id, String firstName, String lastName, int age) { }

  @BeforeEach
  public void before() throws IOException, SQLException {
    IOMethods.rm(DB_DIR);
    Files.createDirectories(Path.of(DB_DIR));
    Connection c = DriverManager.getConnection("jdbc:h2:" + DB_DIR + "/test");
    String sql = """
          CREATE LOCAL TEMPORARY TABLE PERSON(
            ID INT AUTO_INCREMENT, 
            FIRST_NAME VARCHAR(255),
            LAST_NAME VARCHAR(255),
            AGE INT)
          """;
    try (Statement stmt = c.createStatement()) {
      stmt.executeUpdate(sql);
    }
    MY_CON.set(c);
  }

  @AfterEach
  public void after() throws SQLException {
    if (MY_CON.get() != null) {
      MY_CON.get().close();
    }
    IOMethods.rm(DB_DIR);
  }

  @AfterAll
  public static void afterAll() {
    if (MY_CON.get() != null) {
      try {
        MY_CON.get().close();
        IOMethods.rm(DB_DIR);
      } catch (SQLException e) {
        // ...
      }
    }
  }

  @Test
  public void setValues00() throws Exception {
    List<Person> persons = List.of(
          new Person(null, "John", "Smith", 34),
          new Person(null, "Francis", "O'Donell", 27),
          new Person(null, "Mary", "Bear", 52));
    // ...
    SQL sql = SQL.skeleton("""
          INSERT INTO PERSON(FIRST_NAME,LAST_NAME,AGE) VALUES
          ~%%begin:record%
          (~%firstName%,~%lastName%,~%age%)
          ~%%end:record%
          """);
    BeanReader<Person> reader = new BeanReader<>(Person.class, EXCLUDE, "id");
    try (Connection conn = MY_CON.get()) {
      try (SQLSession session = sql.session(conn)) {
        session.setValues(reader, persons).execute();
      }
      int i = SQL.simpleQuery(MY_CON.get(), "SELECT COUNT(*) FROM PERSON")
            .getInt()
            .getAsInt();
      assertEquals(3, i);
    }
  }

  @Test
  public void setValues01() throws Exception {
    List<Person> persons = List.of(
          new Person(null, "John", "Smith", 34),
          new Person(null, "Francis", "O'Donell", 27),
          new Person(null, "Mary", "Bear", 52));
    // ...
    SQL sql = SQL.skeleton("""
          INSERT INTO PERSON(FIRST_NAME,LAST_NAME,AGE) VALUES
          ~%%begin:foo_bar%
          (~%firstName%,~%lastName%,~%age%)
          ~%%end:foo_bar%
          """);
    BeanReader<Person> reader = new BeanReader<>(Person.class);
    try (Connection conn = MY_CON.get()) {
      try (SQLSession session = sql.session(conn)) {
        assertThrows(KlojangSQLException.class,
              () -> session.setValues(reader, persons).execute());
      }
    }
  }

  @Test
  public void sqlFunction00() throws Exception {
    List<Person> persons = List.of(
          new Person(null, "John", "Smith", 34),
          new Person(null, "Francis", "O'Donell", 27),
          new Person(null, "Mary", "Bear", 52));
    // ...
    SQL sql = SQL.skeleton("""
          INSERT INTO PERSON(FIRST_NAME,LAST_NAME,AGE) VALUES
          ~%%begin:record%
          (~%firstName%,~%lastName%,~%age%)
          ~%%end:record%
          """);
    try (Connection con = MY_CON.get()) {
      try (SQLSession session = sql.session(con)) {
        BeanValueTransformer<Person> transformer = (bean, prop, val) -> {
          if (prop.equals("firstName")) {
            return session.sqlFunction("SUBSTRING", val, 1, 3);
          }
          return val;
        };
        BeanReader<Person> reader = new BeanReader<>(Person.class, transformer);
        session.setValues(reader, persons).execute();
        String query = "SELECT FIRST_NAME FROM PERSON";
        List<String> firstNames = SQL.simpleQuery(MY_CON.get(), query)
              .firstColumn(String.class);
        assertEquals(List.of("Joh", "Fra", "Mar"), firstNames);
      }
    }
  }


}
