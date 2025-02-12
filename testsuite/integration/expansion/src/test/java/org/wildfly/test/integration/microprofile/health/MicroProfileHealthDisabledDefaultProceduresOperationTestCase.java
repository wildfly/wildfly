/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;

import java.io.IOException;

import static org.jboss.as.controller.operations.common.Util.getEmptyOperation;
import static org.wildfly.test.integration.microprofile.health.MicroProfileHealthUtils.testManagementOperation;

public class MicroProfileHealthDisabledDefaultProceduresOperationTestCase
        extends MicroProfileHealthDisabledDefaultProceduresTestBase {
    @Override
    void checkGlobalOutcome(ManagementClient managementClient, String operation, boolean mustBeUP, String probeName, Integer expectedChecksCount) throws IOException {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "microprofile-health-smallrye");
        ModelNode checkOp = getEmptyOperation(operation, address);

        ModelNode response = managementClient.getControllerClient().execute(checkOp);
        testManagementOperation(response, mustBeUP, probeName, expectedChecksCount);
    }
}
