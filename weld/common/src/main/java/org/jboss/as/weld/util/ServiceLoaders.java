/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.spi.BeanDeploymentArchiveServicesProvider;
import org.jboss.as.weld.spi.ModuleServicesProvider;
import org.jboss.modules.Module;
import org.jboss.weld.bootstrap.api.BootstrapService;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
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
                WeldLogger.ROOT_LOGGER.missingService(serviceClass);
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
    public static Map<Class<? extends Service>, Service> loadModuleServices(Iterable<ModuleServicesProvider> providers,
            DeploymentUnit rootDeploymentUnit, DeploymentUnit deploymentUnit, Module module, ResourceRoot resourceRoot) {
        List<Service> services = new ArrayList<>();
        for (ModuleServicesProvider provider : providers) {
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
