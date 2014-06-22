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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.as.weld.WeldModuleResourceLoader;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl.BeanArchiveType;
import org.jboss.as.weld.discovery.WeldAnnotationDiscovery;
import org.jboss.as.weld.services.bootstrap.ProxyServicesImpl;
import org.jboss.modules.Module;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.CDI11Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.resources.spi.AnnotationDiscovery;
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

    private final BeanDeploymentModule rootBeanDeploymentModule;

    public WeldDeployment(Set<BeanDeploymentArchiveImpl> beanDeploymentArchives, Collection<Metadata<Extension>> extensions,
            Module module, Set<ClassLoader> subDeploymentClassLoaders, DeploymentUnit deploymentUnit, BeanDeploymentModule rootBeanDeploymentModule) {
        this.subDeploymentClassLoaders = new HashSet<ClassLoader>(subDeploymentClassLoaders);
        this.beanDeploymentArchives = new HashSet<BeanDeploymentArchiveImpl>(beanDeploymentArchives);
        this.extensions = new HashSet<Metadata<Extension>>(extensions);
        this.serviceRegistry = new SimpleServiceRegistry();
        this.additionalBeanDeploymentArchivesByClassloader = new HashMap<ClassLoader, BeanDeploymentArchiveImpl>();
        this.module = module;
        this.rootBeanDeploymentModule = rootBeanDeploymentModule;

        // add static services
        this.serviceRegistry.add(ProxyServices.class, new ProxyServicesImpl(module));
        this.serviceRegistry.add(ResourceLoader.class, new WeldModuleResourceLoader(module));

        CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (index != null) {
            this.serviceRegistry.add(AnnotationDiscovery.class, new WeldAnnotationDiscovery(index));
        }

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
        /*
         * No, there is no BDA for the class yet. Let's create one.
         */
        return createAndRegisterAdditionalBeanDeploymentArchive(beanClass);
    }

    protected BeanDeploymentArchiveImpl createAndRegisterAdditionalBeanDeploymentArchive(Class<?> beanClass) {
        Module module = Module.forClass(beanClass);
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
        newBda.getServices().addAll(serviceRegistry.entrySet());
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

        additionalBeanDeploymentArchivesByClassloader.put(beanClass.getClassLoader(), newBda);
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
    public synchronized BeanDeploymentArchive getBeanDeploymentArchive(final Class<?> beanClass) {
        ClassLoader moduleClassLoader = WildFlySecurityManager.getClassLoaderPrivileged(beanClass);
        for (BeanDeploymentArchiveImpl bda : beanDeploymentArchives) {
            if (bda.getBeanClasses().contains(beanClass.getName()) && moduleClassLoader != null && moduleClassLoader.equals(beanClass.getClassLoader())) {
                return bda;
            }
        }
        /*
         * We haven't found this class in a bean archive so probably it was added by an extension and the class itself does
         * not come from a BDA. Let's try to find an existing BDA that uses the same classloader
         * (and thus has the required accessibility to other BDAs)
         */
        if (additionalBeanDeploymentArchivesByClassloader.containsKey(moduleClassLoader)) {
            return additionalBeanDeploymentArchivesByClassloader.get(moduleClassLoader);
        }
        return null;
    }
}
