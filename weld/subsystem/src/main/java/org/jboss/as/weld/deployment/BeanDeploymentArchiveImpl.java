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

import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jboss.as.weld.WeldModuleResourceLoader;
import org.jboss.as.weld.spi.WildFlyBeanDeploymentArchive;
import org.jboss.as.weld.util.Reflections;
import org.jboss.as.weld.util.ServiceLoaders;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleDependencySpec;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Implementation of {@link BeanDeploymentArchive}.
 * <p>
 * Thread Safety: This class is thread safe and does not require a happens before action between construction and usage
 *
 * @author Stuart Douglas
 *
 */
public class BeanDeploymentArchiveImpl implements WildFlyBeanDeploymentArchive {

    public enum BeanArchiveType {
        IMPLICIT, EXPLICIT, EXTERNAL, SYNTHETIC;
    }

    private final Set<String> beanClasses;

    private final Set<String> allKnownClasses;

    private final Set<BeanDeploymentArchive> beanDeploymentArchives;

    private final BeansXml beansXml;

    private final String id;

    private final ServiceRegistry serviceRegistry;

    private final Module module;

    private final WeldModuleResourceLoader resourceLoader;

    private final Set<EjbDescriptor<?>> ejbDescriptors;

    private final boolean root; // indicates whether this is a root BDA

    private final BeanArchiveType beanArchiveType;

    public BeanDeploymentArchiveImpl(Set<String> beanClasses, Set<String> allClasses, BeansXml beansXml, Module module, String id, BeanArchiveType beanArchiveType) {
        this(beanClasses, allClasses, beansXml, module, id, beanArchiveType, false);
    }

    public BeanDeploymentArchiveImpl(Set<String> beanClasses, Set<String> allClasses, BeansXml beansXml, Module module, String id, BeanArchiveType beanArchiveType, boolean root) {
        this.beanClasses = new ConcurrentSkipListSet<String>(beanClasses);
        this.allKnownClasses = new ConcurrentSkipListSet<String>(allClasses);
        this.beanDeploymentArchives = new CopyOnWriteArraySet<BeanDeploymentArchive>();
        this.beansXml = beansXml;
        this.id = id;
        this.serviceRegistry = new SimpleServiceRegistry();
        this.resourceLoader = new WeldModuleResourceLoader(module);
        this.serviceRegistry.add(ResourceLoader.class, resourceLoader);

        for (Entry<Class<? extends Service>, Service> entry : ServiceLoaders.loadBeanDeploymentArchiveServices(BeanDeploymentArchiveImpl.class, this)
                .entrySet()) {
            this.serviceRegistry.add(entry.getKey(), Reflections.cast(entry.getValue()));
        }

        this.module = module;
        this.ejbDescriptors = new HashSet<EjbDescriptor<?>>();
        this.beanArchiveType = beanArchiveType;
        this.root = root;
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
        this.allKnownClasses.add(clazz);
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

    public ClassLoader getClassLoader() {
        if (module != null) {
            if(WildFlySecurityManager.isChecking()) {
                return WildFlySecurityManager.doUnchecked(new PrivilegedAction<ClassLoader>() {
                    @Override
                    public ClassLoader run() {
                        return module.getClassLoader();
                    }
                });
            } else {
                return module.getClassLoader();
            }
        }
        return null;
    }

    public boolean isRoot() {
        return root;
    }

    /**
     * Determines if a class from this {@link BeanDeploymentArchiveImpl} instance can access a class in the
     * {@link BeanDeploymentArchive} instance represented by the specified <code>BeanDeploymentArchive</code> parameter
     * according to the Java EE class accessibility requirements.
     *
     * @param target
     * @return true if an only if a class this archive can access a class from the archive represented by the specified parameter
     */
    public boolean isAccessible(BeanDeploymentArchive target) {
        if (this == target) {
            return true;
        }

        BeanDeploymentArchiveImpl that = (BeanDeploymentArchiveImpl) target;

        if (that.getModule() == null) {
            /*
             * The target BDA is the bootstrap BDA - it bundles classes loaded by the bootstrap classloader.
             * Everyone can see the bootstrap classloader.
             */
            return true;
        }
        if (module == null) {
            /*
             * This BDA is the bootstrap BDA - it bundles classes loaded by the bootstrap classloader. We assume that a
             * bean whose class is loaded by the bootstrap classloader can only see other beans in the "bootstrap BDA".
             */
            return that.getModule() == null;
        }

        if (module.equals(that.getModule())) {
            return true;
        }

        // basic check whether the module is our dependency
        for (DependencySpec dependency : module.getDependencies()) {
            if (dependency instanceof ModuleDependencySpec) {
                ModuleDependencySpec moduleDependency = (ModuleDependencySpec) dependency;
                if (moduleDependency.getIdentifier().equals(that.getModule().getIdentifier())) {
                    return true;
                }

                // moduleDependency might be an alias - try to load it to get lined module
                Module module = loadModule(moduleDependency);
                if (module != null && module.getIdentifier().equals(that.getModule().getIdentifier())) {
                    return true;
                }
            }
        }

        /*
         * full check - we try to load a class from the target bean archive and check whether its module
         * is the same as the one of the bean archive
         * See WFLY-4250 for more info
         */
        Iterator<String> iterator = target.getBeanClasses().iterator();
        if (iterator.hasNext()) {
            Class<?> clazz = Reflections.loadClass(iterator.next(), module.getClassLoader());
            if (clazz != null) {
                Module classModule = Module.forClass(clazz);
                return classModule != null && classModule.equals(that.getModule());
            }
        }
        return false;
    }

    private Module loadModule(ModuleDependencySpec moduleDependency) {
        try {
            ModuleLoader moduleLoader = moduleDependency.getModuleLoader();
            if (moduleLoader == null) {
                return null;
            } else {
                return moduleLoader.loadModule(moduleDependency.getIdentifier());
            }
        } catch (ModuleLoadException e) {
            return null;
        }
    }

    public BeanArchiveType getBeanArchiveType() {
        return beanArchiveType;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.beanArchiveType.toString());
        builder.append(" BeanDeploymentArchive (");
        builder.append(this.id);
        builder.append(")");
        return builder.toString();
    }

    @Override
    public Collection<String> getKnownClasses(){
        return allKnownClasses;
    }
}
