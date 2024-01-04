/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.timeout;

import jakarta.ejb.NoSuchEJBException;

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
 * Tests that the default stateful timeout value 1000 configured in the server works.
 */
@RunWith(Arquillian.class)
@ServerSetup(DefaultStatefulTimeout1000TestCase.ServerSetup.class)
public class DefaultStatefulTimeout1000TestCase extends StatefulTimeoutTestBase2 {

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
     * will take the configured server default-stateful-bean-session-timeout (1000 milliseconds)
     */
    @Test
    public void testNoTimeoutByApplication() throws Exception {
        Assert.assertEquals(1000, readAttribute().asLong());

        TimeoutNotConfiguredBean timeoutNotConfiguredBean = lookup(TimeoutNotConfiguredBean.class);
        Assert.assertFalse(TimeoutNotConfiguredBean.preDestroy);
        timeoutNotConfiguredBean.doStuff();
        Thread.sleep(2000);
        try {
            timeoutNotConfiguredBean.doStuff();
            throw new RuntimeException("Expecting NoSuchEJBException");
        } catch (NoSuchEJBException expected) {
            //ignore
        }
        Assert.assertTrue(TimeoutNotConfiguredBean.preDestroy);
    }

    /**
     * The server setup task that configures ejb3 subsystem attribute default-stateful-bean-session-timeout.
     */
    public static class ServerSetup extends SnapshotRestoreSetupTask {
        protected void doSetup(ManagementClient client, String containerId) throws Exception {
            ModelNode writeDefaultStatefulTimeoutOp = Util.getWriteAttributeOperation(EJB3_ADDRESS, DEFAULT_STATEFUL_TIMEOUT_NAME, 1000);
            ManagementOperations.executeOperation(client.getControllerClient(), writeDefaultStatefulTimeoutOp);
        }
    }
}
