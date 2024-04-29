/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.stateless.callerprincipal;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;

import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

/**
 *
 * Server setup task for test Ejb2GetCallerPrincipalTestCase.
 * Configures security domain and http connector in EJB3 and remoting subsystems.
 *
 * @author Daniel Cihak
 */
public class Ejb2GetCallerPrincipalServerSetupTask extends SnapshotRestoreSetupTask {

    @Override
    public void doSetup(ManagementClient managementClient, String containerId) throws Exception {
        // /subsystem=remoting/http-connector=http-remoting-connector:write-attribute(name=sasl-authentication-factory, value=application-sasl-authentication)
        ModelNode updateRemotingConnector = createOpNode("subsystem=remoting/http-connector=http-remoting-connector", WRITE_ATTRIBUTE_OPERATION);
        updateRemotingConnector.get(ClientConstants.NAME).set("sasl-authentication-factory");
        updateRemotingConnector.get(ClientConstants.VALUE).set("application-sasl-authentication");

        ModelNode updateOp = Util.createCompositeOperation(List.of(updateRemotingConnector));
        updateOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        updateOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(updateOp, managementClient.getControllerClient());

        ServerReload.reloadIfRequired(managementClient);
    }
}
