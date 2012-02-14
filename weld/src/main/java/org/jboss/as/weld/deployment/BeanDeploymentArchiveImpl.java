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
import org.jboss.modules.Module;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.resources.spi.ResourceLoader;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Implementation of {@link BeanDeploymentArchive}.
 * <p>
 * Thread Safety: This class is thread safe and does not require a happens before action between construction and usage
 *
 * @author Stuart Douglas
 *
 */
public class BeanDeploymentArchiveImpl implements BeanDeploymentArchive {

    private final Set<String> beanClasses;

    private final Set<BeanDeploymentArchive> beanDeploymentArchives;

    private final BeansXml beansXml;

    private final String id;

    private final ServiceRegistry serviceRegistry;

    private final Module module;

    private final WeldModuleResourceLoader resourceLoader;

    private final Set<EjbDescriptor<?>> ejbDescriptors;

    public BeanDeploymentArchiveImpl(Set<String> beanClasses, BeansXml beansXml, Module module, String id) {
        this.beanClasses = new ConcurrentSkipListSet<String>(beanClasses);
        this.beanDeploymentArchives = new CopyOnWriteArraySet<BeanDeploymentArchive>();
        this.beansXml = beansXml;
        this.id = id;
        this.serviceRegistry = new SimpleServiceRegistry();
        this.resourceLoader = new WeldModuleResourceLoader(module);
        this.serviceRegistry.add(ResourceLoader.class, resourceLoader);
        this.module = module;
        this.ejbDescriptors = new HashSet<EjbDescriptor<?>>();
    }

    /**
     * Adds an accessible {@link BeanDeploymentArchive}.
     */
    public void addBeanDeploymentArchive(BeanDeploymentArchive archive) {
        if (archive == this) {
            return;
        }
        beanDeploymentArchives.add(archive);
    }

    /**
     * Adds multiple accessible {@link BeanDeploymentArchive}s
     */
    public void addBeanDeploymentArchives(Collection<? extends BeanDeploymentArchive> archives) {
        for (BeanDeploymentArchive bda : archives) {
            if (bda != this) {
                beanDeploymentArchives.add(bda);
            }
        }
    }

    public void addBeanClass(String clazz) {
        this.beanClasses.add(clazz);
    }

    public void addBeanClass(Class<?> clazz) {
        this.resourceLoader.addAdditionalClass(clazz);
    }

    /**
     * returns an unmodifiable copy of the bean classes in this BDA
     */
    @Override
    public Collection<String> getBeanClasses() {
        return Collections.unmodifiableSet(new HashSet<String>(beanClasses));
    }

    /**
     * Returns an unmodifiable copy of the bean deployment archives set
     */
    @Override
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return Collections.unmodifiableCollection(new HashSet<BeanDeploymentArchive>(beanDeploymentArchives));
    }

    @Override
    public BeansXml getBeansXml() {
        return beansXml;
    }


    public void addEjbDescriptor(EjbDescriptor<?> descriptor) {
        ejbDescriptors.add(descriptor);
    }

    @Override
    public Collection<EjbDescriptor<?>> getEjbs() {
        return Collections.unmodifiableSet(ejbDescriptors);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ServiceRegistry getServices() {
        return serviceRegistry;
    }

    public Module getModule() {
        return module;
    }
}
