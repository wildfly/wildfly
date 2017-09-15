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

import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.jgroups.ProtocolDefaults;
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
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.service.Builder;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractProtocolConfigurationBuilder<P extends Protocol, C extends ProtocolConfiguration<P>> implements ResourceServiceBuilder<C>, Value<C>, ProtocolConfiguration<P>, Consumer<P> {

    private final String name;
    private final InjectedValue<ModuleLoader> loader = new InjectedValue<>();
    private final InjectedValue<ProtocolDefaults> defaults = new InjectedValue<>();
    private final Map<String, String> properties = new HashMap<>();

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
        this.properties.clear();
        for (Property property : ModelNodes.optionalPropertyList(PROPERTIES.resolveModelAttribute(context, model)).orElse(Collections.emptyList())) {
            this.properties.put(property.getName(), property.getValue().asString());
        }
        this.statisticsEnabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBooleanOrNull();
        return this;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public final P createProtocol(ProtocolStackConfiguration stackConfiguration) {
        // A "native" protocol is one that is not specified as a class name
        boolean nativeProtocol = this.moduleName.equals(AbstractProtocolResourceDefinition.Attribute.MODULE.getDefinition().getDefaultValue().asString()) && !this.name.startsWith(org.jgroups.conf.ProtocolConfiguration.protocol_prefix);
        String className = nativeProtocol ? String.join(".", org.jgroups.conf.ProtocolConfiguration.protocol_prefix, this.name) : this.name;
        try {
            Module module = this.loader.getValue().loadModule(this.moduleName);
            Class<? extends Protocol> protocolClass = module.getClassLoader().loadClass(className).asSubclass(Protocol.class);
            Map<String, String> properties = new HashMap<>(this.defaults.getValue().getProperties(protocolClass));
            properties.putAll(this.properties);
            PrivilegedExceptionAction<Protocol> action = () -> {
                try {
                    return protocolClass.newInstance().setProperties(properties);
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            };
            @SuppressWarnings("unchecked")
            P protocol = (P) WildFlySecurityManager.doUnchecked(action);
            this.accept(protocol);
            protocol.enableStats(this.statisticsEnabled != null ? this.statisticsEnabled : stackConfiguration.isStatisticsEnabled());
            return protocol;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
