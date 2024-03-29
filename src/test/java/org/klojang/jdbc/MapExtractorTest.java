package org.klojang.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.klojang.util.IOMethods;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.klojang.jdbc.SQL.simpleQuery;
import static org.klojang.jdbc.SQL.staticSQL;

public class MapExtractorTest {

  private static final String DB_DIR = System.getProperty("user.home") + "/klojang-jdbc-tests/DefaultBeanExtractorTest";
  private static final ThreadLocal<Connection> MY_CON = new ThreadLocal<>();

  //@formatter:off
  public static class Employee {
    int empId;
    String empName;
    public int getEmpId() { return empId; }
    public void setEmpId(int personId) { this.empId = personId; }
    public String getEmpName() { return empName; }
    public void setEmpName(String personName) { this.empName = personName; }
  }

  public static class Department {
    int deptId;
    String deptName;
    public int getDeptId() { return deptId; }
    public void setDeptId(int deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
   }
  //@formatter:on

  @BeforeEach
  public void before() throws IOException, SQLException {
    IOMethods.rm(DB_DIR);
    Files.createDirectories(Path.of(DB_DIR));
    Connection con = DriverManager.getConnection("jdbc:h2:" + DB_DIR + "/h2");
    MY_CON.set(con);
    String sql = "CREATE TABLE EMPLOYEE(EMP_ID INT AUTO_INCREMENT, EMP_NAME VARCHAR(32))";
    staticSQL(sql).session(con).execute();
    sql = "CREATE TABLE DEPARTMENT(DEPT_ID INT AUTO_INCREMENT, DEPT_NAME VARCHAR(32))";
    staticSQL(sql).session(con).execute();
  }

  @Test
  public void test00() throws SQLException {
    Connection con = MY_CON.get();
    staticSQL("INSERT INTO EMPLOYEE(EMP_NAME)VALUES('Foo')").session(con).execute();
    staticSQL("INSERT INTO DEPARTMENT(DEPT_NAME)VALUES('Bar')").session(con).execute();
    ResultSet rs = simpleQuery(con, "SELECT EMP_ID AS ID, EMP_NAME AS NAME FROM EMPLOYEE").getResultSet();
    MapExtractorFactory factory = new MapExtractorFactory();
    MapExtractor sharedExtractor = factory.getExtractor(rs);
    List<Map<String, Object>> emps = sharedExtractor.extractAll();
    assertEquals("Foo", emps.get(0).get("name"));
    rs = simpleQuery(con, "SELECT DEPT_ID AS ID, DEPT_NAME AS NAME FROM DEPARTMENT").getResultSet();
    sharedExtractor = factory.getExtractor(rs);
    List<Map<String, Object>> depts = sharedExtractor.extractAll();
    assertEquals("Bar", depts.get(0).get("name"));
  }
}
