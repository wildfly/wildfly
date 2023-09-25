/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.httpobfuscatedroute;

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

import org.wildfly.extension.undertow.UndertowExtension;

import static org.wildfly.extension.undertow.Constants.OBFUSCATE_SESSION_ROUTE;

/**
 * ServerSetupTask that sets Undertow subsystem obfuscate-session-route attribute to true
 * and reloads the server.
 *
 * @author Flavia Rainone
 */
public class UndertowObfuscatedSessionRouteServerSetup implements ServerSetupTask {

    private static Exception failure;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        failure = null;
        try {
            final ModelNode op = Util.getWriteAttributeOperation(
                    PathAddress.pathAddress(SUBSYSTEM, UndertowExtension.SUBSYSTEM_NAME),
                    OBFUSCATE_SESSION_ROUTE, true);
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
            final ModelNode op = Util.getUndefineAttributeOperation(
                    PathAddress.pathAddress(SUBSYSTEM, UndertowExtension.SUBSYSTEM_NAME), OBFUSCATE_SESSION_ROUTE);
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
