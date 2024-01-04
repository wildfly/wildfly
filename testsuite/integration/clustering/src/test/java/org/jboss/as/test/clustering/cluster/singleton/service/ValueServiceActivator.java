/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.singleton.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.service.ChildTargetService;
import org.wildfly.clustering.singleton.SingletonDefaultRequirement;
import org.wildfly.clustering.singleton.SingletonPolicy;

/**
 * @author Paul Ferraro
 */
@SuppressWarnings("deprecation")
public class ValueServiceActivator implements ServiceActivator {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("test", "service", "value");

    @Override
    public void activate(ServiceActivatorContext context) {
        ServiceBuilder<?> builder = context.getServiceTarget().addService(ServiceName.JBOSS.append("test", "service", "installer"));
        Supplier<SingletonPolicy> policy = builder.requires(ServiceName.parse(SingletonDefaultRequirement.SINGLETON_POLICY.getName()));
        Consumer<ServiceTarget> installer = target -> policy.get().createSingletonServiceBuilder(SERVICE_NAME, new ValueService<>(Boolean.TRUE), new ValueService<>(Boolean.FALSE)).build(context.getServiceTarget()).install();
        builder.setInstance(new ChildTargetService(installer)).install();
    }

    private static final class ValueService<T> implements Service<T> {
        private final T value;

        ValueService(T value) {
            this.value = value;
        }

        @Override
        public void start(final StartContext context) {
            // noop
        }

        @Override
        public void stop(final StopContext context) {
            // noop
        }

        @Override
        public T getValue() {
            return this.value;
        }
    }
}
