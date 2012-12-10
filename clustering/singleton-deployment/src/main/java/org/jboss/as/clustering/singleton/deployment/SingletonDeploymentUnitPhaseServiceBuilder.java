/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.singleton.deployment;

import org.jboss.as.clustering.singleton.SingletonElectionPolicy;
import org.jboss.as.clustering.singleton.SingletonService;
import org.jboss.as.server.deployment.DeploymentUnitPhaseServiceBuilder;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.Services;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author paul
 *
 */
public class SingletonDeploymentUnitPhaseServiceBuilder extends AbstractService<DeploymentUnitPhaseServiceBuilder> implements DeploymentUnitPhaseServiceBuilder {

    private final String name;
    private final String cluster;
    private final SingletonElectionPolicy electionPolicy;

    public ServiceBuilder<DeploymentUnitPhaseServiceBuilder> build(ServiceTarget target) {
        return target.addService(Services.deploymentPolicyName(this.name), this);
    }

    public SingletonDeploymentUnitPhaseServiceBuilder(String name, String cluster, SingletonElectionPolicy electionPolicy) {
        this.name = name;
        this.cluster = cluster;
        this.electionPolicy = electionPolicy;
    }

    @Override
    public String getDeploymentPolicy() {
        return this.name;
    }

    @Override
    public DeploymentUnitPhaseServiceBuilder getValue() throws IllegalStateException {
        return this;
    }

    @Override
    public <T> ServiceBuilder<T> build(ServiceTarget target, ServiceName name, Service<T> service, Phase phase) {
        return (phase == Phase.FIRST_MODULE_USE) ? this.createSingletonService(name, service).build(target, this.cluster) : target.addService(name, service);
    }

    private <T> SingletonService<T> createSingletonService(ServiceName name, Service<T> service) {
        SingletonService<T> singleton = new SingletonService<T>(service, name);
        singleton.setElectionPolicy(this.electionPolicy);
        return singleton;
    }
}
