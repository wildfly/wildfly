/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.datasourcedefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.persistence.TransactionRequiredException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Transaction tests
 *
 * @author Scott Marlow and Zbynek Roubalik
 */
@ExtendWith(ArquillianExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DataSourceDefinitionJPATestCase {

    private static final String ARCHIVE_NAME = "jpa_datasourcedefinition";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(DataSourceDefinitionJPATestCase.class.getPackage());
        jar.addAsManifestResource(DataSourceDefinitionJPATestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        jar.addAsManifestResource(new StringAsset("Dependencies: com.h2database.h2\n"), "MANIFEST.MF");
        return jar;
    }

    @ArquillianResource
    private static InitialContext iniCtx;


    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }

    @Test
    @Order(1)
    public void testMultipleNonTXTransactionalEntityManagerInvocations() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.getEmployeeNoTX(1);   // For each call in, we will use a transactional entity manager
        // that isn't running in a transaction.  So, a new underlying
        // entity manager will be obtained.  The is to ensure that we don't blow up.
        sfsb1.getEmployeeNoTX(1);
        sfsb1.getEmployeeNoTX(1);
    }

    @Test
    @Order(2)
    public void testQueryNonTXTransactionalEntityManagerInvocations() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        String name = sfsb1.queryEmployeeNameNoTX(1);
        assertEquals("success", name, "Query should of thrown NoResultException, which we indicate by returning 'success'");
    }

    // Test that the queried Employee is detached as required by JPA 2.0 section 3.8.6
    // For a transaction scoped persistence context non jta-tx invocation, entities returned from Query
    // must be detached.
    @Test
    @Order(3)
    public void testQueryNonTXTransactionalDetach() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.createEmployee("Jill", "54 Country Lane", 2);
        Employee employee = sfsb1.queryEmployeeNoTX(2);
        assertNotNull(employee);

        boolean detached = sfsb1.isQueryEmployeeDetached(2);
        assertTrue(detached, "JPA 2.0 section 3.8.6 violated, query returned entity in non-tx that wasn't detached ");
    }

    /**
     * Ensure that calling entityManager.flush outside of a transaction, throws a TransactionRequiredException
     *
     * @throws Exception
     */
    @Test
    @Order(4)
    public void testTransactionRequiredException() throws Exception {
        Throwable error = null;
        try {
            SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
            sfsb1.createEmployeeNoTx("Sally", "1 home street", 1);
        } catch (TransactionRequiredException e) {
            error = e;
        } catch (Exception failed) {
            error = failed;
        }
        // jakarta.ejb.EJBException: jakarta.persistence.TransactionRequiredException: no transaction is in progress
        while (error != null && !(error instanceof TransactionRequiredException) && error.getCause() != null) {
            error = error.getCause();
        }
        assertTrue(
                error instanceof TransactionRequiredException, "attempting to persist entity with transactional entity manager and no transaction, should fail with a TransactionRequiredException"
                        + " but we instead got a " + error);
    }


    /**
     * Tests Jakarta Transactions involving an EJB 3 SLSB which makes two DAO calls in transaction.
     * Scenarios:
     * 1) The transaction fails during the first DAO call and the Jakarta Transactions transaction is rolled back and no database changes should occur.
     * 2) The transaction fails during the second DAO call and the Jakarta Transactions transaction is rolled back and no database changes should occur.
     * 3) The transaction fails after the DAO calls and the Jakarta Transactions transaction is rolled back and no database changes should occur.
     */
    @Test
    @Order(5)
    public void testFailInDAOCalls() throws Exception {
        SLSB1 slsb1 = lookup("SLSB1", SLSB1.class);
        slsb1.addEmployee();

        String message = slsb1.failInFirstCall();
        assertEquals("success", message, "DB should be unchanged, which we indicate by returning 'success'");

        message = slsb1.failInSecondCall();
        assertEquals("success", message, "DB should be unchanged, which we indicate by returning 'success'");

        message = slsb1.failAfterCalls();
        assertEquals("success", message, "DB should be unchanged, which we indicate by returning 'success'");
    }

    @Test
    @Order(6)
    public void testUserTxRollbackDiscardsChanges() throws Exception {
        SFSBXPC sfsbxpc = lookup("SFSBXPC", SFSBXPC.class);
        sfsbxpc.createEmployeeNoTx("Amory Lorch", "Lannister House", 10);  // create the employee but leave in xpc
        Employee employee = sfsbxpc.lookup(10);
        assertNotNull(employee, "could read employee record from extended persistence context (not yet saved to db)");

        // rollback any changes that haven't been saved yet
        sfsbxpc.forceRollbackAndLosePendingChanges(10, false);

        employee = sfsbxpc.lookup(10);
        assertNull(employee, "employee record should not be found in db after rollback");

    }

    @Test
    @Order(7)
    public void testEnlistXPCInUserTx() throws Exception {
        SFSBXPC sfsbxpc = lookup("SFSBXPC", SFSBXPC.class);
        sfsbxpc.createEmployeeNoTx("Amory Lorch", "Lannister House", 20);  // create the employee but leave in xpc
        Employee employee = sfsbxpc.lookup(20);
        assertNotNull(employee, "could read employee record from extended persistence context (not yet saved to db)");

        // start/end a user transaction without invoking the (extended) entity manager, which should cause the
        // pending changes to be saved to db
        sfsbxpc.savePendingChanges();

        sfsbxpc.forceRollbackAndLosePendingChanges(20, true);

        employee = sfsbxpc.lookup(20);
        assertNotNull(employee, "could read employee record from extended persistence context (wasn't saved to db during savePendingChanges())");

    }
}
