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

import org.jboss.as.weld.WeldModuleResourceLoader;
import org.jboss.as.weld.services.bootstrap.ProxyServicesImpl;
import org.jboss.modules.Module;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.serialization.spi.ProxyServices;

import javax.enterprise.inject.spi.Extension;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    private final Set<BeanDeploymentArchiveImpl> beanDeploymentArchives;

    /**
     * The bean deployment archive used for classes added through the SPI that are not present in a existing bean archive
     */
    private final BeanDeploymentArchiveImpl additionalBeanDeploymentArchive;

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

    public WeldDeployment(Set<BeanDeploymentArchiveImpl> beanDeploymentArchives, Collection<Metadata<Extension>> extensions,
                          Module module, Set<ClassLoader> subDeploymentClassLoaders) {
        this.subDeploymentClassLoaders = new HashSet<ClassLoader>(subDeploymentClassLoaders);
        this.additionalBeanDeploymentArchive = new BeanDeploymentArchiveImpl(Collections.<String> emptySet(),
                BeansXml.EMPTY_BEANS_XML, module, getClass().getName() + ADDITIONAL_CLASSES_BDA_SUFFIX);

        this.beanDeploymentArchives = new HashSet<BeanDeploymentArchiveImpl>(beanDeploymentArchives);
        this.extensions = new HashSet<Metadata<Extension>>(extensions);
        this.serviceRegistry = new SimpleServiceRegistry();
        this.beanDeploymentsByClassName = new HashMap<String, BeanDeploymentArchiveImpl>();
        this.module = module;

        // add static services
        this.serviceRegistry.add(ProxyServices.class, new ProxyServicesImpl(module));
        this.serviceRegistry.add(ResourceLoader.class, new WeldModuleResourceLoader(module));

        // set up the additional bean archives accessibility rules
        // and map class names to bean deployment archives
        for (BeanDeploymentArchiveImpl bda : beanDeploymentArchives) {
            bda.addBeanDeploymentArchive(additionalBeanDeploymentArchive);
            for (String className : bda.getBeanClasses()) {
                beanDeploymentsByClassName.put(className, bda);
            }
        }
        additionalBeanDeploymentArchive.addBeanDeploymentArchives(this.beanDeploymentArchives);
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
        // if this is a class we have not seen before add it to the additional classes BDA
        additionalBeanDeploymentArchive.addBeanClass(beanClass);
        beanDeploymentsByClassName.put(beanClass.getName(), additionalBeanDeploymentArchive);
        return additionalBeanDeploymentArchive;
    }

    public BeanDeploymentArchiveImpl getAdditionalBeanDeploymentArchive() {
        return additionalBeanDeploymentArchive;
    }

    public Module getModule() {
        return module;
    }

    public Set<ClassLoader> getSubDeploymentClassLoaders() {
        return Collections.unmodifiableSet(subDeploymentClassLoaders);
    }
}
