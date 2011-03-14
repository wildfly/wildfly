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
package org.jboss.as.weld.services.bootstrap;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.naming.ContextNames;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.weld.injection.spi.EjbInjectionServices;

import javax.ejb.EJB;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.reflect.Method;

/**
 * Implementation of EjbInjectionServices.
 *
 * @author Stuart Douglas
 *
 */
public class WeldEjbInjectionServices implements EjbInjectionServices {

    private final ServiceRegistry serviceRegistry;

    private final EEModuleDescription moduleDescription;


    public WeldEjbInjectionServices(ServiceRegistry serviceRegistry, EEModuleDescription moduleDescription) {
        this.serviceRegistry = serviceRegistry;
        this.moduleDescription = moduleDescription;
    }

    @Override
    public Object resolveEjb(InjectionPoint injectionPoint) {
        //TODO: some of this stuff should be cached
        EJB ejb = injectionPoint.getAnnotated().getAnnotation(EJB.class);
        if(ejb == null) {
            throw new RuntimeException("@Ejb annotation not found on " + injectionPoint.getMember());
        }
        if (injectionPoint.getMember() instanceof Method && ((Method) injectionPoint.getMember()).getParameterTypes().length != 1)
        {
            throw new IllegalArgumentException("Injection point represents a method which doesn't follow JavaBean conventions (must have exactly one parameter) " + injectionPoint);
        }
        if(!ejb.lookup().equals("")) {
            final ServiceName ejbServiceName = ContextNames.serviceNameOfContext(moduleDescription.getAppName(),moduleDescription.getModuleName(),moduleDescription.getModuleName(),ejb.lookup());
            ServiceController<?> controller =  serviceRegistry.getRequiredService(ejbServiceName);
            ManagedReferenceFactory factory = (ManagedReferenceFactory) controller.getValue();
            return factory.getReference().getInstance();
        } else {
            //TODO: hook in the ejb resolver, when it exists
            throw new RuntimeException("Currently only the lookup attribute is supported on CDI @EJB injection " + injectionPoint);
        }
    }

    @Override
    public void cleanup() {

    }
}
