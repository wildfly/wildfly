/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.session;

import java.util.function.Consumer;
import java.util.function.Function;

import io.undertow.servlet.api.SessionConfigWrapper;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.web.session.AffinityLocator;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.extension.undertow.CookieConfig;

/**
 * Configures a service that provides a {@link SessionConfigWrapper} factory.
 * @author Paul Ferraro
 */
public class SessionConfigWrapperFactoryServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Function<CookieConfig, SessionConfigWrapper> {

    private final SupplierDependency<SessionIdentifierCodec> codecDependency;
    private final SupplierDependency<AffinityLocator> locatorDependency;

    public SessionConfigWrapperFactoryServiceConfigurator(ServiceName name, SupplierDependency<SessionIdentifierCodec> codecDependency, SupplierDependency<AffinityLocator> locatorDependency) {
        super(name);
        this.codecDependency = codecDependency;
        this.locatorDependency = locatorDependency;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<Function<CookieConfig, SessionConfigWrapper>> function = new CompositeDependency(this.codecDependency, this.locatorDependency).register(builder).provides(name);
        return builder.setInstance(Service.newInstance(function, this));
    }

    @Override
    public SessionConfigWrapper apply(CookieConfig config) {
        return (config != null) ? new AffinitySessionConfigWrapper(config, this.locatorDependency.get()) : new CodecSessionConfigWrapper(this.codecDependency.get());
    }
}
