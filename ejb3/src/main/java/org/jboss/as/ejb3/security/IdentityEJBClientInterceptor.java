/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.security;

import static org.jboss.as.ejb3.security.IdentityUtil.getCurrentSecurityDomain;

import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * An {@code EJBClientInterceptor} that will always runAs the current {@code SecurityIdentity}
 * to activate any outflow identities.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public final class IdentityEJBClientInterceptor implements EJBClientInterceptor {

    public static final IdentityEJBClientInterceptor INSTANCE = new IdentityEJBClientInterceptor();

    @Override
    public void handleInvocation(EJBClientInvocationContext context) throws Exception {
        final SecurityDomain securityDomain = getCurrentSecurityDomain();
        if (securityDomain != null) {
            SecurityIdentity identity = securityDomain.getCurrentSecurityIdentity();
            identity.runAsSupplierEx(() -> {
                context.sendRequest();
                return null;
            });
        } else {
            context.sendRequest();
        }
    }

    @Override
    public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
        return context.getResult();
    }

}
