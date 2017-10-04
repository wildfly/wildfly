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

import java.io.Externalizable;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;

import io.undertow.servlet.api.SessionManagerFactory;

import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.marshalling.jboss.ExternalizerObjectTable;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.jboss.SimpleClassTable;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshalledValueFactory;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingContextFactory;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.MappedValueService;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.session.SessionManagerFactoryBuilderProvider;
import org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration;
import org.wildfly.extension.undertow.session.DistributableSessionManagerConfiguration;

/**
 * Distributable {@link SessionManagerFactory} builder for Undertow.
 * @author Paul Ferraro
 */
public class DistributableSessionManagerFactoryBuilder implements CapabilityServiceBuilder<SessionManagerFactory> {

    static final Map<ReplicationGranularity, SessionManagerFactoryConfiguration.SessionAttributePersistenceStrategy> strategies = new EnumMap<>(ReplicationGranularity.class);
    static {
        strategies.put(ReplicationGranularity.SESSION, SessionManagerFactoryConfiguration.SessionAttributePersistenceStrategy.COARSE);
        strategies.put(ReplicationGranularity.ATTRIBUTE, SessionManagerFactoryConfiguration.SessionAttributePersistenceStrategy.FINE);
    }

    enum MarshallingVersion implements Function<Module, MarshallingConfiguration> {
        VERSION_1() {
            @Override
            public MarshallingConfiguration apply(Module module) {
                MarshallingConfiguration config = new MarshallingConfiguration();
                config.setClassResolver(ModularClassResolver.getInstance(module.getModuleLoader()));
                config.setClassTable(new SimpleClassTable(Serializable.class, Externalizable.class));
                return config;
            }
        },
        VERSION_2() {
            @Override
            public MarshallingConfiguration apply(Module module) {
                MarshallingConfiguration config = new MarshallingConfiguration();
                config.setClassResolver(ModularClassResolver.getInstance(module.getModuleLoader()));
                config.setClassTable(new SimpleClassTable(Serializable.class, Externalizable.class));
                config.setObjectTable(new ExternalizerObjectTable(module.getClassLoader()));
                return config;
            }
        },
        ;
        static final MarshallingVersion CURRENT = VERSION_2;
    }

    private final ServiceName name;
    private final CapabilityServiceBuilder<org.wildfly.clustering.web.session.SessionManagerFactory<LocalSessionContext, Batch>> factoryBuilder;

    public DistributableSessionManagerFactoryBuilder(ServiceName name, DistributableSessionManagerConfiguration config, SessionManagerFactoryBuilderProvider<Batch> provider) {
        this.name = name;

        Module module = config.getModule();
        MarshallingContext context = new SimpleMarshallingContextFactory().createMarshallingContext(new SimpleMarshallingConfigurationRepository(MarshallingVersion.class, MarshallingVersion.CURRENT, module), module.getClassLoader());
        MarshalledValueFactory<MarshallingContext> factory = new SimpleMarshalledValueFactory(context);
        LocalContextFactory<LocalSessionContext> localContextFactory = new LocalSessionContextFactory();
        SessionManagerFactoryConfiguration<MarshallingContext, LocalSessionContext> configuration = new SessionManagerFactoryConfiguration<MarshallingContext, LocalSessionContext>() {
            @Override
            public int getMaxActiveSessions() {
                return config.getMaxActiveSessions();
            }

            @Override
            public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
                return strategies.get(config.getGranularity());
            }

            @Override
            public String getServerName() {
                return config.getServerName();
            }

            @Override
            public String getDeploymentName() {
                return config.getDeploymentName();
            }

            @Override
            public String getCacheName() {
                return config.getCacheName();
            }

            @Override
            public MarshalledValueFactory<MarshallingContext> getMarshalledValueFactory() {
                return factory;
            }

            @Override
            public MarshallingContext getMarshallingContext() {
                return context;
            }

            @Override
            public LocalContextFactory<LocalSessionContext> getLocalContextFactory() {
                return localContextFactory;
            }
        };
        this.factoryBuilder = provider.getBuilder(configuration);
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public Builder<SessionManagerFactory> configure(CapabilityServiceSupport support) {
        this.factoryBuilder.configure(support);
        return this;
    }

    @Override
    public ServiceBuilder<SessionManagerFactory> build(ServiceTarget target) {
        this.factoryBuilder.build(target).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        @SuppressWarnings("rawtypes")
        InjectedValue<org.wildfly.clustering.web.session.SessionManagerFactory> sessionManagerFactoryValue = new InjectedValue<>();
        @SuppressWarnings("unchecked")
        Service<SessionManagerFactory> service = new MappedValueService<>(sessionManagerFactory -> new DistributableSessionManagerFactory(sessionManagerFactory), sessionManagerFactoryValue);
        return target.addService(this.name, service)
                .addDependency(this.factoryBuilder.getServiceName(), org.wildfly.clustering.web.session.SessionManagerFactory.class, sessionManagerFactoryValue)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
