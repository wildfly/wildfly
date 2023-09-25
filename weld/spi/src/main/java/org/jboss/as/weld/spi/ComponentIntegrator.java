/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.spi;

import java.util.function.Supplier;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * Jakarta EE component integrator.
 *
 * @author Martin Kouba
 */
public interface ComponentIntegrator {

    /**
     * Probably just for Jakarta Enterprise Beans.
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
