/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.weld.spi;

import java.util.function.Supplier;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * Java EE component integrator.
 *
 * @author Martin Kouba
 */
public interface ComponentIntegrator {

    /**
     * Probably just for EJBs.
     *
     * @return <code>true</code> if the given description requires a bean name, <code>false</code> otherwise
     */
    boolean isBeanNameRequired(ComponentDescription description);

    /**
     *
     * @return <code>true</code> if the description represents a component with view, <code>false</code> otherwise
     */
    boolean isComponentWithView(ComponentDescription description);

    /**
     *
     * @param beanManagerServiceName
     * @param configuration
     * @param description
     * @param weldComponentServiceBuilder
     * @param bindingServiceName
     * @param integrationAction
     * @param interceptorSupport
     * @return <code>true</code> if an integration was performed, <code>false</code> otherwise
     */
    boolean integrate(ServiceName beanManagerServiceName, ComponentConfiguration configuration, ComponentDescription description,
            ServiceBuilder<?> weldComponentServiceBuilder, Supplier<ServiceName> bindingServiceNameSupplier,
            DefaultInterceptorIntegrationAction integrationAction, ComponentInterceptorSupport interceptorSupport);

    /**
     * NOTE: If performed, exactly one implementation of {@link ComponentInterceptorSupport} must be available.
     */
    @FunctionalInterface
    interface DefaultInterceptorIntegrationAction {

        void perform(ServiceName bindingServiceName);

    }

}
