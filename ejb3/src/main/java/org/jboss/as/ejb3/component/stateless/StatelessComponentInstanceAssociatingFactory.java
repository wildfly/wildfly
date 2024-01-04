/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateless;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInterceptorFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.interceptors.NonPooledEJBComponentInstanceAssociatingInterceptor;
import org.jboss.as.ejb3.component.pool.PooledInstanceInterceptor;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * User: jpai
 */
public class StatelessComponentInstanceAssociatingFactory extends ComponentInterceptorFactory {

    private static final StatelessComponentInstanceAssociatingFactory INSTANCE = new StatelessComponentInstanceAssociatingFactory();

    private StatelessComponentInstanceAssociatingFactory() {

    }

    public static StatelessComponentInstanceAssociatingFactory instance() {
        return INSTANCE;
    }

    @Override
    protected Interceptor create(Component component, InterceptorFactoryContext context) {
        if (component instanceof StatelessSessionComponent == false) {
            throw EjbLogger.ROOT_LOGGER.unexpectedComponent(component, StatelessSessionComponent.class);
        }
        final StatelessSessionComponent statelessSessionComponent = (StatelessSessionComponent) component;
        if (statelessSessionComponent.getPool() != null) {
            return PooledInstanceInterceptor.INSTANCE;
        } else {
            return NonPooledEJBComponentInstanceAssociatingInterceptor.INSTANCE;
        }
    }
}
