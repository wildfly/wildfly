/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.weld.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.weld.spi.BeanDeploymentArchiveServicesProvider;
import org.jboss.as.weld.spi.ModuleServicesProvider;
import org.jboss.modules.Module;
import org.jboss.weld.bootstrap.api.BootstrapService;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.exceptions.IllegalStateException;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 *
 * @author Martin Kouba
 */
public final class ServiceLoaders {

    private ServiceLoaders() {
    }

    /**
     *
     * @param serviceClass
     * @param loaderClass
     * @return
     */
    public static <T> Optional<T> loadSingle(Class<T> serviceClass, Class<?> loaderClass) {
        Iterator<T> iterator = ServiceLoader.load(serviceClass, WildFlySecurityManager.getClassLoaderPrivileged(loaderClass)).iterator();
        T service = null;
        while (iterator.hasNext()) {
            if (service != null) {
                throw new IllegalStateException("Exactly one service provider is required for: " + serviceClass);
            }
            service = iterator.next();
        }
        return Optional.ofNullable(service);
    }

    /**
     *
     * @param clazz
     * @param deploymentUnit
     * @return
     */
    public static Map<Class<? extends Service>, Service> loadModuleServices(ServiceLoader<ModuleServicesProvider> serviceLoader,
            DeploymentUnit rootDeploymentUnit, DeploymentUnit deploymentUnit, Module module, ResourceRoot resourceRoot) {
        List<Service> services = new ArrayList<>();
        for (ModuleServicesProvider provider : serviceLoader) {
            services.addAll(provider.getServices(rootDeploymentUnit, deploymentUnit, module, resourceRoot));
        }
        Map<Class<? extends Service>, Service> servicesMap = new HashMap<>();
        for (Service service : services) {
            for (Class<? extends Service> serviceInterface : identifyServiceInterfaces(service.getClass(), new HashSet<>())) {
                servicesMap.put(serviceInterface, service);
            }
        }
        return servicesMap;
    }

    /**
     *
     * @param clazz
     * @param archive
     * @return
     */
    public static Map<Class<? extends Service>, Service> loadBeanDeploymentArchiveServices(Class<?> clazz, BeanDeploymentArchive archive) {
        ServiceLoader<BeanDeploymentArchiveServicesProvider> serviceLoader = ServiceLoader.load(BeanDeploymentArchiveServicesProvider.class,
                WildFlySecurityManager.getClassLoaderPrivileged(clazz));
        List<Service> services = new ArrayList<>();
        for (BeanDeploymentArchiveServicesProvider provider : serviceLoader) {
            services.addAll(provider.getServices(archive));
        }
        Map<Class<? extends Service>, Service> servicesMap = new HashMap<>();
        for (Service service : services) {
            for (Class<? extends Service> serviceInterface : identifyServiceInterfaces(service.getClass(), new HashSet<>())) {
                servicesMap.put(serviceInterface, service);
            }
        }
        return servicesMap;
    }

    /**
     * Identifies service views for a service implementation class. A service view is either: - an interface that directly extends {@link Service} or
     * {@link BootstrapService} - a clazz that directly implements {@link Service} or {@link BootstrapService}
     *
     * @param clazz the given class
     * @param serviceInterfaces a set that this method populates with service views
     * @return serviceInterfaces
     */
    private static Set<Class<? extends Service>> identifyServiceInterfaces(Class<?> clazz, Set<Class<? extends Service>> serviceInterfaces) {
        if (clazz == null || Object.class.equals(clazz) || BootstrapService.class.equals(clazz)) {
            return serviceInterfaces;
        }
        for (Class<?> interfac3 : clazz.getInterfaces()) {
            if (Service.class.equals(interfac3) || BootstrapService.class.equals(interfac3)) {
                serviceInterfaces.add(Reflections.<Class<? extends Service>> cast(clazz));
            }
        }
        for (Class<?> interfac3 : clazz.getInterfaces()) {
            identifyServiceInterfaces(interfac3, serviceInterfaces);
        }
        identifyServiceInterfaces(clazz.getSuperclass(), serviceInterfaces);
        return serviceInterfaces;
    }

}
