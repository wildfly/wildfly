/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.remote;

import java.io.IOException;
import java.util.Hashtable;
import javax.naming.Context;
import org.jboss.as.naming.NamingContext;
import org.jboss.as.naming.NamingStore;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Endpoint;
import org.wildfly.naming.client.remote.RemoteNamingService;

/**
 * @author John Bailey
 */
public class RemoteNamingServerService implements Service<RemoteNamingService> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("naming", "remote");
    private final InjectedValue<Endpoint> endpoint = new InjectedValue<Endpoint>();
    private final InjectedValue<NamingStore> namingStore = new InjectedValue<NamingStore>();
    private RemoteNamingService remoteNamingService;

    public synchronized void start(StartContext context) throws StartException {
        try {
            final Context namingContext = new NamingContext(namingStore.getValue(), new Hashtable<String, Object>());
            remoteNamingService = new RemoteNamingService(namingContext);
            remoteNamingService.start(endpoint.getValue());
        } catch (Exception e) {
            throw new StartException("Failed to start remote naming service", e);
        }
    }

    public synchronized void stop(StopContext context) {
        try {
            remoteNamingService.stop();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to stop remote naming service", e);
        }
    }

    public synchronized RemoteNamingService getValue() throws IllegalStateException, IllegalArgumentException {
        return remoteNamingService;
    }

    public Injector<Endpoint> getEndpointInjector() {
        return endpoint;
    }

    public Injector<NamingStore> getNamingStoreInjector() {
        return namingStore;
    }
}
