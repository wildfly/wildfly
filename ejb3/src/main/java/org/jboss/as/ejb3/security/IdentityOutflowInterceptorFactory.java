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

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInterceptorFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.authz.RoleMapper;

/**
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class IdentityOutflowInterceptorFactory extends ComponentInterceptorFactory {

    public static final InterceptorFactory INSTANCE = new IdentityOutflowInterceptorFactory();

    private final String category;
    private final RoleMapper roleMapper;

    public IdentityOutflowInterceptorFactory() {
        this(null, null);
    }

    public IdentityOutflowInterceptorFactory(final String category, final RoleMapper roleMapper) {
        this.category = category;
        this.roleMapper = roleMapper;
    }

    @Override
    protected Interceptor create(final Component component, final InterceptorFactoryContext context) {
        if (! (component instanceof EJBComponent)) {
            throw EjbLogger.ROOT_LOGGER.unexpectedComponent(component, EJBComponent.class);
        }

        final EJBComponent ejbComponent = (EJBComponent) component;
        final Function<SecurityIdentity, Set<SecurityIdentity>> identityOutflowFunction = ejbComponent.getIdentityOutflowFunction();
        return new IdentityOutflowInterceptor(identityOutflowFunction, category, roleMapper);
    }
}
