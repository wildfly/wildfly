/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse.trust;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.Remote;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

@Stateless
@PermitAll
@Remote(ContextProvider.class)
public class ContextProviderBean implements ContextProvider{

     @Resource SessionContext context;

     public String getEjbCallerPrincipalName() {
        return context.getCallerPrincipal().getName();
    }
}
