/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.undertow.session;

import io.undertow.servlet.api.SessionManagerFactory;

import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.web.session.SessionManagerFactoryBuilder;
import org.wildfly.clustering.web.session.SessionManagerFactoryBuilderValue;
import org.wildfly.extension.undertow.session.DistributableSessionManagerFactoryBuilder;

/**
 * Distributable session manager factory builder for Undertow.
 * @author Paul Ferraro
 */
public class SessionManagerAdapterFactoryBuilder implements DistributableSessionManagerFactoryBuilder {

    private final SessionManagerFactoryBuilder builder;

    public SessionManagerAdapterFactoryBuilder() {
        this(new SessionManagerFactoryBuilderValue().getValue());
    }

    public SessionManagerAdapterFactoryBuilder(SessionManagerFactoryBuilder builder) {
        this.builder = builder;
    }

    @Override
    public ServiceBuilder<SessionManagerFactory> buildDeploymentDependency(ServiceTarget target, ServiceName name, final ServiceName deploymentServiceName, Module module, JBossWebMetaData metaData) {
        ServiceName clusteringServiceName = name.append("clustering");
        this.builder.buildDeploymentDependency(target, clusteringServiceName, deploymentServiceName, module, metaData)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
        final InjectedValue<org.wildfly.clustering.web.session.SessionManagerFactory> factory = new InjectedValue<>();
        Value<SessionManagerFactory> factoryValue = new Value<SessionManagerFactory>() {
            @Override
            public SessionManagerFactory getValue() throws IllegalStateException, IllegalArgumentException {
                return new SessionManagerAdapterFactory(factory.getValue(), deploymentServiceName.getSimpleName());
            }
        };
        return target.addService(name, new ValueService<>(factoryValue))
                .addDependency(clusteringServiceName, org.wildfly.clustering.web.session.SessionManagerFactory.class, factory)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;
    }

    @Override
    public ServiceBuilder<?> buildServerDependency(ServiceTarget target, Value<String> instanceId) {
        return this.builder.buildServerDependency(target, instanceId);
    }
}
