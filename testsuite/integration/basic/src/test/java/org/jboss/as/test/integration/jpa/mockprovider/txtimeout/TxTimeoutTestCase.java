/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jpa.mockprovider.txtimeout;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Transaction timeout test that ensures that the entity manager is not closed concurrently while application
 * is using EntityManager.
 * AS7-6586
 *
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class TxTimeoutTestCase {

    private static final String ARCHIVE_NAME = "jpa_txTimeoutTestWithMockProvider";

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive persistenceProvider = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        persistenceProvider.addClasses(
                    TestEntityManagerFactory.class,
                    TestEntityManager.class,
                    TestPersistenceProvider.class
                );

        // META-INF/services/javax.persistence.spi.PersistenceProvider
        persistenceProvider.addAsResource(new StringAsset("org.jboss.as.test.integration.jpa.mockprovider.txtimeout.TestPersistenceProvider"),
                "META-INF/services/javax.persistence.spi.PersistenceProvider");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");

        JavaArchive ejbjar = ShrinkWrap.create(JavaArchive.class, "ejbjar.jar");

        ejbjar.addAsManifestResource(emptyEjbJar(), "ejb-jar.xml");
        ejbjar.addClasses(TxTimeoutTestCase.class,
                SFSB1.class
        );
        ejbjar.addAsManifestResource(TxTimeoutTestCase.class.getPackage(), "persistence.xml", "persistence.xml");

        ear.addAsModule(ejbjar);        // add ejbjar to root of ear

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        lib.addClasses(Employee.class, TxTimeoutTestCase.class);
        ear.addAsLibraries(lib, persistenceProvider);
        ear.addAsManifestResource(new StringAsset("Dependencies: org.jboss.jboss-transaction-spi export \n"), "MANIFEST.MF");
        return ear;

    }

    private static StringAsset emptyEjbJar() {
        return new StringAsset(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<ejb-jar xmlns=\"http://java.sun.com/xml/ns/javaee\" \n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd\"\n" +
                "         version=\"3.0\">\n" +
                "   \n" +
                "</ejb-jar>");
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        try {
            return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
        } catch (NamingException e) {
            dumpJndi("");
            throw e;
        }
    }

    // TODO: move this logic to a common base class (might be helpful for writing new tests)
    private void dumpJndi(String s) {
        try {
            dumpTreeEntry(iniCtx.list(s), s);
        } catch (NamingException ignore) {
        }
    }

    private void dumpTreeEntry(NamingEnumeration<NameClassPair> list, String s) throws NamingException {
        System.out.println("\ndump " + s);
        while (list.hasMore()) {
            NameClassPair ncp = list.next();
            System.out.println(ncp.toString());
            if (s.length() == 0) {
                dumpJndi(ncp.getName());
            } else {
                dumpJndi(s + "/" + ncp.getName());
            }
        }
    }

    /**
     * Tests if the entity manager is closed by the application thread.
     * The transaction does not timeout for this test, so the EntityManager.close() will happen in the application
     * thread.
     *
     * @throws Exception
     */
    @Test
    @InSequence(1)
    public void test_positiveTxTimeoutTest() throws Exception {
        TestEntityManager.clearState();
        assertFalse("entity manager state is not reset", TestEntityManager.getClosedByReaperThread());
        SFSB1 sfsb1 = lookup("ejbjar/SFSB1", SFSB1.class);
        sfsb1.createEmployee("Wily", "1 Appletree Lane", 10);
        assertFalse("entity manager should be closed by application thread but was closed by TX Reaper thread",
                TestEntityManager.getClosedByReaperThread());
    }

    /**
     * Ensures that the entity manager is not closed by the reaper thread.
     * The transaction times out for this test, but the EntityManager.close should be ignored from the reaper thread.
     *
     * Ignoring this test, since it will be faster to only run test_negativeTxTimeoutVerifyReaperThreadCanceledTxTest,
     * which has the same test actions (with the addition of the tx reaper thread test)
     *
     * @throws Exception
     */
    @Test
    @InSequence(2)
    @Ignore
    public void test_negativeTxTimeoutTest() throws Exception {
        TestEntityManager.clearState();
        assertFalse("entity manager state is not reset", TestEntityManager.getClosedByReaperThread());
        SFSB1 sfsb1 = lookup("ejbjar/SFSB1", SFSB1.class);

        try {
            sfsb1.createEmployeeWaitForTxTimeout(false, "Wily", "1 Appletree Lane", 10);
        } catch (Exception e) { // ignore the tx rolled back exception
            System.out.println("ignoring the " + e.getMessage());
        }
        assertFalse("entity manager should not of been closed by the reaper thread", TestEntityManager.getClosedByReaperThread());
    }

    /**
     * Repeat the same test as test_negativeTxTimeoutTest but also ensure that the reaper thread canceled the tx.
     * If this test fails, it could be that the tx reaper thread name changed or we are using a different tx manager.
     *
     * @throws Exception
     */
    @Test
    @InSequence(3)
    public void test_negativeTxTimeoutVerifyReaperThreadCanceledTxTest() throws Exception {
        TestEntityManager.clearState();
        assertFalse("entity manager state is not reset", TestEntityManager.getClosedByReaperThread());
        SFSB1 sfsb1 = lookup("ejbjar/SFSB1", SFSB1.class);

        try {
            sfsb1.createEmployeeWaitForTxTimeout(true, "Wily", "1 Appletree Lane", 10);
        } catch (Exception e) { // ignore the tx rolled back exception
            System.out.println("ignoring the " + e.getMessage());
        }
        assertFalse("entity manager should not of been closed by the reaper thread", TestEntityManager.getClosedByReaperThread());
        assertTrue("transaction was canceled by reaper thread", sfsb1.isAfterCompletionCalledByTMTimeoutThread());
    }

}
