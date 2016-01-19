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

import java.util.function.Function;

import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.marshalling.jboss.ExternalizerObjectTable;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;

/**
 * Service that provides a versioned marshalling configuration for a deployment unit.
 * @author Paul Ferraro
 */
public class MarshallingConfigurationRepositoryValue implements Value<MarshallingConfigurationRepository>, MarshallingConfigurationContext {

    public static ServiceName getServiceName(ServiceName deploymentUnitServiceName) {
        return deploymentUnitServiceName.append("marshalling");
    }

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
                config.setObjectTable(new ExternalizerObjectTable(module.getClassLoader()));
                return config;
            }
        },
        ;
        static final MarshallingVersion CURRENT = VERSION_2;
    }

    private final Value<ModuleDeployment> deployment;
    private final Value<Module> module;

    public MarshallingConfigurationRepositoryValue(Value<ModuleDeployment> deployment, Value<Module> module) {
        this.deployment = deployment;
        this.module = module;
    }

    @Override
    public MarshallingConfigurationRepository getValue() {
        return new SimpleMarshallingConfigurationRepository(MarshallingVersion.class, MarshallingVersion.CURRENT, this);
    }

    @Override
    public Module getModule() {
        return this.module.getValue();
    }

    @Override
    public ModuleDeployment getDeployment() {
        return this.deployment.getValue();
    }
}
