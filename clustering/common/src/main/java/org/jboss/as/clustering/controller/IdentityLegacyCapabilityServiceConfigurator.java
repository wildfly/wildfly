/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.util.function.Function;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Equivalent to {@link IdentityCapabilityServiceConfigurator}, but uses legacy service installation.
 * @author Paul Ferraro
 */
@Deprecated
public class IdentityLegacyCapabilityServiceConfigurator<T> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator {

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
        ServiceBuilder<?> builder = target.addService(this.getServiceName(), new ValueService<>(this.requirement));
        return this.requirement.register(builder).setInitialMode(ServiceController.Mode.PASSIVE);
    }
}
