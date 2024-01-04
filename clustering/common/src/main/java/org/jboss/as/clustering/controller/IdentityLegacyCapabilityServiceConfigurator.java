/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.Function;

import org.jboss.as.clustering.msc.InjectedValueDependency;
import org.jboss.as.clustering.msc.ValueDependency;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;

/**
 * Equivalent to {@link IdentityCapabilityServiceConfigurator}, but uses legacy service installation.
 * @author Paul Ferraro
 */
@Deprecated
public class IdentityLegacyCapabilityServiceConfigurator<T> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Service<T> {

    private final Function<CapabilityServiceSupport, ServiceName> requirementNameFactory;
    private final Class<T> targetClass;

    private volatile ValueDependency<T> requirement;

    public IdentityLegacyCapabilityServiceConfigurator(ServiceName name, Class<T> targetClass, ServiceNameFactory targetFactory) {
        this(name, targetClass, new Function<CapabilityServiceSupport, ServiceName>() {
            @Override
            public ServiceName apply(CapabilityServiceSupport support) {
                return targetFactory.getServiceName(support);
            }
        });
    }

    public IdentityLegacyCapabilityServiceConfigurator(ServiceName name, Class<T> targetClass, UnaryServiceNameFactory targetFactory, String requirementName) {
        this(name, targetClass, new Function<CapabilityServiceSupport, ServiceName>() {
            @Override
            public ServiceName apply(CapabilityServiceSupport support) {
                return targetFactory.getServiceName(support, requirementName);
            }
        });
    }

    public IdentityLegacyCapabilityServiceConfigurator(ServiceName name, Class<T> targetClass, BinaryServiceNameFactory targetFactory, String requirementParent, String requirementChild) {
        this(name, targetClass, new Function<CapabilityServiceSupport, ServiceName>() {
            @Override
            public ServiceName apply(CapabilityServiceSupport support) {
                return targetFactory.getServiceName(support, requirementParent, requirementChild);
            }
        });
    }

    private IdentityLegacyCapabilityServiceConfigurator(ServiceName name, Class<T> targetClass, Function<CapabilityServiceSupport, ServiceName> requirementNameFactory) {
        super(name);
        this.requirementNameFactory = requirementNameFactory;
        this.targetClass = targetClass;
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.requirement = new InjectedValueDependency<>(this.requirementNameFactory.apply(support), this.targetClass);
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        return this.requirement.register(builder).setInstance(this).setInitialMode(ServiceController.Mode.PASSIVE);
    }

    @Override
    public T getValue() {
        return this.requirement.get();
    }

    @Override
    public void start(StartContext context) {
        // Do nothing
    }

    @Override
    public void stop(StopContext context) {
        // Do nothing
    }
}
