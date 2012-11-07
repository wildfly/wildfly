/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.stateful;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;

/**
 * Interceptor that delegates calls to EJB 2.x remove methods to the component remove method interceptor chain
 *
 * @author Stuart Douglas
 */
public class StatefulRemoveDelegationInterceptor  implements Interceptor{

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new StatefulRemoveDelegationInterceptor());

    private StatefulRemoveDelegationInterceptor() {

    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        StatefulSessionComponentInstance instance = (StatefulSessionComponentInstance) context.getPrivateData(ComponentInstance.class);
        return instance.getEjb2XRemoveInterceptor().processInvocation(context);
    }
}
