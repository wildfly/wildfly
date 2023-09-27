/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.messagedriven;

import java.lang.reflect.Method;
import java.util.Map;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ejb3.component.EjbComponentInstance;
import org.jboss.as.ejb3.context.EJBContextImpl;
import org.jboss.as.ejb3.context.MessageDrivenContext;
import org.jboss.invocation.Interceptor;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MessageDrivenComponentInstance extends EjbComponentInstance {

    private final MessageDrivenContext messageDrivenContext;

    /**
     * Construct a new instance.
     *
     * @param component the component
     */
    public MessageDrivenComponentInstance(final BasicComponent component, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors) {
        super(component, preDestroyInterceptor, methodInterceptors);
        this.messageDrivenContext = new MessageDrivenContext(this);
    }


    @Override
    public MessageDrivenComponent getComponent() {
        return (MessageDrivenComponent) super.getComponent();
    }

    /**
     * Returns a {@link jakarta.ejb.MessageDrivenContext}
     * @return
     */
    @Override
    public EJBContextImpl getEjbContext() {
        return messageDrivenContext;
    }
}
