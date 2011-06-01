/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.testsuite.integration.jpa.transaction;

import static org.junit.Assert.assertEquals;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Transaction tests
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class TransactionTestCase {

    private static final String ARCHIVE_NAME = "jpa_sessionfactory";

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"mypc\">" +
            "    <description>Persistence Unit." +
            "    </description>" +
            "  <jta-data-source>java:/H2DS</jta-data-source>" +
            "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
            "</properties>" +
            "  </persistence-unit>" +
            "</persistence>";

    private static InitialContext iniCtx;

    @BeforeClass
    public static void beforeClass() throws NamingException {
        iniCtx = new InitialContext();
    }

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(TransactionTestCase.class,
            Employee.class,
            SFSB1.class
        );

        jar.addAsResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        return jar;
    }

    protected static <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected static <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }

    /**
     * Ensure that calling entityManager.flush outside of a transaction, throws a TransactionRequiredException
     *
     * @throws Exception
     */
    @Test
    public void testTransactionRequiredException() throws Exception {
// uncomment after JBCTS-1103 is fixed.
//        Exception error = null;
//        try {
//            SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
//            sfsb1.createEmployeeNoTx("Sally", "1 home street", 1);
//        } catch (TransactionRequiredException e) {
//            error = e;
//        } catch (Exception failed) {
//            error = failed;
//        }
//        assertTrue(
//            "attempting to persist entity with transactional entity manager and no transaction, should fail with a TransactionRequiredException"
//                + " but we instead got a " + error, error instanceof TransactionRequiredException);
    }

    @Test
    public void testMultipleNonTXTransactionalEntityManagerInvocations() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.getEmployeeNoTX(1);   // For each call in, we will use a transactional entity manager
                                    // that isn't running in an transaction.  So, a new underlying
                                    // entity manager will be obtained.  The is to ensure that we don't blow up.
        sfsb1.getEmployeeNoTX(1);
        sfsb1.getEmployeeNoTX(1);
    }

    @Test
    public void testQueryNonTXTransactionalEntityManagerInvocations() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        String name = sfsb1.queryEmployeeNameNoTX(1);
        assertEquals("Query should of thrown NoResultException, which we indicate by returning 'success'","success", name);
    }
}
