/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton.deployment;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.DeploymentUnitPhaseBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.singleton.SingletonPolicy;

/**
 * Builds a singleton service for the next phase in the deployment chain, if configured.
 * @author Paul Ferraro
 */
@SuppressWarnings({ "removal", "deprecation" })
public class SingletonDeploymentUnitPhaseBuilder implements DeploymentUnitPhaseBuilder {
    private static final String EJB_REMOTE_CAPABILITY = "org.wildfly.ejb.remote";

    private final CapabilityServiceSupport support;
    private final SingletonPolicy policy;

    public SingletonDeploymentUnitPhaseBuilder(CapabilityServiceSupport support, SingletonPolicy policy) {
        this.support = support;
        this.policy = policy;
    }

    @Override
    public <T> ServiceBuilder<T> build(ServiceTarget target, ServiceName name, Service<T> service) {
        ServiceBuilder<T> builder = this.policy.createSingletonServiceBuilder(name, service).build(target).setInitialMode(ServiceController.Mode.ACTIVE);
        if (this.support.hasCapability(EJB_REMOTE_CAPABILITY)) {
            builder.requires(this.support.getCapabilityServiceName(EJB_REMOTE_CAPABILITY));
        }
        return builder;
    }
}
