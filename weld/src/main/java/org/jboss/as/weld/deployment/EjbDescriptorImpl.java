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

import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateless.StatelessComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceName;
import org.jboss.weld.ejb.spi.BusinessInterfaceDescriptor;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.resources.spi.ResourceLoader;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of EjbDescriptor
 * @author Stuart Douglas
 */
public class EjbDescriptorImpl<T> implements EjbDescriptor<T>{

    private final EJBComponentDescription componentDescription;
    private final BeanDeploymentArchiveImpl beanDeploymentArchive;
    private final Set<BusinessInterfaceDescriptor<?>> localInterfaces;
    private final ServiceName baseName;

    public EjbDescriptorImpl(EJBComponentDescription componentDescription, BeanDeploymentArchiveImpl beanDeploymentArchive,DeploymentUnit deploymentUnit) {
        this.componentDescription = componentDescription;
        this.beanDeploymentArchive = beanDeploymentArchive;
        final Set<BusinessInterfaceDescriptor<?>> localInterfaces = new HashSet<BusinessInterfaceDescriptor<?>>();
        for(String className : componentDescription.getViewClassNames()) {
            localInterfaces.add(new BusinessInterfaceDescriptorImpl<Object>(beanDeploymentArchive,className));
        }
        this.localInterfaces=localInterfaces;
        this.baseName =  deploymentUnit.getServiceName().append("component").append(componentDescription.getComponentName());
    }


    @Override
    public Class<T> getBeanClass() {
        return (Class<T>) beanDeploymentArchive.getServices().get(ResourceLoader.class).classForName(componentDescription.getEJBClassName());
    }

    @Override
    public Collection<BusinessInterfaceDescriptor<?>> getLocalBusinessInterfaces() {
        return localInterfaces;
    }

    @Override
    public Collection<BusinessInterfaceDescriptor<?>> getRemoteBusinessInterfaces() {
        return Collections.emptySet();
    }

    @Override
    public String getEjbName() {
        return componentDescription.getEJBName();
    }

    @Override
    public Collection<Method> getRemoveMethods() {
        //TODO: fix this
        return Collections.emptySet();
    }

    @Override
    public boolean isStateless() {
        return componentDescription instanceof StatelessComponentDescription;
    }

    @Override
    public boolean isSingleton() {
        return componentDescription instanceof SingletonComponentDescription;
    }

    @Override
    public boolean isStateful() {
        return componentDescription instanceof StatefulComponentDescription;
    }

    @Override
    public boolean isMessageDriven() {
        //TODO: message driven beans
        return false;
    }

    public EJBComponentDescription getComponentDescription() {
        return componentDescription;
    }

    public ServiceName getBaseName() {
        return baseName;
    }

    public ServiceName getCreateServiceName() {
        return baseName.append("CREATE");
    }

    public ServiceName getStartServiceName() {
        return baseName.append("START");
    }

}
