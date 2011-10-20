/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.appclient.service;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.deployment.DeploymentAddHandler;
import org.jboss.as.server.deployment.DeploymentDeployHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for deploying the application client that was specified on the command line
 *
 * @author Stuart Douglas
 */
public class ApplicationClientDeploymentService implements Service<ApplicationClientDeploymentService> {


    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("appClientDeploymentService");

    private final File path;
    private ModelControllerClient controllerClient;
    private final InjectedValue<ModelController> controllerValue = new InjectedValue<ModelController>();
    private final CountDownLatch deploymentCompleteLatch = new CountDownLatch(1);


    public ApplicationClientDeploymentService(final File path) {
        this.path = path;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        controllerClient = controllerValue.getValue().createClient(Executors.newSingleThreadExecutor());

        final DeployTask task = new DeployTask();
        Thread thread = new Thread(new DeploymentTask(new OperationBuilder(task.getUpdate()).build()));
        thread.start();
    }

    @Override
    public synchronized void stop(final StopContext context) {
        //TODO: undeploy
    }

    @Override
    public ApplicationClientDeploymentService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }


    private final class DeployTask {

        protected ModelNode getUpdate() {
            final ModelNode address = new ModelNode().add(DEPLOYMENT, path.getName());
            final ModelNode addOp = Util.getEmptyOperation(DeploymentAddHandler.OPERATION_NAME, address);
            addOp.get(CONTENT).set(createContent());
            addOp.get(PERSISTENT).set(false);
            final ModelNode deployOp = Util.getEmptyOperation(DeploymentDeployHandler.OPERATION_NAME, address);
            return getCompositeUpdate(addOp, deployOp);
        }


        protected ModelNode createContent() {
            final ModelNode content = new ModelNode();
            final ModelNode contentItem = content.get(0);
            contentItem.get(PATH).set(path.getAbsolutePath());
            contentItem.get(ARCHIVE).set(!path.isDirectory());
            return content;
        }

        private ModelNode getCompositeUpdate(final ModelNode... updates) {
            final ModelNode op = Util.getEmptyOperation(COMPOSITE, new ModelNode());
            final ModelNode steps = op.get(STEPS);
            for (ModelNode update : updates) {
                steps.add(update);
            }
            return op;
        }
    }

    private class DeploymentTask implements Runnable {
        private final Operation deploymentOp;


        private DeploymentTask(final Operation deploymentOp) {
            this.deploymentOp = deploymentOp;
        }

        @Override
        public void run() {
            try {
                ModelNode result = controllerClient.execute(deploymentOp);
                if (!SUCCESS.equals(result.get(OUTCOME).asString())) {
                    System.exit(1);
                }
                deploymentCompleteLatch.countDown();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public InjectedValue<ModelController> getControllerValue() {
        return controllerValue;
    }

    public CountDownLatch getDeploymentCompleteLatch() {
        return deploymentCompleteLatch;
    }
}
