/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
