/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton.deployment;

import java.util.function.UnaryOperator;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.DelegatingServiceTarget;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.singleton.service.ServiceTargetFactory;

/**
 * @author Paul Ferraro
 */
public class SingletonDeploymentServiceTargetTransformer implements UnaryOperator<ServiceTarget> {
    private static final String EJB_REMOTE_CAPABILITY = "org.wildfly.ejb.remote";

    private final CapabilityServiceSupport support;
    private final ServiceTargetFactory targetFactory;

    public SingletonDeploymentServiceTargetTransformer(CapabilityServiceSupport support, ServiceTargetFactory targetFactory) {
        this.support = support;
        this.targetFactory = targetFactory;
    }

    @Override
    public ServiceTarget apply(ServiceTarget target) {
        ServiceTarget singletonTarget = this.targetFactory.createSingletonServiceTarget(target);
        if (this.support.hasCapability(EJB_REMOTE_CAPABILITY)) {
            ServiceName requirement = this.support.getCapabilityServiceName(EJB_REMOTE_CAPABILITY);
            return new DelegatingServiceTarget(singletonTarget) {
                @Override
                public ServiceBuilder<?> addService() {
                    return this.addRequirement(super.addService());
                }

                @Deprecated
                @Override
                public ServiceBuilder<?> addService(ServiceName name) {
                    return this.addRequirement(super.addService(name));
                }

                @Deprecated
                @Override
                public <T> ServiceBuilder<T> addService(ServiceName name, org.jboss.msc.service.Service<T> service) {
                    return this.addRequirement(super.addService(name, service));
                }

                private <T> ServiceBuilder<T> addRequirement(ServiceBuilder<T> builder) {
                    builder.requires(requirement);
                    return builder.setInitialMode(ServiceController.Mode.ACTIVE);
                }
            };
        }
        return singletonTarget;
    }
}
