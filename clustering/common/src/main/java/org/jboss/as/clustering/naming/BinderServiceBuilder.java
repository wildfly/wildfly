/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.naming;

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.Builder;

/**
 * Builds a ManagedReferenceFactory JNDI binding.
 * @author Paul Ferraro
 */
public class BinderServiceBuilder<T> implements Builder<ManagedReferenceFactory> {

    private final ContextNames.BindInfo binding;
    private final ServiceName targetServiceName;
    private final Class<T> targetClass;
    private final List<ContextNames.BindInfo> aliases = new LinkedList<>();

    public BinderServiceBuilder(ContextNames.BindInfo binding, ServiceName targetServiceName, Class<T> targetClass) {
        this.binding = binding;
        this.targetServiceName = targetServiceName;
        this.targetClass = targetClass;
    }

    public BinderServiceBuilder<T> alias(ContextNames.BindInfo alias) {
        this.aliases.add(alias);
        return this;
    }

    @Override
    public ServiceName getServiceName() {
        return this.binding.getBinderServiceName();
    }

    @Override
    public ServiceBuilder<ManagedReferenceFactory> build(ServiceTarget target) {
        String name = this.binding.getBindName();
        BinderService binder = new BinderService(name);
        ServiceBuilder<ManagedReferenceFactory> builder = target.addService(this.getServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(name))
                .addDependency(this.targetServiceName, this.targetClass, new ManagedReferenceInjector<T>(binder.getManagedObjectInjector()))
                .addDependency(this.binding.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
        ;
        for (ContextNames.BindInfo alias : this.aliases) {
            builder.addAliases(alias.getBinderServiceName(), ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(alias.getBindName()));
        }
        return builder.setInitialMode(ServiceController.Mode.PASSIVE);
    }
}
