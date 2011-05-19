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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.AUTHENTICATION;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
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
 */
public final class RemoteDeployer implements Deployer {

    private static final Logger LOGGER = Logger.getLogger(RemoteDeployer.class);
    private static final String JBOSSWS_SEC_DOMAIN = "JBossWS";
    private final Map<URL, String> url2Id = new HashMap<URL, String>();

    private ServerDeploymentManager deploymentManager;

    public RemoteDeployer() throws IOException {
        final InetAddress address = InetAddress.getByName("127.0.0.1");
        deploymentManager = ServerDeploymentManager.Factory.create(address, 9999);
        ModelControllerClient client = ModelControllerClient.Factory.create(address, 9999);
        removeJBossWSSecurityDomain(client);
        createJBossWSSecurityDomain(client);
    }

    @Override
    public void deploy(final URL archiveURL) throws Exception {
        final DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan().withRollback().add(archiveURL).andDeploy();
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

    private void createJBossWSSecurityDomain(ModelControllerClient client) throws IOException {
        String usersPropFile = System.getProperty("org.jboss.ws.testsuite.securityDomain.users.propfile");
        String rolesPropFile = System.getProperty("org.jboss.ws.testsuite.securityDomain.roles.propfile");
        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, JBOSSWS_SEC_DOMAIN);
        ModelNode loginModule = op.get(AUTHENTICATION).add();
        loginModule.get(CODE).set("UsersRoles");
        loginModule.get(FLAG).set("required");
        ModelNode moduleOptions = loginModule.get(MODULE_OPTIONS);
        if (usersPropFile != null) {
            moduleOptions.add("usersProperties", usersPropFile);
        }
        if (rolesPropFile != null) {
            moduleOptions.add("rolesProperties", rolesPropFile);
        }
        moduleOptions.add("unauthenticatedIdentity", "anonymous");
        applyUpdate(op, client);
    }

    private void removeJBossWSSecurityDomain(ModelControllerClient client) {
        try {
            ModelNode op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "security");
            op.get(OP_ADDR).add(SECURITY_DOMAIN, JBOSSWS_SEC_DOMAIN);
            applyUpdate(op, client);
        } catch (Throwable e) {
            LOGGER.debug("Could not remove '" + JBOSSWS_SEC_DOMAIN + "' security domain from target container", e);
        }
    }

    private static void applyUpdate(ModelNode update, final ModelControllerClient client) throws IOException {
        ModelNode result = client.execute(OperationBuilder.Factory.create(update).build());
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            if (result.hasDefined("result")) {
                System.out.println(result.get("result"));
            }
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
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

}
