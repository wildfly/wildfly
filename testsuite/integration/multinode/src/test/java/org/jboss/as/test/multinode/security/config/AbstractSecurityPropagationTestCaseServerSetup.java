/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.multinode.security.config;

import java.util.Arrays;
import java.util.ListIterator;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * @author bmaxwell
 *
 */
public abstract class AbstractSecurityPropagationTestCaseServerSetup implements ServerSetupTask, ServerConfiguration {

    protected AbstractSecurityPropagationTestCaseServerSetup() {
    }

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        if ("multinode-server".equals(containerId))
            setup(managementClient, getServerConfigChanges());
        else if ("multinode-client".equals(containerId))
            setup(managementClient, getClientConfigChanges());
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        if ("multinode-server".equals(containerId))
            tearDown(managementClient, getServerConfigChanges());
        else if ("multinode-client".equals(containerId))
            tearDown(managementClient, getClientConfigChanges());
    }

    protected void setup(final ManagementClient managementClient, ConfigChange[] configChanges) throws Exception {
        if (configChanges == null || configChanges.length == 0) {
            return;
        }
        try (CLIWrapper cli = new CLIWrapper(managementClient.getMgmtAddress(), managementClient.getMgmtPort(), true)) {
            for (final ConfigChange configChange : configChanges) {
                configChange.apply(managementClient.getControllerClient(), cli);
            }
            cli.sendLine("reload");
        }
    }

    protected void tearDown(ManagementClient managementClient, ConfigChange[] configChanges) throws Exception {
        if (configChanges == null || configChanges.length == 0) {
            return;
        }

        try (CLIWrapper cli = new CLIWrapper(managementClient.getMgmtAddress(), managementClient.getMgmtPort(), true)) {
            final ListIterator<ConfigChange> reverseConfigIt = Arrays.asList(configChanges)
                    .listIterator(configChanges.length);
            while (reverseConfigIt.hasPrevious()) {
                final ConfigChange configChange = reverseConfigIt.previous();
                configChange.revert(managementClient.getControllerClient(), cli);
            }
            cli.sendLine("reload");
        }
    }
}
