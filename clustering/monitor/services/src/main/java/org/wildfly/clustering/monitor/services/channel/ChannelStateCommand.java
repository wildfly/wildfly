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
package org.wildfly.clustering.monitor.services.channel;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jgroups.Channel;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.protocols.pbcast.GMS;
import org.wildfly.clustering.dispatcher.Command;

public class ChannelStateCommand implements Command<ChannelState, String> {
    private static final long serialVersionUID = -3174301922347674114L;
    private static final Map<ChannelState.RpcType, Field> rpcTypeFields = new EnumMap<>(ChannelState.RpcType.class);
    {
        rpcTypeFields.put(ChannelState.RpcType.ASYNC_ANYCAST, findMessageDispatcherField("async_anycasts"));
        rpcTypeFields.put(ChannelState.RpcType.ASYNC_UNICAST, findMessageDispatcherField("async_unicasts"));
        rpcTypeFields.put(ChannelState.RpcType.ASYNC_MULTICAST, findMessageDispatcherField("async_multicasts"));
        rpcTypeFields.put(ChannelState.RpcType.SYNC_ANYCAST, findMessageDispatcherField("sync_anycasts"));
        rpcTypeFields.put(ChannelState.RpcType.SYNC_UNICAST, findMessageDispatcherField("sync_unicasts"));
        rpcTypeFields.put(ChannelState.RpcType.SYNC_MULTICAST, findMessageDispatcherField("sync_multicasts"));
    }

    private static Field findMessageDispatcherField(String fieldName) {
        return findField(MessageDispatcher.class, fieldName);
    }

    private static Field findField(final Class<?> targetClass, final String fieldName) {
        PrivilegedAction<Field> action = new PrivilegedAction<Field>() {
            @Override
            public Field run() {
                try {
                    return targetClass.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        return AccessController.doPrivileged(action);
    }

    private static Object getFieldValue(final Object object, final Field field) {
        PrivilegedAction<Object> action = new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    field.setAccessible(true);
                    return field.get(object);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } finally {
                    field.setAccessible(false);
                }
            }
        };
        return AccessController.doPrivileged(action);
    }

    @Override
    public ChannelState execute(String cluster) throws Exception {
        ServiceRegistry registry = ServiceContainerHelper.getCurrentServiceContainer();
        ServiceController<Channel> channelService = ServiceContainerHelper.getService(registry, ChannelService.getServiceName(cluster));

        if (!channelService.getState().in(ServiceController.State.UP)) return null;

        Channel channel = channelService.getValue();
        GMS gms = (GMS) channel.getProtocolStack().findProtocol(GMS.class);
        if (gms == null) return null;

        ChannelState response = new ChannelStateResponse(channel.getClusterName(), gms.getView(), gms.printPreviousViews());

        ServiceController<EmbeddedCacheManager> containerService = ServiceContainerHelper.getService(registry, EmbeddedCacheManagerService.getServiceName(cluster));

        if (containerService.getState().in(ServiceController.State.UP)) {
            EmbeddedCacheManager container = containerService.getValue();
            JGroupsTransport transport = (JGroupsTransport) container.getTransport();
            MessageDispatcher dispatcher = transport.getCommandAwareRpcDispatcher();
            for (ChannelState.RpcType type: ChannelState.RpcType.values()) {
                response.getRpcStatistics().put(type, this.getRpcStatistic(dispatcher, type));
            }
        }

        return response;
    }

    private int getRpcStatistic(final MessageDispatcher dispatcher, ChannelState.RpcType type) {
        Field field = rpcTypeFields.get(type);
        AtomicInteger value = (AtomicInteger) getFieldValue(dispatcher, field);
        return (value != null) ? value.intValue() : 0;
    }
}
