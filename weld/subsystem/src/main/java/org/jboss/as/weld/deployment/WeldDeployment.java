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

package org.jboss.as.weld.deployment;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.spi.Extension;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.weld.WeldModuleResourceLoader;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl.BeanArchiveType;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.services.bootstrap.ProxyServicesImpl;
import org.jboss.as.weld.util.Reflections;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.CDI11Deployment;
import org.jboss.weld.bootstrap.spi.EEModuleDescriptor;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Abstract implementation of {@link CDI11Deployment}.
 * <p>
 * Thread safety: This class is thread safe, and does not require a happens before action between construction and usage
 *
 * @author Stuart Douglas
 *
 */
public class WeldDeployment implements CDI11Deployment {

    public static final String ADDITIONAL_CLASSES_BDA_SUFFIX = ".additionalClasses";
    public static final String BOOTSTRAP_CLASSLOADER_BDA_ID = "bootstrapBDA" + ADDITIONAL_CLASSES_BDA_SUFFIX;

    private final Set<BeanDeploymentArchiveImpl> beanDeploymentArchives;

    private final Set<Metadata<Extension>> extensions;

    private final ServiceRegistry serviceRegistry;

    /**
     * The top level module
     */
    private final Module module;

    /**
     * All ModuleClassLoaders in the deployment
     */
    private final Set<ClassLoader> subDeploymentClassLoaders;

    private final Map<ClassLoader, BeanDeploymentArchiveImpl> additionalBeanDeploymentArchivesByClassloader;

    private volatile BeanDeploymentArchiveImpl bootstrapClassLoaderBeanDeploymentArchive;

    private final BeanDeploymentModule rootBeanDeploymentModule;
    private final Map<ModuleIdentifier, EEModuleDescriptor> eeModuleDescriptors;

    public WeldDeployment(Set<BeanDeploymentArchiveImpl> beanDeploymentArchives, Collection<Metadata<Extension>> extensions,
            Module module, Set<ClassLoader> subDeploymentClassLoaders, DeploymentUnit deploymentUnit, BeanDeploymentModule rootBeanDeploymentModule,
            Map<ModuleIdentifier, EEModuleDescriptor> eeModuleDescriptors) {
        this.subDeploymentClassLoaders = new HashSet<ClassLoader>(subDeploymentClassLoaders);
        this.beanDeploymentArchives = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.beanDeploymentArchives.addAll(beanDeploymentArchives);
        this.extensions = new HashSet<Metadata<Extension>>(extensions);
        this.serviceRegistry = new SimpleServiceRegistry();
        this.additionalBeanDeploymentArchivesByClassloader = new ConcurrentHashMap<>();
        this.module = module;
        this.rootBeanDeploymentModule = rootBeanDeploymentModule;
        this.eeModuleDescriptors = eeModuleDescriptors;

        // add static services
        this.serviceRegistry.add(ProxyServices.class, new ProxyServicesImpl(module));
        this.serviceRegistry.add(ResourceLoader.class, new WeldModuleResourceLoader(module));

        calculateAccessibilityGraph(this.beanDeploymentArchives);
        makeTopLevelBdasVisibleFromStaticModules();
    }

    /**
     * {@link org.jboss.as.weld.deployment.processors.WeldDeploymentProcessor} assembles a basic accessibility graph based on
     * the deployment structure. Here, we complete the graph by examining classloader visibility. This allows additional
     * accessibility edges caused e.g. by the Class-Path declaration in the manifest file, to be recognized.
     *
     * @param beanDeploymentArchives
     */
    private void calculateAccessibilityGraph(Iterable<BeanDeploymentArchiveImpl> beanDeploymentArchives) {
        for (BeanDeploymentArchiveImpl from : beanDeploymentArchives) {
            for (BeanDeploymentArchiveImpl target : beanDeploymentArchives) {
                if (from.isAccessible(target)) {
                    from.addBeanDeploymentArchive(target);
                }
            }
        }
    }

