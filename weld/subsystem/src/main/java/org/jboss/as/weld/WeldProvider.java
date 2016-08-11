/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.weld;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.CDIProvider;

import org.jboss.as.weld.deployment.WeldDeployment;
import org.jboss.as.weld.util.Reflections;
import org.jboss.weld.Container;
import org.jboss.weld.ContainerState;
import org.jboss.weld.AbstractCDI;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.logging.BeanManagerLogger;
import org.jboss.weld.manager.BeanManagerImpl;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Service provider for {@link CDI}.
 *
 * @author Jozef Hartinger
 *
 */
public class WeldProvider implements CDIProvider {

    private static final ConcurrentMap<Container, CdiImpl> containers = new ConcurrentHashMap<>();

    static void containerInitialized(Container container, BeanManagerImpl rootBeanManager, WeldDeployment deployment) {
        containers.put(container, new WeldProvider.CdiImpl(container, new BeanManagerProxy(rootBeanManager), deployment));
    }

    static void containerShutDown(Container container) {
        containers.remove(container);
    }

    @Override
    public CDI<Object> getCDI() {
        final Container container = Container.instance();
        checkContainerState(container);
        return containers.get(container);
    }

    private static void checkContainerState(Container container) {
        ContainerState state = container.getState();
        if (state.equals(ContainerState.STOPPED) || state.equals(ContainerState.SHUTDOWN)) {
            throw BeanManagerLogger.LOG.beanManagerNotAvailable();
        }
    }

    private static class CdiImpl extends AbstractCDI<Object> {

        private final Container container;
        private final BeanManagerProxy rootBeanManager;
        private final WeldDeployment deployment;

        public CdiImpl(Container container, BeanManagerProxy rootBeanManager, WeldDeployment deployment) {
            this.container = container;
            this.rootBeanManager = rootBeanManager;
            this.deployment = deployment;
        }

        @Override
        public BeanManagerProxy getBeanManager() {
            checkContainerState(container);
            final String callerName = getCallingClassName();

            final ClassLoader tccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            final Class<?> callerClass = Reflections.loadClass(callerName, tccl);
            if (callerClass != null) {
                final BeanDeploymentArchive bda = deployment.getBeanDeploymentArchive(callerClass);
                if (bda != null) {
                    return new BeanManagerProxy(container.beanDeploymentArchives().get(bda));
                }
            }
            // fallback for cases when we are unable to load the class or no BeanManager exists yet for the given BDA
            return rootBeanManager;
        }

        @Override
        public String toString() {
            return "Weld instance for deployment " + BeanManagerProxy.unwrap(rootBeanManager).getContextId();
        }
    }
}
