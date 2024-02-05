/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        this.enabled = context.getCapabilityServiceSupport().hasCapability(CommonRequirement.NAMING_STORE.getName());
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
