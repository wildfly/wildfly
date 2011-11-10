/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ee.deployment.spi;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.TargetException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * A Target that deploys using the {@link ServerDeploymentManager}.
 *
 * This target is selected by including a targetType=as7 param in the DeploymentManager deployURI.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 02-Aug-2011
 */
final class DeploymentManagerTarget extends JBossTarget {

    // provide logging
    static final Logger log = Logger.getLogger(DeploymentManagerTarget.class);

    static final String DESCRIPTION = "ServerDeploymentManager target";

    private final Map<TargetModuleID, String> runtimeNames = new HashMap<TargetModuleID, String>();
    private final ModelControllerClient modelControllerClient;
    private final ServerDeploymentManager deploymentManager;
    private final URI deployURI;

    public DeploymentManagerTarget(URI deployURI, String username, String password) {
        log.debug("new DeploymentManagerTarget: " + deployURI);
        try {
            URIParser parser = new URIParser(deployURI);
            String serverHost = parser.getParameter("serverHost");
            String serverPort = parser.getParameter("serverPort");
            String host = serverHost != null ? serverHost : "127.0.0.1";
            Integer port = serverPort != null ? Integer.parseInt(serverPort) : 9999;
            if (username != null && password != null) {
                this.modelControllerClient = ModelControllerClient.Factory.create(host, port, getCallbackHandler(username, password));
            } else {
                this.modelControllerClient = ModelControllerClient.Factory.create(host, port);
            }
            this.deploymentManager = ServerDeploymentManager.Factory.create(modelControllerClient);
            this.deployURI = deployURI;
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("Cannot connect to management target: " + deployURI, ex);
        }
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getName() {
        return deployURI.toString();
    }

    @Override
    public void deploy(TargetModuleID targetModuleID) throws Exception {
        log.infof("Begin deploy: %s", targetModuleID);
        DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
        builder = builder.add(targetModuleID.getModuleID(), new URL(targetModuleID.getModuleID())).andDeploy();
        DeploymentPlan plan = builder.build();
        DeploymentAction deployAction = builder.getLastAction();
        String runtimeName = executeDeploymentPlan(plan, deployAction);
        runtimeNames.put(targetModuleID, runtimeName);
        log.infof("End deploy: %s", targetModuleID);
    }

    @Override
    public void start(TargetModuleID targetModuleID) throws Exception {
        // do nothing
    }

    @Override
    public void stop(TargetModuleID targetModuleID) throws Exception {
        // do nothing
    }

    @Override
    public void undeploy(TargetModuleID targetModuleID) throws Exception {
        String runtimeName = runtimeNames.remove(targetModuleID);
        if (runtimeName != null) {
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            DeploymentPlan plan = builder.undeploy(runtimeName).remove(runtimeName).build();
            Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
            future.get();
        }
    }

    @Override
    public TargetModuleID[] getAvailableModules(ModuleType filterType) throws TargetException {
        try {
            List<TargetModuleID> list = new ArrayList<TargetModuleID>();

            final ModelNode operation = new ModelNode();
            operation.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
            operation.get(CHILD_TYPE).set(DEPLOYMENT);
            ModelNode result = modelControllerClient.execute(operation);
            if (FAILED.equals(result.get(OUTCOME).asString()))
                throw new IllegalStateException("Management request failed: " + result);

            List<ModelNode> nodeList = result.get(RESULT).asList();
            for (ModelNode node : nodeList) {
                String moduleID = node.asString();
                ModuleType moduleType = null;
                if (moduleID.endsWith(".ear")) {
                    moduleType = ModuleType.EAR;
                } else if (moduleID.endsWith(".war")) {
                    moduleType = ModuleType.WAR;
                } else if (moduleID.endsWith(".jar")) {
                    // [TODO] not every jar is also an ejb jar
                    moduleType = ModuleType.EJB;
                }
                if (moduleType == null) {
                    log.warnf("Cannot determine module type of: %s", node);
                    continue;
                }
                if (filterType == null || filterType.equals(moduleType)) {
                    TargetModuleIDImpl targetModuleID = new TargetModuleIDImpl(this, moduleID, null, moduleType);
                    targetModuleID.setRunning(true);
                    list.add(targetModuleID);
                }
            }
            TargetModuleID[] targetModuleIDs = new TargetModuleID[list.size()];
            list.toArray(targetModuleIDs);
            return targetModuleIDs;
        } catch (Exception e) {
            TargetException tex = new TargetException("Failed to get available modules");
            tex.initCause(e);
            throw tex;
        }
    }

    private String executeDeploymentPlan(DeploymentPlan plan, DeploymentAction deployAction) throws Exception {
        Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
        ServerDeploymentPlanResult planResult = future.get();

        ServerDeploymentActionResult actionResult = planResult.getDeploymentActionResult(deployAction.getId());
        if (actionResult != null) {
            Exception deploymentException = (Exception) actionResult.getDeploymentException();
            if (deploymentException != null)
                throw deploymentException;
        }

        return deployAction.getDeploymentUnitUniqueName();
    }

    private CallbackHandler getCallbackHandler(final String username, final String password) {
        return new CallbackHandler() {
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback current : callbacks) {
                    if (current instanceof NameCallback) {
                        NameCallback ncb = (NameCallback) current;
                        ncb.setName(username);
                    } else if (current instanceof PasswordCallback) {
                        PasswordCallback pcb = (PasswordCallback) current;
                        pcb.setPassword(password.toCharArray());
                    } else if (current instanceof RealmCallback) {
                        RealmCallback rcb = (RealmCallback) current;
                        rcb.setText(rcb.getDefaultText());
                    } else if (current instanceof RealmChoiceCallback) {
                        // Ignored but not rejected.
                    } else {
                        throw new UnsupportedCallbackException(current);
                    }

                }
            }
        };
    }
}
