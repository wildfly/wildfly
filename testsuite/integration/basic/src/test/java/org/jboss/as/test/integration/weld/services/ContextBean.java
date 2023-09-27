/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.RequestScoped;
import jakarta.xml.ws.WebServiceContext;
import java.security.Principal;

@RequestScoped
public class ContextBean {

    @Resource
    private WebServiceContext context;

    private String name;

    @PostConstruct
    private void postConstruct() {
        if (context != null) {
            Principal principal = context.getUserPrincipal();
            name = (principal == null) ? "anonymous" : principal.getName();
        }
    }

    public String getName() {
        return this.name;
    }
}
