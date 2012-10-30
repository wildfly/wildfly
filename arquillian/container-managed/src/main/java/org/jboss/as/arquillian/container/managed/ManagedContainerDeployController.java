/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
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
package org.jboss.as.arquillian.container.managed;

import java.util.concurrent.Callable;

import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.container.spi.event.container.AfterDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.container.spi.event.container.DeployerEvent;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.as.arquillian.container.CommonDeployableContainer;

public class ManagedContainerDeployController {
    @Inject
    private Instance<Injector> injector;

    public void deploy(@Observes final DeployManagedDeployment event) throws Exception {
        executeOperation(new Callable<Void>() {
            @Inject
            private Event<DeployerEvent> deployEvent;

            @Inject
            @DeploymentScoped
            private InstanceProducer<DeploymentDescription> deploymentDescriptionProducer;

            @Inject
            @DeploymentScoped
            private InstanceProducer<Deployment> deploymentProducer;

            @Inject
            @DeploymentScoped
            private InstanceProducer<ProtocolMetaData> protocolMetadata;

            @Override
            public Void call() throws Exception {
                CommonDeployableContainer<?> deployableContainer = event.getDeployableContainer();
                Deployment deployment = event.getDeployment();
                DeploymentDescription deploymentDescription = deployment.getDescription();
                String policy = event.getDeploymentPolicy();
                /*
                 * TODO: should the DeploymentDescription producer some how be automatically registered ? Or should we just
                 * 'know' who is the first one to create the context
                 */
                deploymentDescriptionProducer.set(deploymentDescription);
                deploymentProducer.set(deployment);

                deployEvent.fire(new BeforeDeploy(deployableContainer, deploymentDescription));

                try {
                    if (deploymentDescription.isArchiveDeployment()) {
                        protocolMetadata
                                .set(deployableContainer.deploy(deploymentDescription.getTestableArchive() != null ? deploymentDescription
                                        .getTestableArchive() : deploymentDescription.getArchive(), policy));
                    } else {
                        deployableContainer.deploy(deploymentDescription.getDescriptor());
                    }
                    deployment.deployed();
                } catch (Exception e) {
                    deployment.deployedWithError(e);
                    throw e;
                }

                deployEvent.fire(new AfterDeploy(deployableContainer, deploymentDescription));
                return null;
            }
        });
    }

    private void executeOperation(Callable<Void> operation) throws Exception {
        injector.get().inject(operation);
        operation.call();
    }
}
