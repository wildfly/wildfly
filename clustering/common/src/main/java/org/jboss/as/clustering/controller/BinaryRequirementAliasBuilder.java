/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Similar to {@link org.wildfly.clustering.service.AliasServiceBuilder} but resolves {@link ServiceName} of dependent requirement during {@link #configure(CapabilityServiceSupport)}.
 * @author Paul Ferraro
 */
public class BinaryRequirementAliasBuilder<T> implements CapabilityServiceBuilder<T> {

    private final ServiceName name;
    private final BinaryServiceNameFactory targetFactory;
    private final String targetParent;
    private final String targetChild;
    private final Class<T> targetClass;

    private volatile ValueDependency<T> dependency;

    public BinaryRequirementAliasBuilder(ServiceName name, BinaryServiceNameFactory targetFactory, String targetParent, String targetChild, Class<T> targetClass) {
        this.name = name;
        this.targetFactory = targetFactory;
        this.targetParent = targetParent;
        this.targetChild = targetChild;
        this.targetClass = targetClass;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public Builder<T> configure(CapabilityServiceSupport support) {
        this.dependency = new InjectedValueDependency<>(this.targetFactory.getServiceName(support, this.targetParent, this.targetChild), this.targetClass);
        return this;
    }

    @Override
    public ServiceBuilder<T> build(ServiceTarget target) {
        return this.dependency.register(target.addService(this.name, new ValueService<>(this.dependency)).setInitialMode(ServiceController.Mode.PASSIVE));
    }
}
