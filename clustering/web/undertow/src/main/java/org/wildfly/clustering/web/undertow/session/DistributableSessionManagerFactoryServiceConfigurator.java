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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.modules.Module;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.marshalling.jboss.ExternalizerObjectTable;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.jboss.SimpleClassTable;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshalledValueFactory;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingContextFactory;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration;
import org.wildfly.clustering.web.session.SessionManagerFactoryServiceConfiguratorProvider;
import org.wildfly.extension.undertow.session.DistributableSessionManagerConfiguration;

import io.undertow.servlet.api.SessionManagerFactory;

/**
 * Distributable {@link SessionManagerFactory} builder for Undertow.
 * @author Paul Ferraro
 */
public class DistributableSessionManagerFactoryServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Function<org.wildfly.clustering.web.session.SessionManagerFactory<LocalSessionContext, Batch>, SessionManagerFactory> {

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

    private final DistributableSessionManagerConfiguration config;
    private final CapabilityServiceConfigurator configurator;

    public DistributableSessionManagerFactoryServiceConfigurator(ServiceName name, DistributableSessionManagerConfiguration config, SessionManagerFactoryServiceConfiguratorProvider provider) {
        super(name);
        this.config = config;

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
        this.configurator = provider.getServiceConfigurator(configuration);
    }

    @Override
    public SessionManagerFactory apply(org.wildfly.clustering.web.session.SessionManagerFactory<LocalSessionContext, Batch> factory) {
        return new DistributableSessionManagerFactory(factory, this.config);
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.configurator.configure(support);
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        this.configurator.build(target).install();

        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Supplier<org.wildfly.clustering.web.session.SessionManagerFactory<LocalSessionContext, Batch>> impl = builder.requires(this.configurator.getServiceName());
        Consumer<SessionManagerFactory> factory = builder.provides(this.getServiceName());
        Service service = new FunctionalService<>(factory, this, impl);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
