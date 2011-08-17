/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.invocation.InterceptorContext;

/**
 * A {@link ComponentInstance} associating interceptor for EJB components (SLSB and message driven) which
 * have pooling disabled. Upon each {@link #processInvocation(org.jboss.invocation.InterceptorContext) invocation}
 * this interceptor creates a new {@link ComponentInstance} and associates it with the invocation. It then
 * {@link org.jboss.as.ee.component.ComponentInstance#destroy() destroys} the instance upon method completion.
 * <p/>
 * User: Jaikiran Pai
 */
public class NonPooledEJBComponentInstanceAssociatingInterceptor extends AbstractEJBInterceptor {

    public static final NonPooledEJBComponentInstanceAssociatingInterceptor INSTANCE = new NonPooledEJBComponentInstanceAssociatingInterceptor();

    private NonPooledEJBComponentInstanceAssociatingInterceptor() {

    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final EJBComponent component = getComponent(context, EJBComponent.class);
        // create the instance
        final ComponentInstance componentInstance = component.createInstance();
        context.putPrivateData(ComponentInstance.class, componentInstance);
        try {
            return context.proceed();
        } finally {
            context.putPrivateData(ComponentInstance.class, null);
            // destroy the instance
            componentInstance.destroy();
        }
    }

}
