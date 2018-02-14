/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.infinispan.commons.CacheException;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.remoting.responses.CacheNotFoundResponse;
import org.infinispan.remoting.transport.jgroups.CommandAwareRpcDispatcher;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.infinispan.remoting.transport.jgroups.MarshallerAdapter;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;

/**
 * Custom {@link JGroupsTransport} that uses a provided channel.
 * @author Paul Ferraro
 */
public class ChannelFactoryTransport extends JGroupsTransport {

    private final ChannelFactory factory;

    public ChannelFactoryTransport(ChannelFactory factory) {
        this.factory = factory;
    }

    @Inject
    @Override
    public void setConfiguration(GlobalConfiguration config) {
        GlobalConfigurationBuilder builder = new GlobalConfigurationBuilder();
        // WFLY-6685 Prevent Infinispan from registering channel mbeans
        // The JGroups subsystem already does this
        builder.globalJmxStatistics().read(config.globalJmxStatistics()).disable();
        // ISPN-4755 workaround
        TransportConfiguration transport = config.transport();
        builder.transport()
                .clusterName(transport.clusterName())
                .distributedSyncTimeout(transport.distributedSyncTimeout())
                .initialClusterSize(transport.initialClusterSize())
                .initialClusterTimeout(transport.initialClusterTimeout(), TimeUnit.MILLISECONDS)
                .machineId(transport.machineId())
                .nodeName(transport.nodeName())
                .rackId(transport.rackId())
                .siteId(transport.siteId())
                .transport(transport.transport())
                .withProperties(transport.properties())
                ;
        super.setConfiguration(builder.build());
    }

    @Override
    protected void initRPCDispatcher() {
        this.dispatcher = new CommandAwareRpcDispatcher(this.channel, this, this.globalHandler, this.getTimeoutExecutor(), this.timeService);
        MarshallerAdapter adapter = new MarshallerAdapter(this.marshaller) {
            @Override
            public Object objectFromBuffer(byte[] buffer, int offset, int length) throws Exception {
                return ChannelFactoryTransport.this.isUnknownForkResponse(ByteBuffer.wrap(buffer, offset, length)) ? CacheNotFoundResponse.INSTANCE : super.objectFromBuffer(buffer, offset, length);
            }
        };
        this.dispatcher.setRequestMarshaller(adapter);
        this.dispatcher.setResponseMarshaller(adapter);
        this.dispatcher.start();
    }

    boolean isUnknownForkResponse(ByteBuffer response) {
        return this.factory.isUnknownForkResponse(response);
    }

    @Override
    protected void initChannel() {
        try {
            this.channel = this.factory.createChannel(this.configuration.globalJmxStatistics().cacheManagerName());
            this.channel.setDiscardOwnMessages(false);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }
}
