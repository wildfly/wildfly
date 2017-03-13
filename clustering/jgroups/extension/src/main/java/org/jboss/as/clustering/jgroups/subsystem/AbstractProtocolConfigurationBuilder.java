/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.AbstractProtocolResourceDefinition.Attribute.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.jgroups.ProtocolDefaults;
import org.jboss.as.clustering.jgroups.protocol.ProtocolFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.jgroups.stack.Configurator;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.service.Builder;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractProtocolConfigurationBuilder<P extends Protocol, C extends ProtocolConfiguration<P>> implements ResourceServiceBuilder<C>, Value<C>, ProtocolConfiguration<P>, Consumer<P> {

    private final String name;
    private final InjectedValue<ModuleLoader> loader = new InjectedValue<>();
    private final InjectedValue<ProtocolDefaults> defaults = new InjectedValue<>();

    private volatile Map<String, String> properties;
    private volatile String moduleName;
    private volatile Boolean statisticsEnabled;

    protected AbstractProtocolConfigurationBuilder(String name) {
        this.name = name;
    }

    @Override
    public ServiceBuilder<C> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, this.loader)
                .addDependency(ProtocolDefaultsBuilder.SERVICE_NAME, ProtocolDefaults.class, this.defaults)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public Builder<C> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.moduleName = MODULE.resolveModelAttribute(context, model).asString();
        this.properties = ModelNodes.optionalPropertyList(PROPERTIES.resolveModelAttribute(context, model)).orElse(Collections.emptyList()).stream().collect(Collectors.toMap(Property::getName, property -> property.getValue().asString()));
        this.statisticsEnabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBooleanOrNull();
        return this;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final P createProtocol(ProtocolStackConfiguration stackConfiguration) {
        StringBuilder builder = new StringBuilder();
        if (this.moduleName.equals(AbstractProtocolResourceDefinition.Attribute.MODULE.getDefinition().getDefaultValue().asString()) && !this.name.startsWith(org.jgroups.conf.ProtocolConfiguration.protocol_prefix)) {
            builder.append(org.jgroups.conf.ProtocolConfiguration.protocol_prefix).append('.');
        }
        String className = builder.append(this.name).toString();
        try {
            Module module = this.loader.getValue().loadModule(this.moduleName);
            Class<? extends Protocol> protocolClass = module.getClassLoader().loadClass(className).asSubclass(Protocol.class);
            Protocol protocol = ProtocolFactory.newInstance(protocolClass);
            P result = (P) ProtocolFactory.TRANSFORMER.apply(protocol);
            Map<String, String> properties = new HashMap<>(this.defaults.getValue().getProperties(this.name));
            properties.putAll(this.properties);
            Configurator.resolveAndAssignFields(result, properties);
            Configurator.resolveAndInvokePropertyMethods(result, properties);
            this.accept(result);
            result.enableStats(this.statisticsEnabled != null ? this.statisticsEnabled : stackConfiguration.isStatisticsEnabled());
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
