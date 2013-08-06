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

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.dispatcher.CommandResponse;
import org.wildfly.clustering.group.Node;

public class ChannelManagementService implements Service<ChannelManagement>, ChannelManagement {

    public static ServiceName getServiceName(String channelName) {
        return ChannelService.getServiceName(channelName).append("management");
    }

    private final ServiceName name;
    private final Value<CommandDispatcherFactory> dispatcherFactory;
    private final String channelName;
    private volatile CommandDispatcher<String> dispatcher;

    public ChannelManagementService(ServiceName name, Value<CommandDispatcherFactory> dispatcherFactory, String channelName) {
        this.name = name;
        this.dispatcherFactory = dispatcherFactory;
        this.channelName = channelName;
    }

    @Override
    public ChannelManagement getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.dispatcher = this.dispatcherFactory.getValue().createCommandDispatcher(this.name, this.channelName);
    }

    @Override
    public void stop(StopContext context) {
        this.dispatcher.close();
    }

    @Override
    public Map<Node, ChannelState> getClusterState() throws InterruptedException {
        Command<ChannelState, String> command = new ChannelStateCommand();
        Map<Node, CommandResponse<ChannelState>> responses = this.dispatcher.executeOnCluster(command);
        Map<Node, ChannelState> result = new TreeMap<Node, ChannelState>();
        for (Map.Entry<Node, CommandResponse<ChannelState>> entry: responses.entrySet()) {
            try {
                ChannelState response = entry.getValue().get();
                if (response != null) {
                    result.put(entry.getKey(), response);
                }
            } catch (ExecutionException e) {
                // Log
                System.out.println("Execution exception: " + e.toString());
            }
        }
        return result;
    }
}
