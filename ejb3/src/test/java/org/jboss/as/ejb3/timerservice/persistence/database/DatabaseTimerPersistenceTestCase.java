package org.jboss.as.ejb3.timerservice.persistence.database;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DatabaseTimerPersistenceTestCase {

    private DatabaseTimerPersistence object = new DatabaseTimerPersistence("", "part", "nodeA", 1000000, true);
    private Field field;
    private Method method;


    @Before
    public void initializeVars() throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException {
        field = object.getClass().getDeclaredField("database");
        field.setAccessible(true);
        method = object.getClass().getDeclaredMethod("investigateDialect");
        method.setAccessible(true);

        final Field sqlField = object.getClass().getDeclaredField("sql");
        sqlField.setAccessible(true);
        final Properties testSqlProperties = new Properties();
        testSqlProperties.setProperty("create-auto-timer", "insert...");
        sqlField.set(object, testSqlProperties);
    }

    @Test
    public void databaseValueTest() throws IllegalAccessException, InvocationTargetException {

        // test mysql
        field.set(object, "mysqlTest");
        method.invoke(object);
        Assert.assertEquals("mysql", field.get(object));

        field.set(object, "custom-mysql-driver");
        method.invoke(object);
        Assert.assertEquals("mysql", field.get(object));

        field.set(object, "testMysql");
        method.invoke(object);
        Assert.assertEquals("mysql", field.get(object));

        // test mariadb
        field.set(object, "mariadbTest");
        method.invoke(object);
        Assert.assertEquals("mariadb", field.get(object));

        field.set(object, "custom-mariadb-driver");
        method.invoke(object);
        Assert.assertEquals("mariadb",field.get(object));

        field.set(object, "testMariadb");
        method.invoke(object);
        Assert.assertEquals("mariadb", field.get(object));

        // test postgres
        field.set(object, "postgresTest");
        method.invoke(object);
        Assert.assertEquals("postgresql", field.get(object));

        field.set(object, "custom-postgres-driver");
        method.invoke(object);
        Assert.assertEquals("postgresql", field.get(object));

        field.set(object, "testPostgres");
        method.invoke(object);
        Assert.assertEquals("postgresql", field.get(object));

        // test db2
        field.set(object, "db2Test");
        method.invoke(object);
        Assert.assertEquals("db2", field.get(object));

        field.set(object, "custom-db2-driver");
        method.invoke(object);
        Assert.assertEquals("db2", field.get(object));

        field.set(object, "testDb2");
        method.invoke(object);
        Assert.assertEquals("db2", field.get(object));

        // test hsql
        field.set(object, "hsqlTest");
        method.invoke(object);
        Assert.assertEquals("hsql", field.get(object));

        field.set(object, "custom-hsql-driver");
        method.invoke(object);
        Assert.assertEquals("hsql", field.get(object));

        field.set(object, "testHsql");
        method.invoke(object);
        Assert.assertEquals("hsql", field.get(object));

        // test hsql
        field.set(object, "h2Test");
        method.invoke(object);
        Assert.assertEquals("h2", field.get(object));

        field.set(object, "custom-h2-driver");
        method.invoke(object);
        Assert.assertEquals("h2", field.get(object));

        field.set(object, "testH2");
        method.invoke(object);
        Assert.assertEquals("h2", field.get(object));

        // test oracle
        field.set(object, "oracleTest");
        method.invoke(object);
        Assert.assertEquals("oracle", field.get(object));

        field.set(object, "custom-oracle-driver");
        method.invoke(object);
        Assert.assertEquals("oracle", field.get(object));

        field.set(object, "testOracle");
        method.invoke(object);
        Assert.assertEquals("oracle", field.get(object));

        // test mssql
        field.set(object, "microsoftTest");
        method.invoke(object);
        Assert.assertEquals("mssql", field.get(object));

        field.set(object, "custom-microsoft-driver");
        method.invoke(object);
        Assert.assertEquals("mssql", field.get(object));

        field.set(object, "testMicrosoft");
        method.invoke(object);
        Assert.assertEquals("mssql", field.get(object));

        field.set(object, "mssql");
        method.invoke(object);
        Assert.assertEquals("mssql", field.get(object));

        field.set(object, "MSSQL");
        method.invoke(object);
        Assert.assertEquals("mssql", field.get(object));

        field.set(object, "mssql-version-x");
        method.invoke(object);
        Assert.assertEquals("mssql", field.get(object));

        // test sybase
        field.set(object, "jconnectTest");
        method.invoke(object);
        Assert.assertEquals("sybase", field.get(object));

        field.set(object, "custom-jconnect-driver");
        method.invoke(object);
        Assert.assertEquals("sybase", field.get(object));

        field.set(object, "testJconnect");
        method.invoke(object);
        Assert.assertEquals("sybase", field.get(object));
    }

}