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
    private final ServiceName targetFactoryServiceName;

    /**
     * Creates a transformer of the service target of the initial deployment phase.
     * @param support the capability service support
     * @param targetFactory the singleton service target factory of the singleton policy
     * @param targetFactoryServiceName the name of the service providing the singleton service target factory, i.e. the singleton policy
     */
    public SingletonDeploymentServiceTargetTransformer(CapabilityServiceSupport support, ServiceTargetFactory targetFactory, ServiceName targetFactoryServiceName) {
        this.support = support;
        this.targetFactory = targetFactory;
        this.targetFactoryServiceName = targetFactoryServiceName;
    }

    @Override
    public ServiceTarget apply(ServiceTarget target) {
        ServiceTarget singletonTarget = this.targetFactory.createSingletonServiceTarget(target);
        ServiceName targetFactoryServiceName = this.targetFactoryServiceName;
        ServiceName ejbRemoteServiceName = this.support.hasCapability(EJB_REMOTE_CAPABILITY) ? this.support.getCapabilityServiceName(EJB_REMOTE_CAPABILITY) : null;
        return new DelegatingServiceTarget(singletonTarget) {
            @Override
            public ServiceBuilder<?> addService() {
                return this.addRequirements(super.addService());
            }

            @Deprecated
            @Override
            public ServiceBuilder<?> addService(ServiceName name) {
                return this.addRequirements(super.addService(name));
            }

            @Deprecated
            @Override
            public <T> ServiceBuilder<T> addService(ServiceName name, org.jboss.msc.service.Service<T> service) {
                return this.addRequirements(super.addService(name, service));
            }

            private <T> ServiceBuilder<T> addRequirements(ServiceBuilder<T> builder) {
                // Keep the singleton policy demanded while the singleton deployment is installed.
                // Otherwise, this ON_DEMAND service stops when the deployment restarts and is still DOWN when the restarted deployment phases require it again,
                // which the deployment operation can report as an unresolved problem if the singleton election triggers these phases during its verification.
                builder.requires(targetFactoryServiceName);
                if (ejbRemoteServiceName != null) {
                    builder.requires(ejbRemoteServiceName);
                    builder.setInitialMode(ServiceController.Mode.ACTIVE);
                }
                return builder;
            }
        };
    }
}
