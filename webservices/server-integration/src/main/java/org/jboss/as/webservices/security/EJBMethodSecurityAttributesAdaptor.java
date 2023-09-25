/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.security;

import java.lang.reflect.Method;
import java.util.Set;

import org.jboss.as.ejb3.security.service.EJBViewMethodSecurityAttributesService;
import org.jboss.wsf.spi.security.EJBMethodSecurityAttribute;

/**
 * Adaptor of Enterprise Beans 3 EJBViewMethodSecurityAttributesService to JBossWS SPI EJBMethodSecurityAttributeProvider
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
