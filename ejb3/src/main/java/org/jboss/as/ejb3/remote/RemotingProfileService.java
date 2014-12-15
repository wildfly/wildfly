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

package org.jboss.as.ejb3.remote;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.remoting.AbstractOutboundConnectionService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.xnio.OptionMap;

/**
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
/**
 * @author Tomasz Adamski
 */
public class RemotingProfileService implements Service<RemotingProfileService> {

    public static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "profile");

    private final Map<ServiceName, InjectedValue<AbstractOutboundConnectionService>> connectionInjectors = new HashMap<ServiceName, InjectedValue<AbstractOutboundConnectionService>>();

    private final Map<String, Long> connectionTimeouts = Collections.synchronizedMap(new HashMap<String, Long>());
    private final Map<String, OptionMap> channelCreationOpts = Collections.synchronizedMap(new HashMap<String, OptionMap>());

    /**
     * (optional) local EJB receiver for the EJB client context
     */
    private final InjectedValue<LocalEjbReceiver> localEjbReceiverInjector = new InjectedValue<LocalEjbReceiver>();

    @Override
    public RemotingProfileService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    public void addRemotingConnectionInjector(final ServiceName connectionName,final InjectedValue<AbstractOutboundConnectionService> connectionInjector) {
        connectionInjectors.put(connectionName, connectionInjector);
    }

    public void addChannelCreationOption(final String connectionRef,final OptionMap optionMap) {
        channelCreationOpts.put(connectionRef, optionMap);
    }

    public void addConnectionTimeout(final String connectionRef, final long timeout){
        connectionTimeouts.put(connectionRef, timeout);
    }

    public Map<ServiceName, InjectedValue<AbstractOutboundConnectionService>> getRemotingConnections() {
        return connectionInjectors;
    }

    public Map<String,Long> getConnectionTimeouts(){
        return connectionTimeouts;
    }

    public Map<String, OptionMap> getChannelCreationOpts() {
        return channelCreationOpts;
    }

    public InjectedValue<LocalEjbReceiver> getLocalEjbReceiverInjector() {
        return localEjbReceiverInjector;
    }


}
