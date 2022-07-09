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
package org.jboss.as.ejb3.component.stateful;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.Module;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.marshalling.jboss.DynamicExternalizerObjectTable;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Service that provides a versioned marshalling configuration for a deployment unit.
 * @author Paul Ferraro
 */
public class MarshallingConfigurationRepositoryServiceConfigurator extends SimpleServiceNameProvider implements ServiceConfigurator, MarshallingConfigurationContext, Supplier<MarshallingConfigurationRepository> {

    enum MarshallingVersion implements Function<MarshallingConfigurationContext, MarshallingConfiguration> {
        VERSION_1() {
            @SuppressWarnings("deprecation")
            @Override
            public MarshallingConfiguration apply(MarshallingConfigurationContext context) {
                Module module = context.getModule();
                ModuleDeployment deployment = context.getDeployment();
                MarshallingConfiguration config = new MarshallingConfiguration();
                config.setClassResolver(ModularClassResolver.getInstance(module.getModuleLoader()));
                config.setSerializabilityChecker(new StatefulSessionBeanSerializabilityChecker(deployment));
                config.setClassTable(new StatefulSessionBeanClassTable());
                config.setObjectTable(new EJBClientContextIdentifierObjectTable());
                return config;
            }
        },
        VERSION_2() {
            @Override
            public MarshallingConfiguration apply(MarshallingConfigurationContext context) {
                Module module = context.getModule();
                ModuleDeployment deployment = context.getDeployment();
                MarshallingConfiguration config = new MarshallingConfiguration();
                config.setClassResolver(ModularClassResolver.getInstance(module.getModuleLoader()));
                config.setSerializabilityChecker(new StatefulSessionBeanSerializabilityChecker(deployment));
                config.setClassTable(new StatefulSessionBeanClassTable());
                config.setObjectResolver(new EJBClientContextIdentifierResolver());
                config.setObjectTable(new DynamicExternalizerObjectTable(module.getClassLoader()));
                return config;
            }
        },
        ;
        static final MarshallingVersion CURRENT = VERSION_2;
    }

    private final Module module;
    private final SupplierDependency<ModuleDeployment> deployment;

    public MarshallingConfigurationRepositoryServiceConfigurator(DeploymentUnit unit) {
        super(unit.getServiceName().append("marshalling"));
        this.module = unit.getAttachment(Attachments.MODULE);
        this.deployment = new ServiceSupplierDependency<>(unit.getServiceName().append(ModuleDeployment.SERVICE_NAME));
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<MarshallingConfigurationRepository> repository = this.deployment.register(builder).provides(name);
        Service service = new FunctionalService<>(repository, Function.identity(), this);
        return builder.setInstance(service);
    }

    @Override
    public MarshallingConfigurationRepository get() {
        return new SimpleMarshallingConfigurationRepository(MarshallingVersion.class, MarshallingVersion.CURRENT, this);
    }

    @Override
    public Module getModule() {
        return this.module;
    }

    @Override
    public ModuleDeployment getDeployment() {
        return this.deployment.get();
    }
}
