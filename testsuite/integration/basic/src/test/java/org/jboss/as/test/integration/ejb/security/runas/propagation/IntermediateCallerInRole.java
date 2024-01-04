/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runas.propagation;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.RunAsPrincipal;
import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * @author tmiyar
 */
@Stateless
@Remote(IntermediateCallerInRoleRemote.class)
@SecurityDomain("ejbtest-domain")
@PermitAll
@RunAsPrincipal("user2")
@RunAs("role2")
public class IntermediateCallerInRole implements IntermediateCallerInRoleRemote {

    @Resource
    private SessionContext context;

    @EJB(lookup = "ejb:/ejb-security-context-propagation/ServerCallerInRole!org.jboss.as.test.integration.ejb.security.runas.propagation.CallerInRole")
    private CallerInRole callerInRole;

    @Override
    public Boolean isCallerInRole(String role) {
        return context.isCallerInRole(role);
    }

    @Override
    public Boolean isServerCallerInRole(String role) {
        return callerInRole.isCallerInRole(role);
    }

}
