/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment.processors;

import static org.jboss.as.weld.interceptors.Jsr299BindingsInterceptor.factory;

import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.InterceptionType;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ee.managedbean.component.ManagedBeanComponentDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.stateful.SerializedCdiInterceptorsKey;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.weld.spi.ComponentIntegrator;
import org.jboss.as.weld.spi.ComponentInterceptorSupport;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 *
 * @author Martin Kouba
 */
public class EjbComponentIntegrator implements ComponentIntegrator {

    @Override
    public boolean isBeanNameRequired(ComponentDescription description) {
        return description instanceof EJBComponentDescription;
    }

    @Override
    public boolean isComponentWithView(ComponentDescription description) {
        return (description instanceof EJBComponentDescription) || (description instanceof ManagedBeanComponentDescription);
    }

    @Override
    public boolean integrate(ServiceName beanManagerServiceName, ComponentConfiguration configuration, ComponentDescription description,
            ServiceBuilder<?> weldComponentServiceBuilder, Supplier<ServiceName> bindingServiceNameSupplier,
            DefaultInterceptorIntegrationAction integrationAction, ComponentInterceptorSupport interceptorSupport) {
        if (description instanceof EJBComponentDescription) {
            ServiceName bindingServiceName = bindingServiceNameSupplier.get();
            integrationAction.perform(bindingServiceName);
            if (description.isPassivationApplicable()) {
                configuration.addPrePassivateInterceptor(
                        factory(InterceptionType.PRE_PASSIVATE, weldComponentServiceBuilder, bindingServiceName, interceptorSupport),
                        InterceptorOrder.ComponentPassivation.CDI_INTERCEPTORS);
                configuration.addPostActivateInterceptor(
                        factory(InterceptionType.POST_ACTIVATE, weldComponentServiceBuilder, bindingServiceName, interceptorSupport),
                        InterceptorOrder.ComponentPassivation.CDI_INTERCEPTORS);
            }
            if (description instanceof StatefulComponentDescription) {
                // add a context key for weld interceptor replication
                configuration.getInterceptorContextKeys().add(SerializedCdiInterceptorsKey.class);
            }
            return true;
        } else if (description instanceof ManagedBeanComponentDescription) {
            integrationAction.perform(bindingServiceNameSupplier.get());
            return true;
        }
        return false;
    }

}
