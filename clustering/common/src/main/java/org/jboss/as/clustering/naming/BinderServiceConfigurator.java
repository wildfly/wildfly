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

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.CommonRequirement;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;

/**
 * Configures a service providing a ManagedReferenceFactory JNDI binding.
 * @author Paul Ferraro
 */
public class BinderServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator {

    private final ContextNames.BindInfo binding;
    private final ServiceName targetServiceName;
    private final List<ContextNames.BindInfo> aliases = new LinkedList<>();

    private volatile boolean enabled = true;

    public BinderServiceConfigurator(ContextNames.BindInfo binding, ServiceName targetServiceName) {
        super(binding.getBinderServiceName());
        this.binding = binding;
        this.targetServiceName = targetServiceName;
    }

    public BinderServiceConfigurator alias(ContextNames.BindInfo alias) {
        this.aliases.add(alias);
        return this;
    }

    @Override
    public ServiceConfigurator configure(OperationContext context) {
        this.enabled = context.hasOptionalCapability(CommonRequirement.NAMING_STORE.getName(), null, null);
        return this;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        if (!this.enabled) {
            // If naming is not enabled, just install a dummy service that never starts
            return target.addService(this.getServiceName()).setInitialMode(ServiceController.Mode.NEVER);
        }
        String name = this.binding.getBindName();
        BinderService binder = new BinderService(name);
        // Until ServiceBasedNamingStore works with new MSC API, we need to use deprecated ServiceBuilder methods
        ServiceBuilder<ManagedReferenceFactory> builder = target.addService(this.getServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(name))
                .addDependency(this.targetServiceName, Object.class, new ManagedReferenceInjector<>(binder.getManagedObjectInjector()))
                .addDependency(this.binding.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
        ;
        for (ContextNames.BindInfo alias : this.aliases) {
            builder.addAliases(alias.getBinderServiceName(), ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(alias.getBindName()));
        }
        return builder.setInitialMode(ServiceController.Mode.PASSIVE);
    }
}
