/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
