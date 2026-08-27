/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.setuptasks;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATISTICS_ENABLED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

/**
 * Maps the {@link #USER_MONITOR} management user to the Monitor role and enables Undertow
 * statistics, then restores both to their pre-test state on tear-down. Each change is
 * applied only when the current state differs from the target, and is reverted only if
 * this task actually made it - so it does not clobber a value another setup task already
 * owns. The {@code Monitor}/{@code norole} management users themselves are provisioned
 * into the server by the build (shared {@code mgmt-users.properties}). Enabling RBAC and
 * securing the endpoints is done per-test.
 */
public class RbacRealmSetupTask extends AbstractSetupTask {
    public static final PathAddress authorization =
            PathAddress.pathAddress(CORE_SERVICE, MANAGEMENT)
                       .append(ACCESS, AUTHORIZATION);
    // RBAC user mapped to the Monitor role, used to authenticate scrapes of the secured management endpoints.
    public static final String USER_MONITOR = "Monitor";
    // RBAC user in the management-realm that does not have any roles assigned.
    public static final String USER_NOROLE = "norole";
    public static final String TEST_RBAC_PASSWORD = "testSuitePassword";

    private static final PathAddress MONITOR_ROLE_MAPPING =
            authorization.append("role-mapping", USER_MONITOR);
    private static final PathAddress UNDERTOW =
            PathAddress.pathAddress("subsystem", "undertow");

    private boolean roleMappingAdded;
    private boolean statisticsEnabledChanged;

    @Override
    public void setup(ManagementClient managementClient,
                      String containerId) throws Exception {
        // Map the Monitor user to the Monitor role, but only if the mapping is not already present.
        if (!Operations.isSuccessfulOutcome(
                executeRead(managementClient, MONITOR_ROLE_MAPPING.toModelNode()))) {
            List<ModelNode> operations = new ArrayList<>();
            operations.add(Util.createAddOperation(MONITOR_ROLE_MAPPING));
            ModelNode addInclude = Util.createAddOperation(
                    MONITOR_ROLE_MAPPING.append("include", USER_MONITOR));
            addInclude.get("name").set(USER_MONITOR);
            addInclude.get("type").set("USER");
            operations.add(addInclude);
            executeOp(managementClient, Util.createCompositeOperation(operations));
            roleMappingAdded = true;
        }

        // Enable Undertow statistics, but only if they are not already enabled. The default undertow config carries
        // statistics-enabled as ${wildfly.undertow.statistics-enabled:false}, so it must be read with expressions
        // resolved - asBoolean() throws on an unresolved expression node.
        if (!isUndertowStatisticsEnabled(managementClient)) {
            executeOp(managementClient,
                      writeAttribute("undertow", STATISTICS_ENABLED, "true"));
            statisticsEnabledChanged = true;
        }

        ServerReload.reloadIfRequired(managementClient);
    }

    @Override
    public void tearDown(ManagementClient managementClient,
                         String containerId) throws Exception {
        if (statisticsEnabledChanged) {
            executeOp(managementClient, clearAttribute("undertow", STATISTICS_ENABLED));
        }
        if (roleMappingAdded) {
            List<ModelNode> operations = new ArrayList<>();
            operations.add(Util.createRemoveOperation(
                    MONITOR_ROLE_MAPPING.append("include", USER_MONITOR)));
            operations.add(Util.createRemoveOperation(MONITOR_ROLE_MAPPING));
            executeOp(managementClient, Util.createCompositeOperation(operations));
        }

        ServerReload.reloadIfRequired(managementClient);
    }

    private boolean isUndertowStatisticsEnabled(ManagementClient managementClient) throws IOException {
        ModelNode op = Operations.createReadAttributeOperation(UNDERTOW.toModelNode(), STATISTICS_ENABLED);
        op.get("resolve-expressions").set(true);
        ModelNode result = managementClient.getControllerClient().execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException("Failed to read undertow " + STATISTICS_ENABLED + ": "
                    + Operations.getFailureDescription(result).asString());
        }
        return result.get("result").asBoolean(false);
    }
}
