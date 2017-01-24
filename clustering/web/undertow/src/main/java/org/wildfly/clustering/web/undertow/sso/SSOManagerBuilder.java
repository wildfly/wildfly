/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.undertow.sso;

import java.io.Externalizable;
import java.io.Serializable;
import java.util.function.Function;

import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.marshalling.jboss.DynamicClassTable;
import org.wildfly.clustering.marshalling.jboss.ExternalizerObjectTable;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.jboss.SimpleClassTable;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshalledValueFactory;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingContextFactory;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerConfiguration;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.clustering.web.undertow.IdentifierFactoryAdapter;

import io.undertow.server.session.SessionIdGenerator;

/**
 * @author Paul Ferraro
 */
public class SSOManagerBuilder<A, D, S, L> implements Builder<SSOManager<A, D, S, L, Batch>>, Service<SSOManager<A, D, S, L, Batch>>, SSOManagerConfiguration<L, MarshallingContext> {

    enum MarshallingVersion implements Function<Module, MarshallingConfiguration> {
        VERSION_1() {
            @Override
            public MarshallingConfiguration apply(Module module) {
                ModuleLoader loader = module.getModuleLoader();
                MarshallingConfiguration config = new MarshallingConfiguration();
                config.setClassResolver(ModularClassResolver.getInstance(loader));
                config.setClassTable(new SimpleClassTable(Serializable.class, Externalizable.class));
                return config;
            }
        },
        VERSION_2() {
            @Override
            public MarshallingConfiguration apply(Module module) {
                ModuleLoader loader = module.getModuleLoader();
                MarshallingConfiguration config = new MarshallingConfiguration();
                config.setClassResolver(ModularClassResolver.getInstance(loader));
                config.setClassTable(new DynamicClassTable(module.getClassLoader()));
                config.setObjectTable(new ExternalizerObjectTable(module.getClassLoader()));
                return config;
            }
        },
        ;
        static final MarshallingVersion CURRENT = VERSION_2;
    }

    private final InjectedValue<SessionIdGenerator> generator = new InjectedValue<>();
    @SuppressWarnings("rawtypes")
    private final InjectedValue<SSOManagerFactory> factory = new InjectedValue<>();
    private final ServiceName factoryServiceName;
    private final ServiceName generatorServiceName;
    private final LocalContextFactory<L> localContextFactory;

    private volatile SSOManager<A, D, S, L, Batch> manager;
    private volatile MarshallingContext context;

    public SSOManagerBuilder(ServiceName factoryServiceName, ServiceName generatorServiceName, LocalContextFactory<L> localContextFactory) {
        this.factoryServiceName = factoryServiceName;
        this.generatorServiceName = generatorServiceName;
        this.localContextFactory = localContextFactory;
    }

    @Override
    public ServiceName getServiceName() {
        return this.factoryServiceName.append("manager");
    }

    @Override
    public ServiceBuilder<SSOManager<A, D, S, L, Batch>> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), this)
                .addDependency(this.factoryServiceName, SSOManagerFactory.class, this.factory)
                .addDependency(this.generatorServiceName, SessionIdGenerator.class, this.generator)
                ;
    }

    @Override
    public void start(StartContext context) throws StartException {
        SSOManagerFactory<A, D, S, Batch> factory = this.factory.getValue();
        Module module = Module.forClass(this.getClass());
        this.context = new SimpleMarshallingContextFactory().createMarshallingContext(new SimpleMarshallingConfigurationRepository(MarshallingVersion.class, MarshallingVersion.CURRENT, module), null);
        this.manager = factory.createSSOManager(this);
        this.manager.start();
    }

    @Override
    public void stop(StopContext context) {
        this.manager.stop();
        this.manager = null;
        this.context = null;
    }

    @Override
    public SSOManager<A, D, S, L, Batch> getValue() {
        return this.manager;
    }

    @Override
    public IdentifierFactory<String> getIdentifierFactory() {
        return new IdentifierFactoryAdapter(this.generator.getValue());
    }

    @Override
    public LocalContextFactory<L> getLocalContextFactory() {
        return this.localContextFactory;
    }

    @Override
    public MarshalledValueFactory<MarshallingContext> getMarshalledValueFactory() {
        return new SimpleMarshalledValueFactory(this.context);
    }

    @Override
    public MarshallingContext getMarshallingContext() {
        return this.context;
    }
}
