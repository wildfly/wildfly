/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.util;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;

/**
 * Utility class for server reload operations in Elytron tests.
 *
 */
public class ServerReloadUtil {

    public static void executeReloadAndWaitForCompletion() throws Exception {
        try (ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient()) {
            ManagementClient managementClient = new ManagementClient(client,
                    TestSuiteEnvironment.getServerAddress(),
                    TestSuiteEnvironment.getServerPort(),
                    "remote+http");
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
    }
}
