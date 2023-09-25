/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import java.util.function.Function;

/**
 * @author Stuart Douglas
 */
public class HttpRemoteNamingServerService implements Service<HttpRemoteNamingServerService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("naming", "http-remote");
    public static final String NAMING = "/naming";
    private static final Function<String, Boolean> REJECT_CLASS_RESOLUTION_FILTER = name -> Boolean.FALSE;
    private final InjectedValue<PathHandler> pathHandlerInjectedValue = new InjectedValue<>();
    private final InjectedValue<NamingStore> namingStore = new InjectedValue<NamingStore>();

    @Override
    public void start(StartContext startContext) throws StartException {
        final Context namingContext = new NamingContext(namingStore.getValue(), new Hashtable<String, Object>());
        HttpRemoteNamingService service = new HttpRemoteNamingService(namingContext, REJECT_CLASS_RESOLUTION_FILTER);
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
