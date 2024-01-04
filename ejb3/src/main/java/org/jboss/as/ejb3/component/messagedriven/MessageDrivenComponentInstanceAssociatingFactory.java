/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.messagedriven;

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
public class MessageDrivenComponentInstanceAssociatingFactory extends ComponentInterceptorFactory {

    private static final MessageDrivenComponentInstanceAssociatingFactory INSTANCE = new MessageDrivenComponentInstanceAssociatingFactory();

    private MessageDrivenComponentInstanceAssociatingFactory() {

    }

    public static MessageDrivenComponentInstanceAssociatingFactory instance() {
        return INSTANCE;
    }

    @Override
    protected Interceptor create(Component component, InterceptorFactoryContext context) {
        if (component instanceof MessageDrivenComponent == false) {
            throw EjbLogger.ROOT_LOGGER.unexpectedComponent(component, MessageDrivenComponent.class);
        }
        final MessageDrivenComponent mdbComponent = (MessageDrivenComponent) component;
        if (mdbComponent.getPool() != null) {
            return PooledInstanceInterceptor.INSTANCE;
        } else {
            return NonPooledEJBComponentInstanceAssociatingInterceptor.INSTANCE;
        }
    }
}
