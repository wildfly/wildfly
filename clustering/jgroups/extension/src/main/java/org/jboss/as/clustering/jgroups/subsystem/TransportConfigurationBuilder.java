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

import static org.jboss.as.clustering.jgroups.subsystem.TransportResourceDefinition.Attribute.*;

import org.jboss.as.clustering.controller.CapabilityDependency;
import org.jboss.as.clustering.controller.RequiredCapability;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class TransportConfigurationBuilder extends AbstractProtocolConfigurationBuilder<TransportConfiguration> implements TransportConfiguration {

    private ValueDependency<SocketBinding> diagnosticsSocketBinding;
    private boolean shared;
    private Topology topology = null;

    public TransportConfigurationBuilder(String stackName, String name) {
        super(stackName, name);
    }

    @Override
    public ServiceBuilder<TransportConfiguration> build(ServiceTarget target) {
        ServiceBuilder<TransportConfiguration> builder = super.build(target);
        if (this.diagnosticsSocketBinding != null) {
            this.diagnosticsSocketBinding.register(builder);
        }
        return builder;
    }

    @Override
    public TransportConfiguration getValue() {
        return this;
    }

    @Override
    public Builder<TransportConfiguration> configure(OperationContext context, ModelNode transport) throws OperationFailedException {
        final String machine = ModelNodes.asString(MACHINE.getDefinition().resolveModelAttribute(context, transport));
        final String rack = ModelNodes.asString(RACK.getDefinition().resolveModelAttribute(context, transport));
        final String site = ModelNodes.asString(SITE.getDefinition().resolveModelAttribute(context, transport));
        if ((site != null) || (rack != null) || (machine != null)) {
            this.topology = new Topology() {
                @Override
                public String getMachine() {
                    return machine;
                }

                @Override
                public String getRack() {
                    return rack;
                }

                @Override
                public String getSite() {
                    return site;
                }
            };
        }

        this.shared = SHARED.getDefinition().resolveModelAttribute(context, transport).asBoolean();

        String diagnosticsBinding = ModelNodes.asString(DIAGNOSTICS_SOCKET_BINDING.getDefinition().resolveModelAttribute(context, transport));
        if (diagnosticsBinding != null) {
            this.diagnosticsSocketBinding = new CapabilityDependency<>(context, RequiredCapability.SOCKET_BINDING, diagnosticsBinding, SocketBinding.class);
        }

        for (ThreadPoolResourceDefinition pool : ThreadPoolResourceDefinition.values()) {
            String prefix = pool.getPrefix();
            ModelNode model = transport.get(pool.getPathElement().getKeyValuePair());

            this.getProperties().put(prefix + ".min_threads", pool.getMinThreads().getDefinition().resolveModelAttribute(context, model).asString());
            this.getProperties().put(prefix + ".max_threads", pool.getMaxThreads().getDefinition().resolveModelAttribute(context, model).asString());

            int queueSize = pool.getQueueLength().getDefinition().resolveModelAttribute(context, model).asInt();
            if (pool != ThreadPoolResourceDefinition.TIMER) {
                this.getProperties().put(prefix + ".queue_enabled", String.valueOf(queueSize > 0));
            }
            this.getProperties().put(prefix + ".queue_max_size", String.valueOf(queueSize));

            // keepalive_time in milliseconds
            this.getProperties().put(prefix + ".keep_alive_time", pool.getKeepAliveTime().getDefinition().resolveModelAttribute(context, model).asString());
        }

        return super.configure(context, transport);
    }

    @Override
    public boolean isShared() {
        return this.shared;
    }

    @Override
    public SocketBinding getDiagnosticsSocketBinding() {
        return (this.diagnosticsSocketBinding != null) ? this.diagnosticsSocketBinding.getValue() : null;
    }

    @Override
    public Topology getTopology() {
        return this.topology;
    }
}
