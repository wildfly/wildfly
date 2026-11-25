/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.appclient.service;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.deployment.DeploymentAddHandler;
import org.jboss.as.server.deployment.DeploymentDeployHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * Service responsible for deploying the application client that was specified on the command line
 *
 * @author Stuart Douglas
 */
public class ApplicationClientDeploymentService implements Service {


    private final File path;
    private final Consumer<ApplicationClientDeploymentService> consumer;
    private final Supplier<ModelControllerClientFactory> clientFactorySupplier;
    private final Supplier<Executor> executorSupplier;
    private final CountDownLatch deploymentCompleteLatch = new CountDownLatch(1);


    public ApplicationClientDeploymentService(final Consumer<ApplicationClientDeploymentService> consumer,
                                              final File path,
                                              final Supplier<ModelControllerClientFactory> clientFactorySupplier,
                                              final Supplier<Executor> executorSupplier) {
        this.consumer = consumer;
        this.path = path;
        this.clientFactorySupplier = clientFactorySupplier;
        this.executorSupplier = executorSupplier;
    }

    @Override
    public synchronized void start(final StartContext context) {
        final DeployTask task = new DeployTask();
        // TODO use executorServiceSupplier
        Thread thread = new Thread(new DeploymentTask(new OperationBuilder(task.getUpdate()).build()));
        thread.start();
        consumer.accept(this);
    }

    @Override
    public synchronized void stop(final StopContext context) {
        //TODO: undeploy
        consumer.accept(null);
    }

    private final class DeployTask {

        ModelNode getUpdate() {
            final ModelNode address = new ModelNode().add(DEPLOYMENT, path.getName());
            final ModelNode addOp = Util.getEmptyOperation(DeploymentAddHandler.OPERATION_NAME, address);
            addOp.get(CONTENT).set(createContent());
            final ModelNode deployOp = Util.getEmptyOperation(DeploymentDeployHandler.OPERATION_NAME, address);
            return getCompositeUpdate(addOp, deployOp);
        }


        ModelNode createContent() {
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
            try (LocalModelControllerClient controllerClient = clientFactorySupplier.get().createSuperUserClient(executorSupplier.get())) {
                ModelNode result = controllerClient.execute(deploymentOp);
                if (!SUCCESS.equals(result.get(OUTCOME).asString())) {
                    System.exit(1);
                }
                deploymentCompleteLatch.countDown();
            }
        }

    }

    public CountDownLatch getDeploymentCompleteLatch() {
        return deploymentCompleteLatch;
    }
}
