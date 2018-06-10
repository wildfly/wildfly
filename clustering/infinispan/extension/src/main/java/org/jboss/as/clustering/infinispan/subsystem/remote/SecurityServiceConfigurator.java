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

package org.jboss.as.clustering.infinispan.subsystem.remote;

import static org.jboss.as.clustering.infinispan.subsystem.remote.SecurityResourceDefinition.Attribute.SSL_CONTEXT;

import javax.net.ssl.SSLContext;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.SecurityConfiguration;
import org.infinispan.client.hotrod.configuration.SecurityConfigurationBuilder;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.infinispan.subsystem.ComponentServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Radoslav Husar
 */
public class SecurityServiceConfigurator extends ComponentServiceConfigurator<SecurityConfiguration> {

    private volatile SupplierDependency<SSLContext> sslContextDependency;

    SecurityServiceConfigurator(PathAddress address) {
        super(RemoteCacheContainerComponent.SECURITY, address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String sslContext = SSL_CONTEXT.resolveModelAttribute(context, model).asStringOrNull();
        this.sslContextDependency = (sslContext != null) ? new ServiceSupplierDependency<>(CommonUnaryRequirement.SSL_CONTEXT.getServiceName(context, sslContext)) : null;
        return this;
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return super.register((this.sslContextDependency != null) ? this.sslContextDependency.register(builder) : builder);
    }

    @Override
    public SecurityConfiguration get() {
        SecurityConfigurationBuilder securityBuilder = new ConfigurationBuilder().security();
        SSLContext sslContext = (this.sslContextDependency != null) ? this.sslContextDependency.get() : null;
        securityBuilder.ssl().sslContext(sslContext).enabled(sslContext != null);
        return securityBuilder.create();
    }
}
