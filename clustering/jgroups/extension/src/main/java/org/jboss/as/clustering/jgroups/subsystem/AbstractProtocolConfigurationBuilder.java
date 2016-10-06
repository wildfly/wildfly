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

import static org.jboss.as.clustering.jgroups.subsystem.ProtocolResourceDefinition.Attribute.*;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractProtocolConfigurationBuilder<P extends ProtocolConfiguration> extends ProtocolServiceNameProvider implements ResourceServiceBuilder<P>, Value<P>, ProtocolConfiguration {

    private final String name;

    private final Map<String, String> properties = new HashMap<>();
    private ModuleIdentifier module = ProtocolConfiguration.DEFAULT_MODULE;
    private ValueDependency<SocketBinding> socketBinding;
    private ValueDependency<DataSource> dataSource;

    public AbstractProtocolConfigurationBuilder(PathAddress address) {
        this(address.getParent(), address.getLastElement().getValue());
    }

    private AbstractProtocolConfigurationBuilder(PathAddress stackAddress, String name) {
        super(stackAddress, name);
        this.name = name;
    }

    @Override
    public ServiceBuilder<P> build(ServiceTarget target) {
        ServiceBuilder<P> builder = target.addService(this.getServiceName(), new ValueService<>(this));
        if (this.socketBinding != null) {
            this.socketBinding.register(builder);
        }
        if (this.dataSource != null) {
            this.dataSource.register(builder);
        }
        return builder.setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public Builder<P> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.module = ModelNodes.asModuleIdentifier(MODULE.resolveModelAttribute(context, model));
        String binding = ModelNodes.asString(SOCKET_BINDING.resolveModelAttribute(context, model));
        if (binding != null) {
            this.socketBinding = new InjectedValueDependency<>(CommonUnaryRequirement.SOCKET_BINDING.getServiceName(context, binding), SocketBinding.class);
        }
        String dataSource = ModelNodes.asString(DATA_SOURCE.resolveModelAttribute(context, model));
        if (dataSource != null) {
            this.dataSource = new InjectedValueDependency<>(CommonUnaryRequirement.DATA_SOURCE.getServiceName(context, dataSource), DataSource.class);
        }
        for (Property property : ModelNodes.asPropertyList(PROPERTIES.resolveModelAttribute(context, model))) {
            this.properties.put(property.getName(), property.getValue().asString());
        }
        return this;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getProtocolClassName() {
        StringBuilder builder = new StringBuilder();
        if (this.module.equals(ProtocolConfiguration.DEFAULT_MODULE) && !this.name.startsWith(org.jgroups.conf.ProtocolConfiguration.protocol_prefix)) {
            builder.append(org.jgroups.conf.ProtocolConfiguration.protocol_prefix).append('.');
        }
        return builder.append(this.name).toString();
    }

    @Override
    public Map<String, String> getProperties() {
        return this.properties;
    }

    @Override
    public SocketBinding getSocketBinding() {
        return (this.socketBinding != null) ? this.socketBinding.getValue() : null;
    }

    @Override
    public ModuleIdentifier getModule() {
        return this.module;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public DataSource getDataSource() {
        return (this.dataSource != null) ? this.dataSource.getValue() : null;
    }
}
