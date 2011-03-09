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
package org.jboss.as.weld.injection;

import org.jboss.as.ee.component.AbstractComponentConfiguration;
import org.jboss.as.ee.component.ComponentInjector;
import org.jboss.as.ee.component.InjectionFactory;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.weld.services.BeanManagerService;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * Creates weld injectors.
 * <p/>
 * This does not use
 * {@link javax.enterprise.inject.spi.BeanManager#createInjectionTarget(javax.enterprise.inject.spi.AnnotatedType)}
 * as the resulting injection point will take over all resource injection points, as well as CDI
 * Inject injection points. As this will only be used on EE component classes that already support resource
 * injection points, this behaviour is not desirable.
 *
 *
 */
public class WeldInjectionFactory implements InjectionFactory{

    private final ServiceName beanManagerServiceName;
    private final ServiceTarget serviceTarget;
    private final DeploymentUnit deploymentUnit;
    private final ClassLoader classLoader;

    public WeldInjectionFactory(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit,ClassLoader classLoader) {
        this.serviceTarget = serviceTarget;
        this.deploymentUnit = deploymentUnit;
        this.beanManagerServiceName = deploymentUnit.getServiceName().append(BeanManagerService.NAME);
        this.classLoader = classLoader;
    }


    /**
     * We have to create an injector for every class, as the class may
     * have @Inject annotations added through the SPI.
     * @param component The component to generate an injector for
     * @return a WeldInjector for the component
     */
    @Override
    public ComponentInjector createInjector(AbstractComponentConfiguration component) {
        final ServiceName serviceName = deploymentUnit.getServiceName().append("component",component.getComponentName(),"weldinjector");
        WeldComponentInjectionService service = new WeldComponentInjectionService(serviceName,component.getComponentClass(),classLoader);
        serviceTarget.addService(serviceName,service)
                .addDependency(beanManagerServiceName, BeanManagerImpl.class, service.getBeanManager())
                .install();
        return service;
    }
}
