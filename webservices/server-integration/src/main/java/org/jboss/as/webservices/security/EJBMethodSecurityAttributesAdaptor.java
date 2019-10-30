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
package org.jboss.as.webservices.security;

import java.lang.reflect.Method;
import java.util.Set;

import org.jboss.as.ejb3.security.service.EJBViewMethodSecurityAttributesService;
import org.jboss.wsf.spi.security.EJBMethodSecurityAttribute;

/**
 * Adaptor of ejb3 EJBViewMethodSecurityAttributesService to JBossWS SPI EJBMethodSecurityAttributeProvider
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 *
 */
public class EJBMethodSecurityAttributesAdaptor implements org.jboss.wsf.spi.security.EJBMethodSecurityAttributeProvider {

    private final EJBViewMethodSecurityAttributesService attributeService;

    public EJBMethodSecurityAttributesAdaptor(EJBViewMethodSecurityAttributesService attributeService) {
        this.attributeService = attributeService;
    }

    @Override
    public EJBMethodSecurityAttribute getSecurityAttributes(final Method viewMethod) {
        if (attributeService == null) {
            return null;
        }
        final org.jboss.as.ejb3.security.EJBMethodSecurityAttribute att = attributeService.getSecurityAttributes(viewMethod);
        return att == null ? null : new org.jboss.wsf.spi.security.EJBMethodSecurityAttribute() {
            @Override
            public boolean isPermitAll() {
                return att.isPermitAll();
            }

            @Override
            public boolean isDenyAll() {
                return att.isDenyAll();
            }

            @Override
            public Set<String> getRolesAllowed() {
                return att.getRolesAllowed();
            }
        };
    }

}
