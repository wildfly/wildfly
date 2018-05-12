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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.marshalling.jboss.DynamicClassTable;
import org.wildfly.clustering.marshalling.jboss.ExternalizerObjectTable;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.jboss.SimpleClassTable;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshalledValueFactory;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingContextFactory;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
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
public class SSOManagerServiceConfigurator<A, D, S, L> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Supplier<SSOManager<A, D, S, L, Batch>>, Consumer<SSOManager<A, D, S, L, Batch>>, SSOManagerConfiguration<L, MarshallingContext> {

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

    private final SupplierDependency<SSOManagerFactory<A, D, S, Batch>> factory;
    private final SupplierDependency<SessionIdGenerator> generator;
    private final LocalContextFactory<L> localContextFactory;

    private volatile MarshallingContext context;

    public SSOManagerServiceConfigurator(ServiceName name, SupplierDependency<SSOManagerFactory<A, D, S, Batch>> factory, SupplierDependency<SessionIdGenerator> generator, LocalContextFactory<L> localContextFactory) {
        super(name);
        this.factory = factory;
        this.generator = generator;
        this.localContextFactory = localContextFactory;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SSOManager<A, D, S, L, Batch>> manager = new CompositeDependency(this.factory, this.generator).register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(manager, Function.identity(), this, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public SSOManager<A, D, S, L, Batch> get() {
        SSOManagerFactory<A, D, S, Batch> factory = this.factory.get();
        Module module = Module.forClass(this.getClass());
        this.context = new SimpleMarshallingContextFactory().createMarshallingContext(new SimpleMarshallingConfigurationRepository(MarshallingVersion.class, MarshallingVersion.CURRENT, module), null);
        SSOManager<A, D, S, L, Batch> manager = factory.createSSOManager(this);
        manager.start();
        return manager;
    }

    @Override
    public void accept(SSOManager<A, D, S, L, Batch> manager) {
        manager.stop();
    }

    @Override
    public IdentifierFactory<String> getIdentifierFactory() {
        return new IdentifierFactoryAdapter(this.generator.get());
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
