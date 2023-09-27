/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.jbossappxml;

import org.jboss.ejb3.annotation.RunAsPrincipal;

import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * Simple EJB3 bean that calls another bean with a run as
 *
 * @author anil saldhana
 */
@RunAs("Employee")
@RunAsPrincipal("javajoe")
@Stateless
@Remote(BeanInterface.class)
public class FirstBean implements BeanInterface {

    @EJB
    private SecondBean secondBean;

    public String getCallerPrincipal() {
        return secondBean.getCallerPrincipal();
    }

    @Override
    public boolean isCallerInRole(String roleName) {
        return secondBean.isCallerInRole(roleName);
    }
}
