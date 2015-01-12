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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.marshalling.VersionedMarshallingConfiguration;

/**
 * Service that provides a versioned marshalling configuration for a deployment unit.
 */
public class VersionedMarshallingConfigurationService implements Service<VersionedMarshallingConfiguration>, VersionedMarshallingConfiguration {

    public static ServiceName getServiceName(ServiceName deploymentUnitServiceName) {
        return deploymentUnitServiceName.append("marshalling");
    }

    private static final int CURRENT_VERSION = 1;

    private final Map<Integer, MarshallingConfiguration> configurations = new ConcurrentHashMap<>();
    private final Value<ModuleDeployment> deployment;
    private final Value<ModuleLoader> loader;

    public VersionedMarshallingConfigurationService(Value<ModuleDeployment> deployment, Value<ModuleLoader> loader) {
        this.deployment = deployment;
        this.loader = loader;
    }

    @Override
    public void start(StartContext context) {
        MarshallingConfiguration config = new MarshallingConfiguration();
        config.setClassResolver(ModularClassResolver.getInstance(this.loader.getValue()));
        config.setSerializabilityChecker(new StatefulSessionBeanSerializabilityChecker(this.deployment.getValue()));
        config.setClassTable(new StatefulSessionBeanClassTable());
        config.setObjectTable(new EJBClientContextIdentifierObjectTable());

        this.configurations.put(CURRENT_VERSION, config);
    }

    @Override
    public void stop(StopContext context) {
        this.configurations.clear();
    }

    @Override
    public VersionedMarshallingConfiguration getValue() {
        return this;
    }

    @Override
    public int getCurrentMarshallingVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public MarshallingConfiguration getMarshallingConfiguration(int version) {
        MarshallingConfiguration configuration = this.configurations.get(version);
        if (configuration == null) {
            throw EjbLogger.ROOT_LOGGER.unsupportedMarshallingVersion(version);
        }
        return configuration;
    }
}
