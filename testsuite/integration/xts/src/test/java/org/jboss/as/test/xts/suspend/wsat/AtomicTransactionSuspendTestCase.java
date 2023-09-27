/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.suspend.wsat;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.xts.suspend.AbstractTestCase;
import org.jboss.as.test.xts.util.DeploymentHelper;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(Arquillian.class)
public class AtomicTransactionSuspendTestCase extends AbstractTestCase {
    @TargetsContainer(EXECUTOR_SERVICE_CONTAINER)
    @Deployment(name = EXECUTOR_SERVICE_ARCHIVE_NAME, testable = false)
    public static WebArchive getExecutorServiceArchive() {
        return getExecutorServiceArchiveBase().addClasses(AtomicTransactionExecutionService.class,
                AtomicTransactionRemoteService.class, TransactionParticipant.class)
                .addAsManifestResource(DeploymentHelper.createPermissions(), "permissions.xml");
    }

    @TargetsContainer(REMOTE_SERVICE_CONTAINER)
    @Deployment(name = REMOTE_SERVICE_ARCHIVE_NAME, testable = false)
    public static WebArchive getRemoteServiceArchive() {
        return getRemoteServiceArchiveBase().addClasses(AtomicTransactionRemoteService.class,
                TransactionParticipant.class)
                .addAsManifestResource(DeploymentHelper.createPermissions(), "permissions.xml");
    }

    protected void assertParticipantInvocations(List<String> invocations) {
        assertEquals(Arrays.asList("prepare", "commit"), invocations);
    }

}
