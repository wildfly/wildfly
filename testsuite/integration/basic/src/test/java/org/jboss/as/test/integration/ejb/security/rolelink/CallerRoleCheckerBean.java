/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.rolelink;

import jakarta.annotation.Resource;
import jakarta.ejb.LocalBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * @author Jaikiran Pai
 */
@Stateless
@LocalBean
@SecurityDomain(value = CallerRoleCheckerBean.SECURITY_DOMAIN_NAME)
public class CallerRoleCheckerBean {

    public static final String SECURITY_DOMAIN_NAME = "security-link-test-security-domain";

    @Resource
    private SessionContext sessionContext;

    public boolean isCallerInRole(final String role) {
        return this.sessionContext.isCallerInRole(role);
    }
}
