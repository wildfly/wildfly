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
import org.jboss.as.weld.WeldModuleResourceLoader;
import org.jboss.as.weld.discovery.WeldAnnotationDiscovery;
import org.jboss.as.weld.services.bootstrap.ProxyServicesImpl;
import org.jboss.modules.Module;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.resources.spi.AnnotationDiscovery;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.serialization.spi.ProxyServices;

/**
 * Abstract implementation of {@link Deployment}.
 * <p>
 * Thread safety: This class is thread safe, and does not require a happens before action between construction and usage
 *
 * @author Stuart Douglas
 *
 */
public class WeldDeployment implements Deployment {

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

    /**
     * Maps class names to bean archives.
     *
     * The spec does not allow for the same class to be deployed in multiple bean archives
     */
    private final Map<String, BeanDeploymentArchiveImpl> beanDeploymentsByClassName;

    private final Map<ClassLoader, BeanDeploymentArchiveImpl> additionalBeanDeploymentArchivesByClassloader;

    public WeldDeployment(Set<BeanDeploymentArchiveImpl> beanDeploymentArchives, Collection<Metadata<Extension>> extensions,
            Module module, Set<ClassLoader> subDeploymentClassLoaders, DeploymentUnit deploymentUnit) {
        this.subDeploymentClassLoaders = new HashSet<ClassLoader>(subDeploymentClassLoaders);
        this.beanDeploymentArchives = new HashSet<BeanDeploymentArchiveImpl>(beanDeploymentArchives);
        this.extensions = new HashSet<Metadata<Extension>>(extensions);
        this.serviceRegistry = new SimpleServiceRegistry();
        this.beanDeploymentsByClassName = new HashMap<String, BeanDeploymentArchiveImpl>();
        this.additionalBeanDeploymentArchivesByClassloader = new HashMap<ClassLoader, BeanDeploymentArchiveImpl>();
        this.module = module;

        // add static services
        this.serviceRegistry.add(ProxyServices.class, new ProxyServicesImpl(module));
        this.serviceRegistry.add(ResourceLoader.class, new WeldModuleResourceLoader(module));

        CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (index != null) {
            this.serviceRegistry.add(AnnotationDiscovery.class, new WeldAnnotationDiscovery(index));
        }

        // set up the additional bean archives accessibility rules
        // and map class names to bean deployment archives
        for (BeanDeploymentArchiveImpl bda : beanDeploymentArchives) {
            for (String className : bda.getBeanClasses()) {
                beanDeploymentsByClassName.put(className, bda);
            }
        }

        calculateAccessibilityGraph(this.beanDeploymentArchives);
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
    public synchronized BeanDeploymentArchive loadBeanDeploymentArchive(Class<?> beanClass) {
        if (beanDeploymentsByClassName.containsKey(beanClass.getName())) {
            return beanDeploymentsByClassName.get(beanClass.getName());
        }
        /*
         * We haven't found this class in a bean archive so apparently it was added by an extension and the class itself does
         * not come from a BDA. Before we create a new BDA, let's try to find an existing BDA that uses the same classloader
         * (and thus has the required accessibility to other BDAs)
         */
        if (additionalBeanDeploymentArchivesByClassloader.containsKey(beanClass.getClassLoader())) {
            return additionalBeanDeploymentArchivesByClassloader.get(beanClass.getClassLoader());
        }
        /*
         * No, there is no BDA for the class' classloader yet. Let's create one.
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
                BeansXml.EMPTY_BEANS_XML, module, id, false);
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
        beanDeploymentsByClassName.put(beanClass.getName(), newBda);
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
}
