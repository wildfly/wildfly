/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.jpa.datasourcedefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.TransactionRequiredException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Transaction tests
 *
 * @author Scott Marlow and Zbynek Roubalik
 */
@RunWith(Arquillian.class)
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
    @InSequence(1)
    public void testMultipleNonTXTransactionalEntityManagerInvocations() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.getEmployeeNoTX(1);   // For each call in, we will use a transactional entity manager
        // that isn't running in a transaction.  So, a new underlying
        // entity manager will be obtained.  The is to ensure that we don't blow up.
        sfsb1.getEmployeeNoTX(1);
        sfsb1.getEmployeeNoTX(1);
    }

    @Test
    @InSequence(2)
    public void testQueryNonTXTransactionalEntityManagerInvocations() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        String name = sfsb1.queryEmployeeNameNoTX(1);
        assertEquals("Query should of thrown NoResultException, which we indicate by returning 'success'", "success", name);
    }

    // Test that the queried Employee is detached as required by JPA 2.0 section 3.8.6
    // For a transaction scoped persistence context non jta-tx invocation, entities returned from Query
    // must be detached.
    @Test
    @InSequence(3)
    public void testQueryNonTXTransactionalDetach() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.createEmployee("Jill", "54 Country Lane", 2);
        Employee employee = sfsb1.queryEmployeeNoTX(2);
        assertNotNull(employee);

        boolean detached = sfsb1.isQueryEmployeeDetached(2);
        assertTrue("JPA 2.0 section 3.8.6 violated, query returned entity in non-tx that wasn't detached ", detached);
    }

    /**
     * Ensure that calling entityManager.flush outside of a transaction, throws a TransactionRequiredException
     *
     * @throws Exception
     */
    @Test
    @InSequence(4)
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
        // javax.ejb.EJBException: javax.persistence.TransactionRequiredException: no transaction is in progress
        while (error != null && !(error instanceof TransactionRequiredException) && error.getCause() != null) {
            error = error.getCause();
        }
        assertTrue(
                "attempting to persist entity with transactional entity manager and no transaction, should fail with a TransactionRequiredException"
                        + " but we instead got a " + error, error instanceof TransactionRequiredException);
    }


    /**
     * Tests JTA involving an EJB 3 SLSB which makes two DAO calls in transaction.
     * Scenarios:
     * 1) The transaction fails during the first DAO call and the JTA transaction is rolled back and no database changes should occur.
     * 2) The transaction fails during the second DAO call and the JTA transaction is rolled back and no database changes should occur.
     * 3) The transaction fails after the DAO calls and the JTA transaction is rolled back and no database changes should occur.
     */
    @Test
    @InSequence(5)
    public void testFailInDAOCalls() throws Exception {
        SLSB1 slsb1 = lookup("SLSB1", SLSB1.class);
        slsb1.addEmployee();

        String message = slsb1.failInFirstCall();
        assertEquals("DB should be unchanged, which we indicate by returning 'success'", "success", message);

        message = slsb1.failInSecondCall();
        assertEquals("DB should be unchanged, which we indicate by returning 'success'", "success", message);

        message = slsb1.failAfterCalls();
        assertEquals("DB should be unchanged, which we indicate by returning 'success'", "success", message);
    }

    @Test
    @InSequence(6)
    public void testUserTxRollbackDiscardsChanges() throws Exception {
        SFSBXPC sfsbxpc = lookup("SFSBXPC", SFSBXPC.class);
        sfsbxpc.createEmployeeNoTx("Amory Lorch", "Lannister House", 10);  // create the employee but leave in xpc
        Employee employee = sfsbxpc.lookup(10);
        assertNotNull("could read employee record from extended persistence context (not yet saved to db)", employee);

        // rollback any changes that haven't been saved yet
        sfsbxpc.forceRollbackAndLosePendingChanges(10, false);

        employee = sfsbxpc.lookup(10);
        assertNull("employee record should not be found in db after rollback", employee);

    }

    @Test
    @InSequence(7)
    public void testEnlistXPCInUserTx() throws Exception {
        SFSBXPC sfsbxpc = lookup("SFSBXPC", SFSBXPC.class);
        sfsbxpc.createEmployeeNoTx("Amory Lorch", "Lannister House", 20);  // create the employee but leave in xpc
        Employee employee = sfsbxpc.lookup(20);
        assertNotNull("could read employee record from extended persistence context (not yet saved to db)", employee);

        // start/end a user transaction without invoking the (extended) entity manager, which should cause the
        // pending changes to be saved to db
        sfsbxpc.savePendingChanges();

        sfsbxpc.forceRollbackAndLosePendingChanges(20, true);

        employee = sfsbxpc.lookup(20);
        assertNotNull("could read employee record from extended persistence context (wasn't saved to db during savePendingChanges())", employee);

    }
}
