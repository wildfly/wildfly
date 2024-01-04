/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.jbossappxml;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.LocalBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

/**
 * EJB3 bean that returns the current caller principal
 * @author anil saldhana
 */
@Stateless
@RolesAllowed("Employee")
@LocalBean
public class SecondBean implements BeanInterface{
    @Resource
    private SessionContext context;
    public String getCallerPrincipal(){
        return context.getCallerPrincipal().getName();
    }
    @Override
    public boolean isCallerInRole(String roleName) {
        return context.isCallerInRole(roleName);
    }
}
