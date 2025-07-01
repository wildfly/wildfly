/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.stability;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.version.Stability;

import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

@ServerSetup(StabilityExperimentalServerSetupTestCase.ExperimentalStabilitySetupTask.class)
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class StabilityExperimentalServerSetupTestCase extends AbstractStabilityServerSetupTaskTest {
    public StabilityExperimentalServerSetupTestCase() {
        super(Stability.EXPERIMENTAL);
    }


    public static class ExperimentalStabilitySetupTask extends StabilityServerSetupSnapshotRestoreTasks.Experimental {
        @Override
        protected void doSetup(ManagementClient managementClient) throws Exception {
            // Write a system property so the model ges stored with a lower stability level.
            // This is to make sure we can reload back to the higher level from the snapshot
            AbstractStabilityServerSetupTaskTest.addSystemProperty(managementClient, StabilityExperimentalServerSetupTestCase.class);
        }
    }

}
