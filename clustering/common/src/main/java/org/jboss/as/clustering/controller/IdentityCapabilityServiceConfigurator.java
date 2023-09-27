/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.service.ServiceSupplierDependency;

/**
 * Similar to {@link org.wildfly.clustering.service.IdentityServiceConfigurator}, but resolves the {@link ServiceName} of the requirement during {@link #configure(CapabilityServiceSupport)}.
 * @author Paul Ferraro
 */
public class IdentityCapabilityServiceConfigurator<T> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator {

    private final Function<CapabilityServiceSupport, ServiceName> requirementNameFactory;

    private volatile SupplierDependency<T> requirement;

    public IdentityCapabilityServiceConfigurator(ServiceName name, ServiceNameFactory targetFactory) {
        this(name, new Function<CapabilityServiceSupport, ServiceName>() {
            @Override
            public ServiceName apply(CapabilityServiceSupport support) {
                return targetFactory.getServiceName(support);
            }
        });
    }

    public IdentityCapabilityServiceConfigurator(ServiceName name, UnaryServiceNameFactory targetFactory, String requirementName) {
        this(name, new Function<CapabilityServiceSupport, ServiceName>() {
            @Override
            public ServiceName apply(CapabilityServiceSupport support) {
                return targetFactory.getServiceName(support, requirementName);
            }
        });
    }

    public IdentityCapabilityServiceConfigurator(ServiceName name, BinaryServiceNameFactory targetFactory, String requirementParent, String requirementChild) {
        this(name, new Function<CapabilityServiceSupport, ServiceName>() {
            @Override
            public ServiceName apply(CapabilityServiceSupport support) {
                return targetFactory.getServiceName(support, requirementParent, requirementChild);
            }
        });
    }

    private IdentityCapabilityServiceConfigurator(ServiceName name, Function<CapabilityServiceSupport, ServiceName> requirementNameFactory) {
        super(name);
        this.requirementNameFactory = requirementNameFactory;
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.requirement = new ServiceSupplierDependency<>(this.requirementNameFactory.apply(support));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<T> consumer = this.requirement.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(consumer, Function.identity(), this.requirement);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.PASSIVE);
    }
}
