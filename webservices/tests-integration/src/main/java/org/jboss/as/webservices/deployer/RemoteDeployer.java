/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.webservices.deployer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.AUTHENTICATION;
import static org.jboss.as.security.Constants.CLASSIC;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.LOGIN_MODULES;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.wsf.spi.deployer.Deployer;

/**
 * Remote deployer that uses AS7 client deployment API.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class RemoteDeployer implements Deployer {

    private static final Logger LOGGER = Logger.getLogger(RemoteDeployer.class);
    private static final int PORT = 9999;
    private final Map<URL, String> url2Id = new HashMap<URL, String>();
    private final InetAddress address = InetAddress.getByName("127.0.0.1");

    private ServerDeploymentManager deploymentManager;

    public RemoteDeployer() throws IOException {
        deploymentManager = ServerDeploymentManager.Factory.create(address, PORT);
    }

    @Override
    public void deploy(final URL archiveURL) throws Exception {
        final DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan().add(archiveURL).andDeploy();
        final DeploymentPlan plan = builder.build();
        final DeploymentAction deployAction = builder.getLastAction();
        final String uniqueId = deployAction.getDeploymentUnitUniqueName();
        executeDeploymentPlan(plan, deployAction);
        url2Id.put(archiveURL, uniqueId);
    }

    @Override
    public void undeploy(final URL archiveURL) throws Exception {
        final DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
        final String uniqueName = url2Id.get(archiveURL);
        if (uniqueName != null) {
            final DeploymentPlan plan = builder.undeploy(uniqueName).remove(uniqueName).build();
            final DeploymentAction deployAction = builder.getLastAction();
            try {
                executeDeploymentPlan(plan, deployAction);
            } finally {
                url2Id.remove(archiveURL);
            }
        }
    }

    private void executeDeploymentPlan(final DeploymentPlan plan, final DeploymentAction deployAction) throws Exception {
        try {
            final ServerDeploymentPlanResult planResult = deploymentManager.execute(plan).get();

            if (deployAction != null) {
                final ServerDeploymentActionResult actionResult = planResult
                .getDeploymentActionResult(deployAction.getId());
                if (actionResult != null) {
                    final Exception deploymentException = (Exception) actionResult.getDeploymentException();
                    if (deploymentException != null)
                        throw deploymentException;
                }
            }
        } catch (final Exception e) {
            LOGGER.fatal(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void addSecurityDomain(String name, Map<String, String> authenticationOptions) throws Exception {
        final ModelControllerClient client = ModelControllerClient.Factory.create(address, PORT);
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, name);
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, name);
        op.get(OP_ADDR).add(AUTHENTICATION, CLASSIC);

        final ModelNode loginModule = op.get(LOGIN_MODULES).add();
        loginModule.get(CODE).set("UsersRoles");
        loginModule.get(FLAG).set("required");
        op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        updates.add(op);

        final ModelNode moduleOptions = loginModule.get(MODULE_OPTIONS);
        if (authenticationOptions != null) {
            for (final String k : authenticationOptions.keySet()) {
                moduleOptions.add(k, authenticationOptions.get(k));
            }
        }

        applyUpdates(updates, client);
    }

    @Override
    public void removeSecurityDomain(String name) throws Exception {
        final ModelControllerClient client = ModelControllerClient.Factory.create(address, PORT);
        final ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, name);
        // Don't rollback when the AS detects the war needs the module
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);

        applyUpdate(op, client);
    }

    private static void applyUpdates(final List<ModelNode> updates, final ModelControllerClient client) throws Exception {
        for (final ModelNode update : updates) {
            applyUpdate(update, client);
        }
    }

    private static void applyUpdate(final ModelNode update, final ModelControllerClient client) throws Exception {
        final ModelNode result = client.execute(new OperationBuilder(update).build());
        checkResult(result);
    }

    private static void checkResult(final ModelNode result) throws Exception {
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            if (result.hasDefined("result")) {
                LOGGER.info(result.get("result"));
            }
        } else if (result.hasDefined("failure-description")) {
            throw new Exception(result.get("failure-description").toString());
        } else {
            throw new Exception("Operation not successful; outcome = " + result.get("outcome"));
        }
    }

}
