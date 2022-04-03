/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2019, Red Hat, Inc., and individual contributors
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
