/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.security;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInterceptorFactory;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.wildfly.security.auth.server.SecurityDomain;

/**
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class SecurityDomainInterceptorFactory extends ComponentInterceptorFactory {

    public static final InterceptorFactory INSTANCE = new SecurityDomainInterceptorFactory();

    private static final String DEFAULT_DOMAIN = "other";

    @Override
    protected Interceptor create(final Component component, final InterceptorFactoryContext context) {
        if (! (component instanceof EJBComponent)) {
            throw EjbLogger.ROOT_LOGGER.unexpectedComponent(component, EJBComponent.class);
        }

        final EJBComponent ejbComponent = (EJBComponent) component;
        final EJBSecurityMetaData securityMetaData = ejbComponent.getSecurityMetaData();

        String securityDomainName =  securityMetaData.getSecurityDomainName();
        if (securityDomainName == null) {
            securityDomainName = DEFAULT_DOMAIN;
        }

        final SecurityDomain securityDomain = ejbComponent.getSecurityDomain();
        if (securityDomain == null) {
            throw EjbLogger.ROOT_LOGGER.invalidSecurityForDomainSet(ejbComponent.getComponentName());
        }
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.trace("Using security domain: " + securityDomainName + " for EJB " + ejbComponent.getComponentName());
        }
        return new SecurityDomainInterceptor(securityDomain);
    }
}
