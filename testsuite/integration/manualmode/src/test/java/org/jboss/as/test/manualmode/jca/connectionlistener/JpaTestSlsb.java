package org.jboss.as.test.manualmode.jca.connectionlistener;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.jboss.logging.Logger;
import org.junit.Assert;


/**
 * @author <a href="mailto:hsvabek@redhat.com">Hynek Svabek</a>
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class JpaTestSlsb implements JpaTestSlsbRemote {

    private static final Logger log = Logger.getLogger(JpaTestSlsb.class);

    @Resource(mappedName = "java:jboss/datasources/StatDS")
    private DataSource ds;
    @Resource(mappedName = "java:jboss/datasources/StatXaDS")
    private DataSource xads;


    private DataSource dataSource;

    @Inject
    UserTransaction userTransaction;

    @PostConstruct
    public void postConstruct() {
        dataSource = ds;
    }

    public void initDataSource(boolean useXaDs) {
        if (useXaDs) {
            dataSource = xads;
        } else {
            dataSource = ds;
        }
    }

    public void insertRecord() throws SQLException {
        insertRecord(false);
    }

    public void insertRecord(boolean doRollback) throws SQLException {
        Connection conn = null;
        Statement statement = null;
        try {
            userTransaction.begin();

            Assert.assertNotNull(dataSource);
            conn = dataSource.getConnection();

            statement = conn.createStatement();
            statement.executeUpdate("INSERT INTO test_table(description, type) VALUES ('add in bean', '2')");

            if (doRollback) {
                throw new IllegalStateException("Rollback!");
            }

            userTransaction.commit();
        } catch (Exception e) {
            try {
                userTransaction.rollback();
            } catch (IllegalStateException | SecurityException | SystemException e1) {
                log.warn(e1.getMessage());
            }
        } finally {
            closeStatement(statement);
            closeConnection(conn);
        }
    }


    public void assertRecords(int expectedRecords) throws SQLException {
        Connection conn = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            conn = dataSource.getConnection();


            statement = conn.createStatement();
            resultSet = statement.executeQuery("SELECT COUNT(*) FROM test_table WHERE type = '1'");
            resultSet.next();
            int activatedCount = resultSet.getInt(1);
            log.trace("Activated count: " + activatedCount);
            closeResultSet(resultSet);
            closeStatement(statement);


            //Even if we only read from DB, activated listener insert record to DB.
            Assert.assertTrue("Count of records created activatedConnectionListener must be > our created records", activatedCount > expectedRecords);


            statement = conn.createStatement();
            resultSet = statement.executeQuery("SELECT COUNT(*) FROM test_table WHERE type = '0'");
            resultSet.next();
            int passivatedCount = resultSet.getInt(1);
            closeResultSet(resultSet);
            closeStatement(statement);
            log.trace("Passivated count: " + passivatedCount);

            if (expectedRecords > 0) {
                Assert.assertTrue("Count of records created passivatedConnectionListener must be > 0", passivatedCount > 0);
            } else {
                Assert.assertTrue("Count of records created activatedConnectionListener must be >= 0", passivatedCount == 0);
            }

            statement = conn.createStatement();
            resultSet = statement.executeQuery("SELECT COUNT(*) FROM test_table WHERE type = '2'");
            resultSet.next();
            int ourRecordsCount = resultSet.getInt(1);
            closeResultSet(resultSet);
            closeStatement(statement);
            log.trace("Records count: " + ourRecordsCount);

            Assert.assertTrue("Unexpected count of records.", expectedRecords == ourRecordsCount);
        } finally {
            closeResultSet(resultSet);
            closeStatement(statement);
            closeConnection(conn);
        }
    }

    public void assertExactCountOfRecords(int expectedRecords, int expectedActivated, int expectedPassivated) throws SQLException {
        Connection conn = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            conn = dataSource.getConnection();

            statement = conn.createStatement();
            resultSet = statement.executeQuery("SELECT COUNT(*) FROM test_table WHERE type = '1'");
            resultSet.next();
            int activatedCount = resultSet.getInt(1);
            closeResultSet(resultSet);
            closeStatement(statement);
            log.trace("Activated count: " + activatedCount);

            //Even if we only read from DB, activated listener insert record to DB.
            Assert.assertEquals("Activated count: ", expectedActivated, activatedCount);

            statement = conn.createStatement();
            resultSet = statement.executeQuery("SELECT COUNT(*) FROM test_table WHERE type = '0'");
            resultSet.next();
            int passivatedCount = resultSet.getInt(1);
            closeResultSet(resultSet);
            closeStatement(statement);
            log.trace("Passivated count: " + passivatedCount);

            Assert.assertEquals("Passivated count: ", expectedPassivated, passivatedCount);

            statement = conn.createStatement();
            resultSet = statement.executeQuery("SELECT COUNT(*) FROM test_table WHERE type = '2'");
            resultSet.next();
            int ourRecordsCount = resultSet.getInt(1);
            closeResultSet(resultSet);
            closeStatement(statement);
            log.trace("Records count: " + ourRecordsCount);

            Assert.assertEquals("Records count: ", expectedRecords, ourRecordsCount);
        } finally {
            closeResultSet(resultSet);
            closeStatement(statement);
            closeConnection(conn);
        }
    }

    private void closeResultSet(ResultSet resultSet) {
        try {
            if (resultSet != null && !resultSet.isClosed()) {
                resultSet.close();
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    private void closeConnection(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    private void closeStatement(Statement statement) {
        try {
            if (statement != null && !statement.isClosed()) {
                statement.close();
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }
}
