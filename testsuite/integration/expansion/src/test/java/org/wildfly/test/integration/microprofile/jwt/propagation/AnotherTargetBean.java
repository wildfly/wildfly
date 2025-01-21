/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.jwt.propagation;

import java.security.Principal;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;
/**
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@Stateless
@SecurityDomain("AnotherBusinessDomain")
public class AnotherTargetBean {


    @Resource
    private SessionContext context;

    @PermitAll
    public boolean isCallerInRole(String role) {
        return context.isCallerInRole(role);
    }

    @PermitAll
    public String getCallerPrincipal() {
        Principal caller = context.getCallerPrincipal();
        return caller.getName();
    }

}
