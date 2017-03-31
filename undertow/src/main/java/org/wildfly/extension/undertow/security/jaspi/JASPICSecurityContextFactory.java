/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.undertow.security.jaspi;

import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.api.SecurityContextFactory;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.HttpServerExchange;

/**
 * <p>
 * A {@link io.undertow.security.api.SecurityContextFactory} implementation that creates {@link JASPICSecurityContext}
 * instances.
 * </p>
 */
public class JASPICSecurityContextFactory implements SecurityContextFactory {

    private final String securityDomain;

    /**
     * <p>
     * Creates an instance of {@code JASPICSecurityContextFactory} with the specified security domain.
     * </p>
     *
     * @param securityDomain the security domain that is to be set in all created {@link JASPICSecurityContext} instances.
     */
    public JASPICSecurityContextFactory(final String securityDomain) {
        this.securityDomain = securityDomain;
    }

    @Override
    public SecurityContext createSecurityContext(final HttpServerExchange exchange, final AuthenticationMode mode,
           final IdentityManager identityManager, final String programmaticMechName) {
        JASPICSecurityContext context = new JASPICSecurityContext(exchange, mode, identityManager, this.securityDomain);
        if (programmaticMechName != null)
            context.setProgramaticMechName(programmaticMechName);
        return context;
    }
}
