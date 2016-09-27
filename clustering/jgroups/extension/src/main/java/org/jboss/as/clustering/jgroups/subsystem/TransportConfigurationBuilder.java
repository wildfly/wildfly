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

import java.util.EnumSet;
import java.util.Optional;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class TransportConfigurationBuilder extends AbstractProtocolConfigurationBuilder<TransportConfiguration> implements TransportConfiguration {

    private ValueDependency<SocketBinding> diagnosticsSocketBinding;
    private Topology topology = null;

    public TransportConfigurationBuilder(PathAddress address) {
        super(address);
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
        Optional<String> machine = ModelNodes.optionalString(MACHINE.resolveModelAttribute(context, transport));
        Optional<String> rack = ModelNodes.optionalString(RACK.resolveModelAttribute(context, transport));
        Optional<String> site = ModelNodes.optionalString(SITE.resolveModelAttribute(context, transport));
        if (site.isPresent() || rack.isPresent() || machine.isPresent()) {
            this.topology = new Topology() {
                @Override
                public String getMachine() {
                    return machine.orElse(null);
                }

                @Override
                public String getRack() {
                    return rack.orElse(null);
                }

                @Override
                public String getSite() {
                    return site.orElse(null);
                }
            };
        }

        this.diagnosticsSocketBinding = ModelNodes.optionalString(DIAGNOSTICS_SOCKET_BINDING.resolveModelAttribute(context, transport)).map(diagnosticsBinding -> new InjectedValueDependency<>(CommonUnaryRequirement.SOCKET_BINDING.getServiceName(context, diagnosticsBinding), SocketBinding.class)).orElse(null);

        for (ThreadPoolResourceDefinition pool : EnumSet.allOf(ThreadPoolResourceDefinition.class)) {
            String prefix = pool.getPrefix();
            ModelNode model = transport.get(pool.getPathElement().getKeyValuePair());

            this.getProperties().put(prefix + ".min_threads", pool.getMinThreads().resolveModelAttribute(context, model).asString());
            this.getProperties().put(prefix + ".max_threads", pool.getMaxThreads().resolveModelAttribute(context, model).asString());

            int queueSize = pool.getQueueLength().resolveModelAttribute(context, model).asInt();
            if (pool != ThreadPoolResourceDefinition.TIMER) {
                this.getProperties().put(prefix + ".queue_enabled", String.valueOf(queueSize > 0));
            }
            this.getProperties().put(prefix + ".queue_max_size", String.valueOf(queueSize));

            // keepalive_time in milliseconds
            this.getProperties().put(prefix + ".keep_alive_time", pool.getKeepAliveTime().resolveModelAttribute(context, model).asString());
            this.getProperties().put(prefix + ".rejection_policy", "abort");
        }

        return super.configure(context, transport);
    }

    @Override
    public boolean isShared() {
        return false;
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
