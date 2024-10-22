/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiName;
import org.jboss.msc.service.ServiceName;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class DefaultUnaryServiceInstallerProvider<T> implements Function<String, Iterable<ServiceInstaller>> {

    private final UnaryServiceDescriptor<T> descriptor;
    private final Function<String, JndiName> jndiNameFactory;

    protected DefaultUnaryServiceInstallerProvider(UnaryServiceDescriptor<T> descriptor, Function<String, JndiName> jndiNameFactory) {
        this.descriptor = descriptor;
        this.jndiNameFactory = jndiNameFactory;
    }

    @Override
    public Iterable<ServiceInstaller> apply(String value) {
        ServiceName name = ServiceNameFactory.resolveServiceName(this.descriptor, null);
        List<ServiceInstaller> installers = new ArrayList<>(2);
        installers.add(ServiceInstaller.builder(ServiceDependency.on(this.descriptor, value)).provides(name).build());
        if (!value.equals(ModelDescriptionConstants.DEFAULT)) {
            ContextNames.BindInfo binding = ContextNames.bindInfoFor(this.jndiNameFactory.apply(ModelDescriptionConstants.DEFAULT).getAbsoluteName());
            installers.add(new BinderServiceInstaller(binding, name));
        }
        return installers;
    }
}
