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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.util.Reflections;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.weld.interceptor.proxy.LifecycleMixin;
import org.jboss.weld.logging.BeanLogger;
import org.jboss.weld.serialization.spi.BeanIdentifier;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * {@link ProxyServices} implementation that delegates to the module class loader if the bean class loader cannot be determined
 *
 * @author Stuart Douglas
 * @author Jozef Hartinger
 *
 */
public class ProxyServicesImpl implements ProxyServices {

    private static String[] REQUIRED_WELD_DEPENDENCIES = new String[] {
        "org.jboss.weld.core",
        "org.jboss.weld.spi"
    };

    // these are used to check whether a classloader is capable of loading Weld proxies
    private static String[] WELD_CLASSES = new String[] {
        BeanIdentifier.class.getName(),
        LifecycleMixin.class.getName()
    };

    private final Module module;
    private final ConcurrentMap<ModuleIdentifier, Boolean> processedStaticModules = new ConcurrentHashMap<>();

    public ProxyServicesImpl(Module module) {
        this.module = module;
    }

    public ClassLoader getClassLoader(final Class<?> proxiedBeanType) {
        if (WildFlySecurityManager.isChecking()) {
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
                // this class comes from a static module
                // first, check if we can use its classloader to load proxy classes
                final Module  definingModule = loader.getModule();
                Boolean hasWeldDependencies = processedStaticModules.get(definingModule.getIdentifier());
                boolean logWarning = false; // only log for the first class in the module

                if (hasWeldDependencies == null) {
                    hasWeldDependencies = canLoadWeldProxies(definingModule); // may be run multiple times but that does not matter
                    logWarning = processedStaticModules.putIfAbsent(definingModule.getIdentifier(), hasWeldDependencies) == null;
                }
                if (hasWeldDependencies) {
                    // this module declares weld dependencies - we can use module's classloader to load the proxy class
                    // pros: package-private members will work fine
                    // cons: proxy classes will remain loaded by the module's classloader after undeployment (nothing else leaks)
                    return proxiedBeanType.getClassLoader();
                } else {
                    // no weld dependencies - we use deployment's classloader to load the proxy class
                    // pros: proxy classes unloaded with undeployment
                    // cons: package-private methods and constructors will yield IllegalAccessException
                    if (logWarning) {
                        WeldLogger.ROOT_LOGGER.loadingProxiesUsingDeploymentClassLoader(definingModule.getIdentifier(), Arrays.toString(REQUIRED_WELD_DEPENDENCIES));
                    }
                    return this.module.getClassLoader();
                }
            }
        } else {
            return proxiedBeanType.getClassLoader();
        }
    }

    private static boolean canLoadWeldProxies(Module module) {
        for (String weldClass : WELD_CLASSES) {
            if (!Reflections.isAccessible(weldClass, module.getClassLoader())) {
                return false;
            }
        }
        return true;
    }

    public void cleanup() {
        processedStaticModules.clear();
    }

    public Class<?> loadBeanClass(final String className) {
        try {
            return (Class<?>) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    return Class.forName(className, true, getClassLoader(this.getClass()));
                }
            });
        } catch (PrivilegedActionException pae) {
            throw BeanLogger.LOG.cannotLoadClass(className, pae.getException());
        }
    }

}
