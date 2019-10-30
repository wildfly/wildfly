package org.jboss.as.test.manualmode.jca.connectionlistener;

import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.manualmode.ejb.Util;
import org.jboss.jca.adapters.jdbc.spi.listener.ConnectionListener;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simple connection listener {@link ConnectionListener} test case.
 *
 * @author <a href="mailto:hsvabek@redhat.com">Hynek Svabek</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(value = {AbstractTestsuite.TestCaseSetup.class})
public class ConnectionListenerTestCase extends AbstractTestsuite {

    private static final Logger log = Logger.getLogger(ConnectionListenerTestCase.class);

    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;

    @Before
    public void init() throws Exception {
        this.context = Util.createNamingContext();
    }

    @After
    public void after() throws Exception {
        this.context.close();
    }

    /**
     * Test: insert record in transaction and then rollback
     *
     * @throws Exception
     */
    @Test
    public void testConnListenerWithRollback() throws Exception {
        testConnListenerTest(DEP_1, false);
    }

    /**
     * Test: insert record in transaction and then rollback
     *
     * @throws Exception
     */
    @Test
    public void testConnListenerXaWithRollback() throws Exception {
        testConnListenerTest(DEP_1_XA, true);
    }

    /**
     * Test: insert record in transaction and then rollback
     *
     * @throws Exception
     */
    @Test
    public void testConnListener2WithRollback() throws Exception {
        testConnListener2Test(DEP_2, false);
    }

    /**
     * Test: insert record in transaction and then rollback
     *
     * @throws Exception
     */
    @Test
    public void testConnListener2XaWithRollback() throws Exception {
        testConnListener2Test(DEP_2_XA, true);
    }

    /**
     * Test: insert record in transaction
     *
     * @throws Exception
     */
    @Test
    public void testConnListener3WithoutRollback() throws Exception {
        testConnListener3Test(DEP_3, false);
    }

    /**
     * Test: insert record in transaction
     *
     * @throws Exception
     */
    @Test
    public void testConnListener3XaWithoutRollback() throws Exception {
        testConnListener3Test(DEP_3_XA, true);
    }

    private void testConnListenerTest(String deployment, boolean useXaDatasource) throws NamingException, SQLException {
        try {
            if (!controller.isStarted(CONTAINER)) {
                controller.start(CONTAINER);
            }

            deployer.deploy(deployment);

            JpaTestSlsbRemote bean = lookup(JpaTestSlsbRemote.class, JpaTestSlsb.class, deployment);
            assertNotNull(bean);

            bean.initDataSource(useXaDatasource);

            bean.assertRecords(0);
            bean.insertRecord();
            bean.insertRecord();
            bean.insertRecord();
            bean.insertRecord();
            bean.insertRecord();

            bean.assertRecords(5);

            bean.insertRecord();
            bean.insertRecord(true);
            bean.insertRecord(true);
            bean.assertRecords(6);
            bean.assertRecords(6);

            /*
             * Activated count - Every new connection creates new activated record, rollback -> remove this record..
             * Passivated count - After connection.close() is invoked passivatedConnectionListener, rollback doesn't remove passivated record -> it is created after rollback
             */
            bean.assertExactCountOfRecords(6, 11, 12);
        } finally {
            close(deployment);
        }
    }

    private void testConnListener2Test(String deployment, boolean useXaDatasource) throws SQLException, NamingException {
        try {
            if (!controller.isStarted(CONTAINER)) {
                controller.start(CONTAINER);
            }

            deployer.deploy(deployment);

            JpaTestSlsbRemote bean = lookup(JpaTestSlsbRemote.class, JpaTestSlsb.class, deployment);
            assertNotNull(bean);

            bean.initDataSource(useXaDatasource);

            bean.insertRecord();
            bean.insertRecord();
            bean.insertRecord(true);
            /*
            * Activated count - we need open connection for select (in assertExactCountOfRecords...) 1 extra activated record is inserted... -> 3
            * Passivated count - rollback remove Activated record, but not passivated -> it is created after rollback
            */
            bean.assertExactCountOfRecords(2, 3, 3);

        } finally {
            close(deployment);
        }
    }

    private void testConnListener3Test(String deployment, boolean useXaDatasource) throws Exception {
        try {
            if (!controller.isStarted(CONTAINER)) {
                controller.start(CONTAINER);
            }

            deployer.deploy(deployment);

            JpaTestSlsbRemote bean = lookup(JpaTestSlsbRemote.class, JpaTestSlsb.class, deployment);
            assertNotNull(bean);

            bean.insertRecord();
            bean.insertRecord();
            bean.insertRecord();
            bean.insertRecord();
            bean.insertRecord();
            /*
             * Activated count - we need open connection for select (in assertExactCountOfRecords...) -> 1 extra activated record is inserted... -> 6
             */
            bean.assertExactCountOfRecords(5, 6, 5);

        } finally {
            close(deployment);
        }
    }

    private void close(String deploymentName) {
        try {
            if (!controller.isStarted(CONTAINER)) {
                controller.start(CONTAINER);
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        try {
            deployer.undeploy(deploymentName);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }

        try {
            if (controller.isStarted(CONTAINER)) {
                controller.stop(CONTAINER);
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }
}