    /**
     * Adds additional edges to the accessibility graph that allow static CDI-enabled modules to inject beans from top-level deployment units
     */
    private void makeTopLevelBdasVisibleFromStaticModules() {
        for (BeanDeploymentArchiveImpl bda : beanDeploymentArchives) {
            if (bda.getBeanArchiveType().equals(BeanDeploymentArchiveImpl.BeanArchiveType.EXTERNAL) || bda.getBeanArchiveType().equals(BeanDeploymentArchiveImpl.BeanArchiveType.SYNTHETIC)) {
                for (BeanDeploymentArchiveImpl topLevelBda : rootBeanDeploymentModule.getBeanDeploymentArchives()) {
                    bda.addBeanDeploymentArchive(topLevelBda);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return Collections.unmodifiableSet(new HashSet<BeanDeploymentArchive>(beanDeploymentArchives));
    }

    /** {@inheritDoc} */
    public Iterable<Metadata<Extension>> getExtensions() {
        return Collections.unmodifiableSet(extensions);
    }

    /** {@inheritDoc} */
    public ServiceRegistry getServices() {
        return serviceRegistry;
    }

    /** {@inheritDoc} */
    public synchronized BeanDeploymentArchive loadBeanDeploymentArchive(final Class<?> beanClass) {
        final BeanDeploymentArchive bda = this.getBeanDeploymentArchive(beanClass);
        if (bda != null) {
            return bda;
        }
        Module module = Module.forClass(beanClass);
        if (module == null) {
            // Bean class loaded by the bootstrap class loader
            if (bootstrapClassLoaderBeanDeploymentArchive == null) {
                bootstrapClassLoaderBeanDeploymentArchive = createAndRegisterAdditionalBeanDeploymentArchive(module, beanClass);
            } else {
                bootstrapClassLoaderBeanDeploymentArchive.addBeanClass(beanClass);
            }
            return bootstrapClassLoaderBeanDeploymentArchive;
        }
        /*
         * No, there is no BDA for the class yet. Let's create one.
         */
        return createAndRegisterAdditionalBeanDeploymentArchive(module, beanClass);
    }

    protected BeanDeploymentArchiveImpl createAndRegisterAdditionalBeanDeploymentArchive(Module module, Class<?> beanClass) {
        String id = null;
        if (module == null) {
            id = BOOTSTRAP_CLASSLOADER_BDA_ID;
        } else {
            id = module.getIdentifier() + ADDITIONAL_CLASSES_BDA_SUFFIX;
        }
        BeanDeploymentArchiveImpl newBda = new BeanDeploymentArchiveImpl(Collections.singleton(beanClass.getName()),
                BeansXml.EMPTY_BEANS_XML, module, id, BeanArchiveType.SYNTHETIC, false);
        WeldLogger.DEPLOYMENT_LOGGER.beanArchiveDiscovered(newBda);
        newBda.addBeanClass(beanClass);
        ServiceRegistry newBdaServices = newBda.getServices();
        for (Entry<Class<? extends Service>, Service> entry : serviceRegistry.entrySet()) {
            // Do not overwrite existing services
            if (!newBdaServices.contains(entry.getKey())) {
                newBdaServices.add(entry.getKey(), Reflections.cast(entry.getValue()));
            }
        }
        if (module == null) {
            // For bootstrapClassLoaderBeanDeploymentArchive always use the parent ResourceLoader
            newBdaServices.add(ResourceLoader.class, serviceRegistry.get(ResourceLoader.class));
        }

        if (module != null && eeModuleDescriptors.containsKey(module.getIdentifier())) {
            newBda.getServices().add(EEModuleDescriptor.class, eeModuleDescriptors.get(module.getIdentifier()));
        }
        // handle BDAs visible from the new BDA
        for (BeanDeploymentArchiveImpl bda : beanDeploymentArchives) {
            if (newBda.isAccessible(bda)) {
                newBda.addBeanDeploymentArchive(bda);
            }
        }
        // handle visibility of the new BDA from other BDAs
        for (BeanDeploymentArchiveImpl bda : beanDeploymentArchives) {
            if (bda.isAccessible(newBda)) {
                bda.addBeanDeploymentArchive(newBda);
            }
        }
        // make the top-level deployment BDAs visible from the additional archive
        newBda.addBeanDeploymentArchives(rootBeanDeploymentModule.getBeanDeploymentArchives());

        // Ignore beans loaded by the bootstrap class loader. This should only be JDK classes in most cases.
        // See getBeanDeploymentArchive(final Class<?> beanClass), per the JavaDoc this is mean to archive which
        // contains the bean class.
        final ClassLoader cl = beanClass.getClassLoader();
        if (cl != null) {
            additionalBeanDeploymentArchivesByClassloader.put(cl, newBda);
        }
        beanDeploymentArchives.add(newBda);
        return newBda;
    }

    public Module getModule() {
        return module;
    }

    public Set<ClassLoader> getSubDeploymentClassLoaders() {
        return Collections.unmodifiableSet(subDeploymentClassLoaders);
    }

    public synchronized <T extends Service> void addWeldService(Class<T> type, T service) {
        serviceRegistry.add(type, service);
        for (BeanDeploymentArchiveImpl bda : additionalBeanDeploymentArchivesByClassloader.values()) {
            bda.getServices().add(type, service);
        }
    }

    @Override
    public BeanDeploymentArchive getBeanDeploymentArchive(final Class<?> beanClass) {
        ClassLoader moduleClassLoader = WildFlySecurityManager.getClassLoaderPrivileged(beanClass);
        if (moduleClassLoader != null) {
            for (BeanDeploymentArchiveImpl bda : beanDeploymentArchives) {
                if (bda.getBeanClasses().contains(beanClass.getName()) && moduleClassLoader.equals(bda.getClassLoader())) {
                    return bda;
                }
            }
        }
        /*
         * We haven't found this class in a bean archive so probably it was added by an extension and the class itself does
         * not come from a BDA. Let's try to find an existing BDA that uses the same classloader
         * (and thus has the required accessibility to other BDAs)
         */
        if (moduleClassLoader != null && additionalBeanDeploymentArchivesByClassloader.containsKey(moduleClassLoader)) {
            return additionalBeanDeploymentArchivesByClassloader.get(moduleClassLoader);
        }
        return null;
    }
}
