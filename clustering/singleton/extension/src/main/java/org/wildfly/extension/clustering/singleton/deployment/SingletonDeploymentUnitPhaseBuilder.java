/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.singleton.deployment;

import org.jboss.as.server.deployment.DeploymentUnitPhaseBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.clustering.singleton.DeploymentPolicy;

/**
 * Builds a singleton service for the next phase in the deployment chain, if configured.
 * @author Paul Ferraro
 */
public class SingletonDeploymentUnitPhaseBuilder implements DeploymentUnitPhaseBuilder {

    private final DeploymentPolicy policy;

    public SingletonDeploymentUnitPhaseBuilder(DeploymentPolicy policy) {
        this.policy = policy;
    }

    @Override
    public <T> ServiceBuilder<T> build(ServiceTarget target, ServiceName name, Service<T> service) {

        return this.policy.getSingletonServiceBuilderFactory().createSingletonServiceBuilder(name, service)
                .electionPolicy(this.policy.getElectionPolicy())
                .requireQuorum(this.policy.getQuorum())
                .build(target).setInitialMode(ServiceController.Mode.ACTIVE);
    }
}
