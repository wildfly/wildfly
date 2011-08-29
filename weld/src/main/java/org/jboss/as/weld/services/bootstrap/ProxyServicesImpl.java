/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld.services.bootstrap;

import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.weld.exceptions.WeldException;
import org.jboss.weld.logging.messages.BeanMessage;
import org.jboss.weld.serialization.spi.ProxyServices;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * {@link ProxyServices} implementation that delegates to the module class loader if the bean class loader cannot be determined
 *
 * @author Stuart Douglas
 *
 */
public class ProxyServicesImpl implements ProxyServices {

    private final Module module;

    public ProxyServicesImpl(Module module) {
        this.module = module;
    }

    public ClassLoader getClassLoader(final Class<?> proxiedBeanType) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return _getClassLoader(proxiedBeanType);
                }
            });
        } else {
            return _getClassLoader(proxiedBeanType);
        }
    }

    private ClassLoader _getClassLoader(Class<?> proxiedBeanType) {
        if (proxiedBeanType.getName().startsWith("java")) {
            return module.getClassLoader();
        } else if(proxiedBeanType.getClassLoader() instanceof ModuleClassLoader) {
            final ModuleClassLoader loader = (ModuleClassLoader)proxiedBeanType.getClassLoader();
            //even though this is not strictly spec compliant if a class from the app server is
            //being proxied we use the deployment CL to prevent a memory leak
            //in theory this means that package private methods will not work correctly
            //however the application does not have access to package private methods anyway
            //as it is in a different class loader
            if(loader.getModule().getModuleLoader() instanceof ServiceModuleLoader) {
                //this is a dynamic module
                //we can use it to load the proxy
                return proxiedBeanType.getClassLoader();
            } else {
                //otherwise we use the deployments CL
                //rather than using a server modules CL
                return module.getClassLoader();
            }
        } else {
            return proxiedBeanType.getClassLoader();
        }
    }

    public void cleanup() {
        // This implementation requires no cleanup

    }

    public Class<?> loadBeanClass(final String className) {
        try {
            return (Class<?>) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    return Class.forName(className, true, getClassLoader(this.getClass()));
                }
            });
        } catch (PrivilegedActionException pae) {
            throw new WeldException(BeanMessage.CANNOT_LOAD_CLASS, className, pae.getException());
        }
    }

}
