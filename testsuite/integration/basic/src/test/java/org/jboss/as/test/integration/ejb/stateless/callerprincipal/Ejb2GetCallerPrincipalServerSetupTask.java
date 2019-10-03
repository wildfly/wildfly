/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ejb.stateless.callerprincipal;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
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
        List<ModelNode> operations = new ArrayList<>();

        // /subsystem=ejb3/application-security-domain=other:add(security-domain=ApplicationDomain)
        ModelNode addEjbDomain = createOpNode("subsystem=ejb3/application-security-domain=other", ADD);
        addEjbDomain.get("security-domain").set("ApplicationDomain");
        operations.add(addEjbDomain);

        // /subsystem=remoting/http-connector=http-remoting-connector:write-attribute(name=sasl-authentication-factory, value=application-sasl-authentication)
        ModelNode updateRemotingConnector = createOpNode("subsystem=remoting/http-connector=http-remoting-connector", WRITE_ATTRIBUTE_OPERATION);
        updateRemotingConnector.get(ClientConstants.NAME).set("sasl-authentication-factory");
        updateRemotingConnector.get(ClientConstants.VALUE).set("application-sasl-authentication");
        operations.add(updateRemotingConnector);

        ModelNode updateOp = Operations.createCompositeOperation(operations);
        updateOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        updateOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        CoreUtils.applyUpdate(updateOp, managementClient.getControllerClient());

        ServerReload.reloadIfRequired(managementClient);
    }
}
