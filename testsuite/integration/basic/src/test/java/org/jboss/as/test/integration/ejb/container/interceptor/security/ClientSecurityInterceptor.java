/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.container.interceptor.security;

import java.security.Principal;
import java.util.Map;

import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.jboss.security.SecurityContextAssociation;

/**
 * Client side interceptor responsible for propagating the local identity.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Josef Cacek
 */
public class ClientSecurityInterceptor implements EJBClientInterceptor {

    /**
     * Sets the current principal name to the invocation context. Principal name is stored to the context under the
     * {@link ServerSecurityInterceptor#DELEGATED_USER_KEY} key.
     *
     * @param context
     * @throws Exception
     * @see org.jboss.ejb.client.EJBClientInterceptor#handleInvocation(org.jboss.ejb.client.EJBClientInvocationContext)
     */
    public void handleInvocation(EJBClientInvocationContext context) throws Exception {
        Principal currentPrincipal = SecurityContextAssociation.getPrincipal();

        if (currentPrincipal != null) {
            Map<String, Object> contextData = context.getContextData();
            contextData.put(ServerSecurityInterceptor.DELEGATED_USER_KEY, currentPrincipal.getName());
        }

        context.sendRequest();
    }

    /**
     * Simply returns the invocation result.
     */
    public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
        return context.getResult();
    }

}
