/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
     * Returns a {@link javax.ejb.MessageDrivenContext}
     * @return
     */
    @Override
    public EJBContextImpl getEjbContext() {
        return messageDrivenContext;
    }
}
