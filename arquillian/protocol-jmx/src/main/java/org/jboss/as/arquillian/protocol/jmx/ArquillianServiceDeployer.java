/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.protocol.jmx;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEPLOY;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeStop;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;
import org.jboss.as.arquillian.protocol.jmx.JMXProtocolAS7.ServiceArchiveHolder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;

/**
 * A deployer for the Arquillian JMXProtocol endpoint.
 *
 * @see JMXProtocolPackager
 *
 * @author thomas.diesler@jboss.com
 * @since 31-May-2011
 */
public class ArquillianServiceDeployer {

    private static final Logger log = Logger.getLogger(ArquillianServiceDeployer.class);

    @Inject
    @SuiteScoped
    private Instance<ServiceArchiveHolder> archiveHolderInst;

    @Inject
    @ContainerScoped
    private Instance<Container> containerInst;

    private AtomicBoolean serviceArchiveDeployed = new AtomicBoolean();

    public synchronized void doServiceDeploy(@Observes BeforeDeploy event) {
        ServiceArchiveHolder archiveHolder = archiveHolderInst.get();
        if (archiveHolder != null && serviceArchiveDeployed.get() == false) {
            Archive<?> archive = archiveHolder.getArchive();

            // [AS7-972] arquillian-service not getting undeployed
            if (isArquillianServiceDeployed(archive))
                undeployArquillianService(archive);

            try {
                log.infof("Deploy arquillian service: %s", archive);
                DeployableContainer<?> deployableContainer = containerInst.get().getDeployableContainer();
                deployableContainer.deploy(archive);
                serviceArchiveDeployed.set(true);
            } catch (Throwable th) {
                log.error("Cannot deploy arquillian service", th);
            }
        }
    }

    public synchronized void undeploy(@Observes BeforeStop event) {
        ServiceArchiveHolder archiveHolder = archiveHolderInst.get();
        if (archiveHolder != null && serviceArchiveDeployed.get() == true) {
            try {
                Archive<?> archive = archiveHolder.getArchive();
                log.infof("Undeploy arquillian service: %s", archive);
                DeployableContainer<?> deployableContainer = containerInst.get().getDeployableContainer();
                deployableContainer.undeploy(archive);
                serviceArchiveDeployed.set(false);
            } catch (Throwable th) {
                log.error("Cannot undeploy arquillian service", th);
            }
        }
    }

    private boolean isArquillianServiceDeployed(final Archive<?> archive) {
        try {
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
            operation.get(CHILD_TYPE).set(DEPLOYMENT);
            ModelNode result = getModelControllerClient().execute(OperationBuilder.Factory.create(operation).build());
            if (FAILED.equals(result.get(OUTCOME).asString()))
                throw new IllegalStateException("Management request failed: " + result);

            boolean serviceFound = false;
            List<ModelNode> nodeList = result.get(RESULT).asList();
            for (ModelNode node : nodeList) {
                if (node.asString().equals(archive.getName())) {
                    log.infof("Found already deployed arquillian service: %s", node);
                    serviceFound = true;
                    break;
                }
            }
            return serviceFound;
        } catch (Exception ex) {
            log.errorf(ex, "Cannot determine whether arquillian service is deployed");
            return false;
        }
    }

    private void undeployArquillianService(final Archive<?> archive) {
        try {
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(COMPOSITE);
            operation.get(ADDRESS).setEmptyList();
            ModelNode steps = operation.get(STEPS);
            ModelNode undeployNode = new ModelNode();
            undeployNode.get(OP).set(UNDEPLOY);
            undeployNode.get(ADDRESS).set(DEPLOYMENT, archive.getName());
            steps.add(undeployNode);
            ModelNode removeNode = new ModelNode();
            removeNode.get(OP).set(REMOVE);
            removeNode.get(ADDRESS).set(DEPLOYMENT, archive.getName());
            steps.add(removeNode);

            log.infof("Undeploying arquillian service  with: %s", operation);
            ModelNode result = getModelControllerClient().execute(OperationBuilder.Factory.create(operation).build());
            if (FAILED.equals(result.get(OUTCOME).asString()))
                log.errorf("Management request failed: %s", result);

        } catch (Exception ex) {
            log.errorf(ex, "Cannot undeploy arquillian service");
        }
    }

    private ModelControllerClient getModelControllerClient() throws UnknownHostException {
        // TODO: make configurable via protocol config
        return ModelControllerClient.Factory.create(InetAddress.getByName("127.0.0.1"), 9999);
    }
}
