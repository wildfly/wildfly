/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.manual.microprofile.lra;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.base.AbstractExpressionSupportTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.jboss.as.controller.operations.common.Util.createAddOperation;

@RunWith(Arquillian.class)
public class LRAExpressionSupportTestCase extends AbstractExpressionSupportTestCase {

    private static final String JBOSSAS = "jbossas-custom";
    private ManagementClient managementClient;

    private void setup(String containerName) throws Exception {
        if (!container.isStarted(containerName)) {
            container.start(containerName);
        }
        managementClient = createManagementClient();
        ModelControllerClient controllerClient = managementClient.getControllerClient();

        controllerClient.execute(createAddOperation(PathAddress.pathAddress("extension", "org.wildfly.extension.microprofile.lra-coordinator")));
        controllerClient.execute(createAddOperation(PathAddress.pathAddress("extension", "org.wildfly.extension.microprofile.lra-participant")));
        controllerClient.execute(createAddOperation(PathAddress.pathAddress("subsystem", "microprofile-lra-coordinator")));
        controllerClient.execute(createAddOperation(PathAddress.pathAddress("subsystem", "microprofile-lra-participant")));
    }

    private void teardown(String containerName) throws IOException {
        container.stop(containerName);
        managementClient.close();
    }

    private void testContainer(String containerName) throws Exception {
        try {
            setup(containerName);
            test(managementClient);
        } finally {
            teardown(containerName);
        }
    }

    @Test
    public void testDefault() throws Exception {
        testContainer(JBOSSAS);
    }
}
