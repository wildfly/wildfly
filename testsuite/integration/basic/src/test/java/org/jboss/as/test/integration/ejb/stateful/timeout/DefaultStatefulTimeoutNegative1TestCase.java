/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.timeout;

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
 * Tests that the default stateful timeout value -1 configured in the server works.
 */
@RunWith(Arquillian.class)
@ServerSetup(DefaultStatefulTimeoutNegative1TestCase.ServerSetup.class)
public class DefaultStatefulTimeoutNegative1TestCase extends StatefulTimeoutTestBase2 {
    private static final int CONFIGURED_TIMEOUT = -1;

    /**
     * stateful timeout is configured with annotation by application, and the server
     * default-stateful-bean-session-timeout must not take effect.
     */
    @Test
    public void testStatefulTimeoutFromAnnotation() throws Exception {
        Assert.assertEquals(CONFIGURED_TIMEOUT, readAttribute().asLong());
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
     * will take the configured server default-stateful-bean-session-timeout (-1, no timeout or removal)
     */
    @Test
    public void testNoTimeoutByApplication() throws Exception {
        TimeoutNotConfiguredBean timeoutNotConfiguredBean = lookup(TimeoutNotConfiguredBean.class);
        timeoutNotConfiguredBean.doStuff();
        Thread.sleep(2000);
        timeoutNotConfiguredBean.doStuff();
        Assert.assertFalse(TimeoutNotConfiguredBean.preDestroy);
    }

    /**
     * The server setup task that configures ejb3 subsystem attribute default-stateful-bean-session-timeout.
     */
    public static class ServerSetup extends SnapshotRestoreSetupTask {
        protected void doSetup(ManagementClient client, String containerId) throws Exception {
            ModelNode writeDefaultStatefulTimeoutOp = Util.getWriteAttributeOperation(EJB3_ADDRESS, DEFAULT_STATEFUL_TIMEOUT_NAME, CONFIGURED_TIMEOUT);
            ManagementOperations.executeOperation(client.getControllerClient(), writeDefaultStatefulTimeoutOp);
        }
    }
}
