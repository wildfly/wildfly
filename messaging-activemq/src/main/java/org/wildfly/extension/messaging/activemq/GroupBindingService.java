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

package org.wildfly.extension.messaging.activemq;

import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;


/**
 * @author Emanuel Muckenhuber
 */
public class GroupBindingService implements Service<SocketBinding> {

    private static final String BASE = "bindings";
    private static final String BROADCAST = "broadcast";
    private static final String DISCOVERY = "discovery";

    private final InjectedValue<SocketBinding> bindingRef = new InjectedValue<SocketBinding>();

    @Override
    public void start(final StartContext context) throws StartException {
        //
    }

    @Override
    public void stop(StopContext context) {
        //
    }

    @Override
    public SocketBinding getValue() throws IllegalStateException, IllegalArgumentException {
        return bindingRef.getValue();
    }

    public InjectedValue<SocketBinding> getBindingRef() {
        return bindingRef;
    }

    public static ServiceName getBroadcastBaseServiceName(ServiceName activeMQServiceName) {
        return activeMQServiceName.append(BASE).append(BROADCAST);
    }

    public static ServiceName getDiscoveryBaseServiceName(ServiceName activeMQServiceName) {
        return activeMQServiceName.append(BASE).append(DISCOVERY);
    }
}
