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
package org.jboss.as.jsr88.spi;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.jsr88.spi.DeploymentLogger.ROOT_LOGGER;
import static org.jboss.as.jsr88.spi.DeploymentMessages.MESSAGES;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.TargetException;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.dmr.ModelNode;

/**
 * A Target that deploys using the {@link ServerDeploymentManager}.
 * <p/>
 * This target is selected by including a targetType=as7 param in the DeploymentManager deployURI.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 02-Aug-2011
 */
final class DeploymentManagerTarget extends JBossTarget {

    static final String DESCRIPTION = "ServerDeploymentManager target";

    private final Map<TargetModuleID, Boolean> runtimeState = new HashMap<TargetModuleID, Boolean>();
    private final ModelControllerClient modelControllerClient;
    private final ServerDeploymentManager deploymentManager;
    private final URI deployURI;

    public DeploymentManagerTarget(URI deployURI, final ModelControllerClient modelControllerClient) {
        ROOT_LOGGER.debugf("new DeploymentManagerTarget: %s", deployURI);
        this.modelControllerClient = modelControllerClient;
        this.deploymentManager = ServerDeploymentManager.Factory.create(modelControllerClient);
        this.deployURI = deployURI;
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
        ROOT_LOGGER.beginDeploy(targetModuleID);
        DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
        TargetModuleExt targetModule = (TargetModuleExt) targetModuleID;
        URL contentURL = targetModule.getContentFile().toURI().toURL();
        builder = builder.add(targetModule.getModuleID(), contentURL).andDeploy();
        DeploymentPlan plan = builder.build();
        DeploymentAction deployAction = builder.getLastAction();
        executeDeploymentPlan(plan, deployAction);
        ROOT_LOGGER.endDeploy(targetModuleID);
    }

    @Override
    public void start(TargetModuleID targetModuleID) throws Exception {
        // [TODO] A hack that fakes module start/stop behaviour
        // [AS7-2777] Add notion of start/stop for deployments
        ((TargetModuleIDImpl) targetModuleID).setRunning(Boolean.TRUE);
        runtimeState.put(targetModuleID, Boolean.TRUE);
    }

    @Override
    public void stop(TargetModuleID targetModuleID) throws Exception {
        // [TODO] A hack that fakes module start/stop behaviour
        // [AS7-2777] Add notion of start/stop for deployments
        ((TargetModuleIDImpl) targetModuleID).setRunning(Boolean.FALSE);
        runtimeState.put(targetModuleID, Boolean.FALSE);
    }

    @Override
    public void undeploy(TargetModuleID targetModuleID) throws Exception {
        String deploymentName = targetModuleID.getModuleID();
        DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
        DeploymentPlan plan = builder.undeploy(deploymentName).remove(deploymentName).build();
        Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
        future.get();
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
                throw new IllegalStateException(MESSAGES.managementRequestFailed(result));

            List<ModelNode> nodeList = result.get(RESULT).asList();
            for (ModelNode node : nodeList) {
                String moduleID = node.asString();
                ModuleType moduleType = null;
                if (moduleID.endsWith(".ear")) {
                    moduleType = ModuleType.EAR;
                } else if (moduleID.endsWith(".war")) {
                    moduleType = ModuleType.WAR;
                } else if (moduleID.endsWith(".rar")) {
                    moduleType = ModuleType.RAR;
                } else if (moduleID.endsWith(".jar")) {
                    // [TODO] not every jar is also an ejb jar
                    moduleType = ModuleType.EJB;
                }
                if (moduleType == null) {
                    ROOT_LOGGER.cannotDetermineModuleType(node);
                    continue;
                }
                if (filterType == null || filterType.equals(moduleType)) {
                    TargetModuleIDImpl targetModuleID = new TargetModuleIDImpl(this, moduleID, null, moduleType, null);
                    Boolean state = runtimeState.get(targetModuleID);
                    targetModuleID.setRunning(state != null ? state : Boolean.TRUE);
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

    private void executeDeploymentPlan(DeploymentPlan plan, DeploymentAction deployAction) throws Exception {
        Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
        ServerDeploymentPlanResult planResult = future.get();

        ServerDeploymentActionResult actionResult = planResult.getDeploymentActionResult(deployAction.getId());
        if (actionResult != null) {
            Exception deploymentException = (Exception) actionResult.getDeploymentException();
            if (deploymentException != null)
                throw deploymentException;
        }
    }

}
