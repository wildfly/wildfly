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
@SecurityDomain("BusinessDomain")
public class TargetBean {

    @Resource
    private SessionContext context;

    @PermitAll
    public boolean isCallerInRole(String role) {
        //new Throwable().printStackTrace(); //rls
        return context.isCallerInRole(role);
    }

    @PermitAll
    public String getCallerPrincipal() {
        //new Throwable().printStackTrace(); //rls
        Principal caller = context.getCallerPrincipal();
        return caller.getName();
    }

    // rls added
    @PermitAll
    public String getSessionContext() {
        return  ""+ context;
    }
}
