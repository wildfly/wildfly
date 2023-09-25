/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runas.propagation;

import org.jboss.ejb3.annotation.SecurityDomain;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.Remote;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

/**
 * @author tmiyar
 *
 */
@Stateless
@Remote(CallerInRole.class)
@SecurityDomain("ejbtest-domain")
@PermitAll
public class ServerCallerInRole implements CallerInRole {

    @Resource
    private SessionContext context;

    @Override
    public Boolean isCallerInRole(String role) {
        return context.isCallerInRole(role);
    }

}
