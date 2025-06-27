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

@ServerSetup(StabilityDefaultServerSetupTestCase.DefaultStabilitySetupTask.class)
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class StabilityDefaultServerSetupTestCase extends AbstractStabilityServerSetupTaskTest {
    public StabilityDefaultServerSetupTestCase() {
        super(Stability.DEFAULT);
    }

    public static class DefaultStabilitySetupTask extends StabilityServerSetupSnapshotRestoreTasks.Default {
        @Override
        protected void doSetup(ManagementClient managementClient) throws Exception {
            // Not really needed since the resulting written xml will be of a higher stability level
            // than the server. Still we are doing it for experimental preview, so it doesn't hurt to
            // do the same here.
            AbstractStabilityServerSetupTaskTest.addSystemProperty(managementClient, StabilityDefaultServerSetupTestCase.class);
        }
    }
}
