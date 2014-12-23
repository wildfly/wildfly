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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.jboss.as.threads.ThreadsServices;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.jgroups.Channel;
import org.wildfly.clustering.infinispan.spi.service.CacheContainerServiceName;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.service.ChannelServiceName;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class TransportConfigurationBuilder implements Builder<TransportConfiguration>, Value<TransportConfiguration>, TransportConfiguration {

    private final InjectedValue<Channel> channel = new InjectedValue<>();
    private final InjectedValue<ChannelFactory> factory = new InjectedValue<>();
    private final String name;
    private long lockTimeout = TransportResourceDefinition.LOCK_TIMEOUT.getDefaultValue().asLong();
    private ValueDependency<Executor> executor = null;

    public TransportConfigurationBuilder(String name) {
        this.name = name;
    }

    @Override
    public ServiceName getServiceName() {
        return CacheContainerServiceName.CACHE_CONTAINER.getServiceName(this.name).append("transport");
    }

    @Override
    public ServiceBuilder<TransportConfiguration> build(ServiceTarget target) {
        ServiceBuilder<TransportConfiguration> builder = target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(ChannelServiceName.CHANNEL.getServiceName(this.name), Channel.class, this.channel)
                .addDependency(ChannelServiceName.FACTORY.getServiceName(this.name), ChannelFactory.class, this.factory)
        ;
        if (this.executor != null) {
            this.executor.register(builder);
        }
        return builder.setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public TransportConfiguration getValue() {
        return this;
    }

    public TransportConfigurationBuilder setLockTimeout(long value, TimeUnit unit) {
        this.lockTimeout = TimeUnit.MILLISECONDS.convert(value, unit);
        return this;
    }

    public TransportConfigurationBuilder setExecutor(String executorName) {
        if (executorName != null) {
            this.executor = new InjectedValueDependency<>(ThreadsServices.executorName(executorName), Executor.class);
        }
        return this;
    }

    @Override
    public long getLockTimeout() {
        return this.lockTimeout;
    }

    @Override
    public Channel getChannel() {
        return this.channel.getValue();
    }

    @Override
    public ChannelFactory getChannelFactory() {
        return this.factory.getValue();
    }

    @Override
    public Executor getExecutor() {
        return (this.executor != null) ? this.executor.getValue() : null;
    }
}
