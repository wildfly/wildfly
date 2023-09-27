/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.PathAddress;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.web.service.sso.DistributableSSOManagementProvider;

/**
 * Abstract service configurator for single sign-on management providers.
 * @author Paul Ferraro
 */
public abstract class SSOManagementServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Supplier<DistributableSSOManagementProvider> {

    public SSOManagementServiceConfigurator(PathAddress address) {
        super(SSOManagementResourceDefinition.Capability.SSO_MANAGEMENT_PROVIDER, address);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<DistributableSSOManagementProvider> provider = builder.provides(name);
        Service service = Service.newInstance(provider, this.get());
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
