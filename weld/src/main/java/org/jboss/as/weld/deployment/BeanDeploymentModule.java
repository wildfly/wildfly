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
import java.util.Set;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.spi.EEModuleDescriptor;
import org.jboss.weld.util.collections.ImmutableSet;

/**
 * A collection of Bean Deployment archives that share similar bean visibility.
 * <p/>
 * When the module is created all BDA's are given visibility to each other. They can then be given visibility to other bda's or
 * bean modules with one operation.
 *
 * @author Stuart Douglas
 *
 */
public class BeanDeploymentModule {

    private final Set<BeanDeploymentArchiveImpl> beanDeploymentArchives;
    private final EEModuleDescriptor moduleDescriptor;

    public BeanDeploymentModule(String moduleId, DeploymentUnit deploymentUnit, Collection<BeanDeploymentArchiveImpl> beanDeploymentArchives) {
        this.beanDeploymentArchives = ImmutableSet.copyOf(beanDeploymentArchives);
        for (BeanDeploymentArchiveImpl bda : beanDeploymentArchives) {
            bda.addBeanDeploymentArchives(beanDeploymentArchives);
        }
        this.moduleDescriptor = WeldEEModuleDescriptor.of(moduleId, deploymentUnit);
        if (moduleDescriptor != null) {
            addService(EEModuleDescriptor.class, moduleDescriptor);
        }
    }

    /**
     * Makes the {@link BeanDeploymentArchiveImpl} accessible to all bdas in the module
     *
     * @param archive The archive to make accessible
     */
    public synchronized void addBeanDeploymentArchive(BeanDeploymentArchiveImpl archive) {
        for (BeanDeploymentArchiveImpl bda : beanDeploymentArchives) {
            bda.addBeanDeploymentArchive(archive);
        }
    }

    /**
     * Makes all {@link BeanDeploymentArchiveImpl}s in the given module accessible to all bdas in this module
     *
     * @param module The module to make accessible
     */
    public synchronized void addBeanDeploymentModule(BeanDeploymentModule module) {
        for (BeanDeploymentArchiveImpl bda : beanDeploymentArchives) {
            bda.addBeanDeploymentArchives(module.beanDeploymentArchives);
        }
    }

    /**
     * Makes all {@link BeanDeploymentArchiveImpl}s in the given modules accessible to all bdas in this module
     *
     * @param modules The modules to make accessible
     */
    public synchronized void addBeanDeploymentModules(Collection<BeanDeploymentModule> modules) {
        for (BeanDeploymentArchiveImpl bda : beanDeploymentArchives) {
            for (BeanDeploymentModule bdm : modules) {
                bda.addBeanDeploymentArchives(bdm.beanDeploymentArchives);
            }
        }
    }

    /**
     * Adds a service to all bean deployment archives in the module
     * @param clazz The service type
     * @param service The service
     * @param <S> The service type
     */
    public synchronized <S extends Service> void addService(Class<S> clazz, S service) {
        for (BeanDeploymentArchiveImpl bda : beanDeploymentArchives) {
            bda.getServices().add(clazz,service);
        }
    }

    public Set<BeanDeploymentArchiveImpl> getBeanDeploymentArchives() {
        return beanDeploymentArchives;
    }

    public EEModuleDescriptor getModuleDescriptor() {
        return moduleDescriptor;
    }
}
