/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.naming;

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Installs a binder service.
 * @author Paul Ferraro
 */
public class BinderServiceInstaller implements ServiceInstaller {

    private final ContextNames.BindInfo binding;
    private final ServiceName targetServiceName;
    private final List<ContextNames.BindInfo> aliases = new LinkedList<>();

    public BinderServiceInstaller(ContextNames.BindInfo binding, ServiceName targetServiceName) {
        this.binding = binding;
        this.targetServiceName = targetServiceName;
    }

    public BinderServiceInstaller withAlias(ContextNames.BindInfo alias) {
        this.aliases.add(alias);
        return this;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ServiceController<?> install(RequirementServiceTarget target) {
        String name = this.binding.getBindName();
        BinderService binder = new BinderService(name);
        // Until ServiceBasedNamingStore works with new MSC API, we need to use deprecated ServiceBuilder methods
        ServiceBuilder<ManagedReferenceFactory> builder = target.addService(this.binding.getBinderServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(name))
                .addDependency(this.targetServiceName, Object.class, new ManagedReferenceInjector<>(binder.getManagedObjectInjector()))
                .addDependency(this.binding.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
        ;
        for (ContextNames.BindInfo alias : this.aliases) {
            builder.addAliases(alias.getBinderServiceName(), ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(alias.getBindName()));
        }
        return builder.setInitialMode(ServiceController.Mode.PASSIVE).install();
    }
}
