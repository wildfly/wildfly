/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.interceptor.server;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.utils.ClassLoadingUtils;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class ClientInterceptorCache {

    private final List<ServerInterceptorMetaData> serverInterceptorMetaData;

    private List<Class<? extends EJBClientInterceptor>> clientInterceptors = null;

    public ClientInterceptorCache(final List<ServerInterceptorMetaData> interceptorsMetaData){
        this.serverInterceptorMetaData = interceptorsMetaData;
    }

    public List<Class<? extends EJBClientInterceptor>> getClientInterceptors() {
        synchronized(this) {
            if (clientInterceptors == null) {
                loadClientInterceptors();
            }
        }
        return clientInterceptors;
    }

    private void loadClientInterceptors(){
        clientInterceptors = new ArrayList<>();
        for (final ServerInterceptorMetaData si: serverInterceptorMetaData) {
            final String moduleId = si.getModule();
            try {
                final Module module = Module.getCallerModuleLoader().loadModule(moduleId);
                clientInterceptors.add(ClassLoadingUtils.loadClass(si.getClazz(), module).asSubclass(EJBClientInterceptor.class));
            } catch (ModuleLoadException e) {
                throw EjbLogger.ROOT_LOGGER.cannotLoadServerInterceptorModule(moduleId, e);
            } catch (ClassNotFoundException e) {
                throw EeLogger.ROOT_LOGGER.cannotLoadInterceptor(e, si.getClazz());
            }

        }
    }
}
