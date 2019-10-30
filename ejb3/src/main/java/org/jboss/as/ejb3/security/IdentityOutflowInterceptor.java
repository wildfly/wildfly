/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import java.util.Set;
import java.util.function.Function;

import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.authz.RoleMapper;

/**
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
final class IdentityOutflowInterceptor implements Interceptor {

    private final Function<SecurityIdentity, Set<SecurityIdentity>> identityOutflowFunction;
    private final String category;
    private final RoleMapper roleMapper;

    IdentityOutflowInterceptor(final Function<SecurityIdentity, Set<SecurityIdentity>> identityOutflowFunction) {
        this(identityOutflowFunction, null, null);
    }

    IdentityOutflowInterceptor(final Function<SecurityIdentity, Set<SecurityIdentity>> identityOutflowFunction, final String category, final RoleMapper roleMapper) {
        this.identityOutflowFunction = identityOutflowFunction;
        this.category = category;
        this.roleMapper = roleMapper;
    }

    public Object processInvocation(final InterceptorContext context) throws Exception {
        if (identityOutflowFunction != null) {
            final SecurityDomain securityDomain = context.getPrivateData(SecurityDomain.class);
            final SecurityIdentity currentIdentity = securityDomain.getCurrentSecurityIdentity();
            Set<SecurityIdentity> outflowedIdentities = identityOutflowFunction.apply(currentIdentity);
            SecurityIdentity[] newIdentities;

            if (category != null && roleMapper != null) {
                // Propagate the runAsRole or any extra principal roles that are configured
                // (TODO: ensure this is the desired behaviour)
                newIdentities = outflowedIdentities.stream().map(outflowedIdentity -> {
                            final RoleMapper mergeMapper = roleMapper.or((roles) -> outflowedIdentity.getRoles(category));
                            return outflowedIdentity.withRoleMapper(category, mergeMapper);
                        }).toArray(SecurityIdentity[]::new);

            } else {
                newIdentities = outflowedIdentities.toArray(new SecurityIdentity[outflowedIdentities.size()]);
            }
            return SecurityIdentity.runAsAll(context, newIdentities);
        } else {
            return context.proceed();
        }
    }
}
