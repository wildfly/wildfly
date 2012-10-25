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

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.Container.State;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentScenario;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentTargetDescription;
import org.jboss.arquillian.container.spi.event.DeploymentEvent;
import org.jboss.arquillian.container.test.impl.client.deployment.ClientDeployer;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

public class ManagedClientDeployer extends ClientDeployer implements ManagedDeployer {
    @Inject
    private Event<DeploymentEvent> event;

    @Inject
    private Instance<ContainerRegistry> containerRegistry;

    @Inject
    private Instance<DeploymentScenario> deploymentScenario;

    @Override
    public void deploy(String name, String policy) {
        DeploymentScenario scenario = deploymentScenario.get();
        if (scenario == null) {
            throw new IllegalArgumentException("No deployment scenario in context");
        }
        ContainerRegistry registry = containerRegistry.get();
        if (registry == null) {
            throw new IllegalArgumentException("No container registry in context");
        }

        Deployment deployment = scenario.deployment(new DeploymentTargetDescription(name));
        if (deployment == null) {
            throw new IllegalArgumentException("No deployment in context found with name " + name);
        }

        if (deployment.getDescription().managed()) {
            throw new IllegalArgumentException("Could not deploy " + name
                    + " deployment. The deployment is controlled by Arquillian");
        }

        Container container = registry.getContainer(deployment.getDescription().getTarget());

        if (!container.getState().equals(State.STARTED)) {
            throw new IllegalArgumentException("Deployment with name " + name + " could not be deployed. Container "
                    + container.getName() + " must be started first.");
        }

        event.fire(new DeployManagedDeployment(container, deployment, policy));
    }
}
