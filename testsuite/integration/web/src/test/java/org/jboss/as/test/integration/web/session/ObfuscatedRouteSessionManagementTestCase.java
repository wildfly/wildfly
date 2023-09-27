/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.session;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.client.helpers.ClientConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.junit.runner.RunWith;

/**
 * Runs {@link SessionManagementTestCase} with Undertow subsystem {@code obfuscate-session-route} attribute set to
 * {@code true}.
 *
 * @author Flavia Rainone
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ObfuscatedRouteSessionManagementTestCase.SetupTask.class)
public class ObfuscatedRouteSessionManagementTestCase extends SessionManagementTestCase {
    static class SetupTask implements ServerSetupTask {

        private static Exception failure;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            failure = null;
            try {
                final ModelNode op = Util.getWriteAttributeOperation(
                        PathAddress.pathAddress(SUBSYSTEM, "undertow"),
                        "obfuscate-session-route", true);
                runOperationAndReload(op, managementClient);
            } catch (Exception e) {
                failure = e;
                throw e;
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (failure != null)
                return;
            try {
                final ModelNode op = Util.getWriteAttributeOperation(
                        PathAddress.pathAddress(SUBSYSTEM, "undertow"),
                        "obfuscate-session-route", false);
                runOperationAndReload(op, managementClient);
            } catch (Exception e) {
                failure = e;
                throw e;
            }
        }

        private void runOperationAndReload(ModelNode operation, ManagementClient client) throws Exception {
            final ModelNode result = client.getControllerClient().execute(operation);
            if (result.hasDefined(FAILURE_DESCRIPTION)) {
                final String failureDesc = result.get(FAILURE_DESCRIPTION).toString();
                throw new RuntimeException(failureDesc);
            }
            if (!result.hasDefined(OUTCOME) || !SUCCESS.equals(result.get(OUTCOME).asString())) {
                throw new RuntimeException("Operation not successful; outcome = " + result.get(OUTCOME));
            }
            ServerReload.reloadIfRequired(client);
        }

        public static Exception getFailure() {
            return failure;
        }
    }
}
