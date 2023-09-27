/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.lazyconnectionmanager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.annotation.Resource;
import jakarta.transaction.UserTransaction;

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
 * Test cases for deploying a lazy association resource adapter archive using LocalTransaction with enlistment=false
 *
 * @author <a href="mailto:jesper.pedersen@ironjacamar.org">Jesper Pedersen</a>
 * @author <a href="mailto:msimka@redhat.com">Martin Simka</a>
 */
@RunWith(Arquillian.class)
public class LazyAssociationLocalTransactionEnlistmentFalseTestCase extends LazyAssociationAbstractTestCase {
    private static Logger logger = Logger.getLogger(LazyAssociationLocalTransactionEnlistmentFalseTestCase.class);

    @Deployment(name = LazyAssociationAbstractTestCase.RAR_NAME)
    public static Archive<ResourceAdapterArchive> createDeployment() {
        return createResourceAdapter("ra-localtx.xml",
                "ironjacamar-enlistmentfalse.xml",
                LazyAssociationLocalTransactionEnlistmentFalseTestCase.class);
    }

    @Resource(mappedName = "java:/eis/Lazy")
    private LazyConnectionFactory lcf;

    @Resource(mappedName = "java:jboss/UserTransaction")
    private UserTransaction userTransaction;

    @Test
    public void verifyEagerlyEnlisted() throws Throwable {
        assertNotNull(lcf);
        assertNotNull(userTransaction);

        boolean status = true;
        userTransaction.begin();

        LazyConnection lc = null;
        try {
            lc = lcf.getConnection();

            assertTrue(lc.isManagedConnectionSet());
            assertTrue(lc.closeManagedConnection());
            assertFalse(lc.isManagedConnectionSet());
            assertTrue(lc.associate());
            assertTrue(lc.isManagedConnectionSet());

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
}
