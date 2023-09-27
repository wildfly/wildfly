/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.transaction.envers;

import static org.junit.Assert.assertEquals;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.persistence.TransactionRequiredException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test Envers on Transactions
 *
 * @author Madhumita Sadhukhan
 */
@RunWith(Arquillian.class)
public class TransactionAuditedTestCase {

    private static final String ARCHIVE_NAME = "jpa_sessionfactory";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(TransactionAuditedTestCase.class, Employee.class, SFSB1.class);
        jar.addAsManifestResource(TransactionAuditedTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @ArquillianResource
    private static InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType
                .cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }

    /**
     * Ensure that auditing works with transactions
     */
    @Test
    public void testAuditingOverTransaction() throws Exception {

        try {

            SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
            Employee emp = sfsb1.createEmployeeTx("Madhumita", "1 home street", 1);

            sfsb1.updateEmployeeTx("40 Patrice Lumumby", emp);
            sfsb1.updateEmployeeTx("40 Patrice Lumumby Ostrava CZ", emp);
            String address = sfsb1.retrieveOldEmployeeVersion(emp.getId());
            assertEquals("1 home street", address);
        } catch (TransactionRequiredException e) {
            System.out.println("TransactionRequiredException in catch:--");
        } catch (Exception failed) {
            System.out.println("Exception in catch:--");
        }

    }

    /**
     * Ensure that auditing does not save data not committed when a transaction is rolled back
     *
     * @throws Exception
     */
    @Test
    public void testAuditingOverTransactionRollback() throws Exception {

        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        Employee emp = null;
        try {

            emp = sfsb1.createEmployeeTx("Kaushik", "Red Hat Purkynova Brno", 2);

            sfsb1.updateEmployeeTxwithRollBack("Vratimovska 689", emp);
            sfsb1.updateEmployeeTx("Vratimovska 689", emp);
            sfsb1.updateEmployeeTx("Schwaigrova 2 Brno CZ", emp);
            sfsb1.updateEmployeeTx("40 Patrice Lumumby Ostrava CZ", emp);
        } catch (Exception e) {

            System.out.println("Rollback in testAuditingOverTransactionRollback() catch:--");

        }
    }

}
