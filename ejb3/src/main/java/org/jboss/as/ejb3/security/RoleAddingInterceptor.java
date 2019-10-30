/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.security.PrivilegedActionException;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.common.Assert;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.authz.RoleMapper;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class RoleAddingInterceptor implements Interceptor {
    private final String category;
    private final RoleMapper roleMapper;

    public RoleAddingInterceptor(final String category, final RoleMapper roleMapper) {
        this.category = category;
        this.roleMapper = roleMapper;
    }

    public Object processInvocation(final InterceptorContext context) throws Exception {
        final SecurityDomain securityDomain = context.getPrivateData(SecurityDomain.class);
        Assert.checkNotNullParam("securityDomain", securityDomain);
        final SecurityIdentity currentIdentity = securityDomain.getCurrentSecurityIdentity();
        final RoleMapper mergeMapper = roleMapper.or((roles) -> currentIdentity.getRoles(category));
        final SecurityIdentity newIdentity = currentIdentity.withRoleMapper(category, mergeMapper);
        try {
            return newIdentity.runAs(context);
        } catch (PrivilegedActionException e) {
            Throwable cause = e.getCause();
            if(cause != null) {
                if(cause instanceof Exception) {
                    throw (Exception) cause;
                } else {
                    throw new RuntimeException(e);
                }
            } else {
                throw e;
            }
        }
    }
}
