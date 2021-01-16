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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.server.Services;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.marshalling.protostream.ModuleClassResolver;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValueFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerConfiguration;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.clustering.web.undertow.IdentifierFactoryAdapter;
import org.wildfly.security.manager.WildFlySecurityManager;

import io.undertow.server.session.SessionIdGenerator;

/**
 * @author Paul Ferraro
 */
public class SSOManagerServiceConfigurator<A, D, S, L> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, Supplier<SSOManager<A, D, S, L, Batch>>, Consumer<SSOManager<A, D, S, L, Batch>>, SSOManagerConfiguration<ByteBufferMarshaller, L> {

    private final SupplierDependency<SSOManagerFactory<A, D, S, Batch>> factory;
    private final SupplierDependency<SessionIdGenerator> generator;
    private final SupplierDependency<ModuleLoader> loader = new ServiceSupplierDependency<>(Services.JBOSS_SERVICE_MODULE_LOADER);
    private final LocalContextFactory<L> localContextFactory;

    private volatile ByteBufferMarshaller marshaller;

    public SSOManagerServiceConfigurator(ServiceName name, SupplierDependency<SSOManagerFactory<A, D, S, Batch>> factory, SupplierDependency<SessionIdGenerator> generator, LocalContextFactory<L> localContextFactory) {
        super(name);
        this.factory = factory;
        this.generator = generator;
        this.localContextFactory = localContextFactory;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SSOManager<A, D, S, L, Batch>> manager = new CompositeDependency(this.factory, this.generator, this.loader).register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(manager, Function.identity(), this, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public SSOManager<A, D, S, L, Batch> get() {
        SSOManagerFactory<A, D, S, Batch> factory = this.factory.get();
        this.marshaller = new ProtoStreamByteBufferMarshaller(new SerializationContextBuilder(new ModuleClassResolver(this.loader.get())).load(WildFlySecurityManager.getClassLoaderPrivileged(this.getClass())).build());
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
    public MarshalledValueFactory<ByteBufferMarshaller> getMarshalledValueFactory() {
        return new ByteBufferMarshalledValueFactory(this.marshaller);
    }
}
