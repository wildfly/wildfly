/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.clustering;

import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.CLUSTERED_SINGLETON_CAPABILITY;

import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.singleton.service.SingletonPolicy;

/**
 * Service that installs a singleton service on service start using a singleton policy.
 *
 * @author Flavia Rainone
 */
public class SingletonBarrierService implements Service {
    public static final ServiceName SERVICE_NAME = CLUSTERED_SINGLETON_CAPABILITY.getCapabilityServiceName().append("barrier");

    private final Supplier<SingletonPolicy> policy;

    public SingletonBarrierService(Supplier<SingletonPolicy> policy) {
        this.policy = policy;
    }

    @Override
    public void start(StartContext context) {
        this.policy.get().createSingletonServiceConfigurator(CLUSTERED_SINGLETON_CAPABILITY.getCapabilityServiceName())
                .build(context.getChildTarget())
                .install();
    }

    @Override
    public void stop(StopContext context) {
    }
}