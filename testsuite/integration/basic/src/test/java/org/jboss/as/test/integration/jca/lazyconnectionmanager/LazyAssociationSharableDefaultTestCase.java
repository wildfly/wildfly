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
 * Test cases for deploying a lazy association resource adapter archive
 *
 * @author <a href="mailto:jesper.pedersen@ironjacamar.org">Jesper Pedersen</a>
 * @author <a href="mailto:msimka@redhat.com">Martin Simka</a>
 */
@RunWith(Arquillian.class)
public class LazyAssociationSharableDefaultTestCase extends LazyAssociationAbstractTestCase {
    private static Logger logger = Logger.getLogger(LazyAssociationSharableDefaultTestCase.class);

    @Deployment(name = LazyAssociationAbstractTestCase.RAR_NAME)
    public static Archive<ResourceAdapterArchive> createDeployment() {
        return createResourceAdapter("ra-notx.xml",
                "ironjacamar-default.xml",
                LazyAssociationSharableDefaultTestCase.class);
    }

    @Resource(mappedName = "java:/eis/Lazy")
    private LazyConnectionFactory lcf;

    @Test
    public void testBasic() {
        assertNotNull(lcf);

        LazyConnection lc = null;
        try {
            lc = lcf.getConnection();

            assertTrue(lc.isManagedConnectionSet());
            assertTrue(lc.closeManagedConnection());
            assertFalse(lc.isManagedConnectionSet());

            assertTrue(lc.associate());

            assertTrue(lc.isManagedConnectionSet());

            assertFalse(lc.isEnlisted());
            assertTrue(lc.enlist());
            assertFalse(lc.isEnlisted());
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            fail("Throwable:" + t.getMessage());
        } finally {
            if (lc != null) { lc.close(); }
        }
    }

    /**
     * Two connections - one managed connection
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testTwoConnections() {
        assertNotNull(lcf);

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
            fail("Throwable:" + t.getMessage());
        } finally {
            if (lc1 != null) { lc1.close(); }

            if (lc2 != null) { lc2.close(); }
        }
    }
}
