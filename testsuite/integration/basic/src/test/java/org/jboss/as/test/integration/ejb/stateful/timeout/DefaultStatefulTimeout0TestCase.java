/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.stateful.timeout;

import javax.ejb.NoSuchEJBException;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the default stateful timeout value 0 configured in the server works.
 */
@RunWith(Arquillian.class)
@ServerSetup(DefaultStatefulTimeout0TestCase.ServerSetup.class)
public class DefaultStatefulTimeout0TestCase extends StatefulTimeoutTestBase2 {
    private static final int CONFIGURED_TIMEOUT = 0;

    /**
     * stateful timeout is configured with annotation by application, and the server
     * default-stateful-bean-session-timeout must not take effect.
     */
    @Test
    public void testStatefulTimeoutFromAnnotation() throws Exception {
        super.testStatefulTimeoutFromAnnotation();
    }

    /**
     * stateful timeout is configured with annotation by application, and the server
     * default-stateful-bean-session-timeout must not take effect.
     */
    @Test
    public void testStatefulTimeoutWithPassivation() throws Exception {
        super.testStatefulTimeoutWithPassivation();
    }

    /**
     * stateful timeout is configured with descriptor by application, and the server
     * default-stateful-bean-session-timeout must not take effect.
     */
    @Test
    public void testStatefulTimeoutFromDescriptor() throws Exception {
        super.testStatefulTimeoutFromDescriptor();
    }

    /**
     * stateful timeout is configured with annotation by application, and the server
     * default-stateful-bean-session-timeout must not take effect.
     */
    @Test
    public void testStatefulBeanNotDiscardedWhileInTransaction() throws Exception {
        super.testStatefulBeanNotDiscardedWhileInTransaction();
    }

    /**
     * stateful timeout is configured with annotation by application, and the server
     * default-stateful-bean-session-timeout must not take effect.
     */
    @Test
    public void testStatefulBeanWithPassivationNotDiscardedWhileInTransaction() throws Exception {
        super.testStatefulBeanWithPassivationNotDiscardedWhileInTransaction();
    }

    /**
     * Verifies that a stateful bean that does not configure stateful timeout via annotation or descriptor
     * will take the configured server default-stateful-bean-session-timeout (0 milliseconds, immediate timeout)
     */
    @Test
    public void testNoTimeoutByApplication() throws Exception {
        Assert.assertEquals(CONFIGURED_TIMEOUT, readAttribute().asLong());

        TimeoutNotConfiguredBean timeoutNotConfiguredBean = lookup(TimeoutNotConfiguredBean.class);
        timeoutNotConfiguredBean.doStuff();
        Thread.sleep(2000);
        try {
            timeoutNotConfiguredBean.doStuff();
            throw new RuntimeException("Expecting NoSuchEJBException");
        } catch (NoSuchEJBException expected) {
            //ignore
        }

        //re-configure the server default-stateful-bean-session-timeout to 600,000, and then 0 milliseconds,
        try {
            writeAttribute(600_000);
            Assert.assertEquals(600_000, readAttribute().asLong());
        } finally {
            writeAttribute(CONFIGURED_TIMEOUT);
        }
    }

    /**
     * The server setup task that configures ejb3 subsystem attribute default-stateful-bean-session-timeout.
     */
    public static class ServerSetup extends SnapshotRestoreSetupTask {
        protected void doSetup(ManagementClient client, String containerId) throws Exception {
            ModelNode writeDefaultStatefulTimeoutOp = Util.getWriteAttributeOperation(
                    EJB3_ADDRESS, DEFAULT_STATEFUL_TIMEOUT_NAME, CONFIGURED_TIMEOUT);
            ManagementOperations.executeOperation(client.getControllerClient(), writeDefaultStatefulTimeoutOp);
        }
    }
}
