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

        String securityDomainName =  securityMetaData.getSecurityDomain();
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
