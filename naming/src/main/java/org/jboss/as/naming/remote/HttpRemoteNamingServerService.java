/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.naming.remote;

import org.jboss.as.naming.NamingContext;
import org.jboss.as.naming.NamingStore;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import io.undertow.server.handlers.PathHandler;
import org.wildfly.httpclient.naming.HttpRemoteNamingService;

import javax.naming.Context;
import java.util.Hashtable;

/**
 * @author Stuart Douglas
 */
public class HttpRemoteNamingServerService implements Service<HttpRemoteNamingServerService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("naming", "http-remote");
    public static final String NAMING = "/naming";
    private final InjectedValue<PathHandler> pathHandlerInjectedValue = new InjectedValue<>();
    private final InjectedValue<NamingStore> namingStore = new InjectedValue<NamingStore>();

    @Override
    public void start(StartContext startContext) throws StartException {
        final Context namingContext = new NamingContext(namingStore.getValue(), new Hashtable<String, Object>());
        HttpRemoteNamingService service = new HttpRemoteNamingService(namingContext);
        pathHandlerInjectedValue.getValue().addPrefixPath(NAMING, service.createHandler());
    }

    @Override
    public void stop(StopContext stopContext) {
        pathHandlerInjectedValue.getValue().removePrefixPath(NAMING);

    }

    @Override
    public HttpRemoteNamingServerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<PathHandler> getPathHandlerInjectedValue() {
        return pathHandlerInjectedValue;
    }

    public InjectedValue<NamingStore> getNamingStore() {
        return namingStore;
    }
}
