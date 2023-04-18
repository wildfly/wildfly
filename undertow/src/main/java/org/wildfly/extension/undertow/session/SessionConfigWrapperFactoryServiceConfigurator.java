/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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
