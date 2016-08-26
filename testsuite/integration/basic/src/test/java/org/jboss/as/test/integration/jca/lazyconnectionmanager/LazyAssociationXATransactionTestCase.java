/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.lazyconnectionmanager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.annotation.Resource;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyConnection;
import org.jboss.as.test.integration.jca.lazyconnectionmanager.rar.LazyConnectionFactory;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test cases for deploying a lazy association resource adapter archive using XATransaction
 *
 * @author <a href="mailto:jesper.pedersen@ironjacamar.org">Jesper Pedersen</a>
 * @author <a href="mailto:msimka@redhat.com">Martin Simka</a>
 */
@RunWith(Arquillian.class)
public class LazyAssociationXATransactionTestCase extends LazyAssociationAbstractTestCase {
    private static Logger logger = Logger.getLogger(LazyAssociationXATransactionTestCase.class);

    @Deployment(name = LazyAssociationAbstractTestCase.RAR_NAME)
    public static Archive<ResourceAdapterArchive> createDeployment() {
        return createResourceAdapter("ra-xatx.xml",
                "ironjacamar-xa-default.xml",
                LazyAssociationXATransactionTestCase.class);
    }

    @Resource(mappedName = "java:/eis/Lazy")
    private LazyConnectionFactory lcf;

    @Resource(mappedName = "java:jboss/UserTransaction")
    private UserTransaction userTransaction;

    @Test
    public void testBasic() throws Throwable {
        assertNotNull(lcf);
        assertNotNull(userTransaction);

        boolean status = true;
        userTransaction.begin();

        LazyConnection lc = null;
        try {
            lc = lcf.getConnection();

            assertTrue(lc.isManagedConnectionSet());

            lc.closeManagedConnection();

            assertFalse(lc.isManagedConnectionSet());

            lc.associate();

            assertTrue(lc.isManagedConnectionSet());

            assertFalse(lc.isEnlisted());
            assertTrue(lc.enlist());
            assertTrue(lc.isEnlisted());
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            status = false;
            fail("Throwable:" + t.getMessage());
        } finally {
            if (lc != null) { lc.close(); }

            if (status) {
                userTransaction.commit();
            } else {
                userTransaction.rollback();
            }
        }
    }

    /**
     * Two connections - one managed connection - without enlistment
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testTwoConnectionsWithoutEnlistment() throws Throwable {
        assertNotNull(lcf);
        assertNotNull(userTransaction);

        boolean status = true;
        userTransaction.begin();

        LazyConnection lc1 = null;
        LazyConnection lc2 = null;
        try {
            lc1 = lcf.getConnection();

            assertTrue(lc1.isManagedConnectionSet());

            lc2 = lcf.getConnection();

            assertTrue(lc2.isManagedConnectionSet());
            assertFalse(lc1.isManagedConnectionSet());

            assertTrue(lc2.closeManagedConnection());

            assertFalse(lc1.isManagedConnectionSet());
            assertFalse(lc2.isManagedConnectionSet());

            assertTrue(lc1.associate());

            assertTrue(lc1.isManagedConnectionSet());
            assertFalse(lc2.isManagedConnectionSet());
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            status = false;
            fail("Throwable:" + t.getMessage());
        } finally {
            if (lc1 != null) { lc1.close(); }

            if (lc2 != null) { lc2.close(); }

            if (status) {
                userTransaction.commit();
            } else {
                userTransaction.rollback();
            }
        }
    }

    /**
     * Two connections - one managed connection - with enlistment
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testTwoConnectionsWithEnlistment() throws Throwable {
        assertNotNull(lcf);
        assertNotNull(userTransaction);

        boolean status = true;
        userTransaction.begin();

        LazyConnection lc1 = null;
        LazyConnection lc2 = null;
        try {
            lc1 = lcf.getConnection();

            assertTrue(lc1.isManagedConnectionSet());
            assertFalse(lc1.isEnlisted());
            assertTrue(lc1.enlist());
            assertTrue(lc1.isEnlisted());

            lc2 = lcf.getConnection();

            assertTrue(lc2.isManagedConnectionSet());
            assertFalse(lc1.isManagedConnectionSet());

            assertTrue(lc2.closeManagedConnection());

            assertFalse(lc1.isManagedConnectionSet());
            assertFalse(lc2.isManagedConnectionSet());

            assertTrue(lc1.associate());

            assertTrue(lc1.isManagedConnectionSet());
            assertFalse(lc2.isManagedConnectionSet());
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            status = false;
            fail("Throwable:" + t.getMessage());
        } finally {
            if (lc1 != null) { lc1.close(); }

            if (lc2 != null) { lc2.close(); }

            if (status) {
                userTransaction.commit();
            } else {
                userTransaction.rollback();
            }
        }
    }

}
