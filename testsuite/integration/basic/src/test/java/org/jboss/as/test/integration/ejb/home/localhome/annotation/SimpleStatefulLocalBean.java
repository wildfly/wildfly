/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.home.localhome.annotation;

import jakarta.annotation.Resource;
import jakarta.ejb.LocalHome;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;

import org.jboss.as.test.integration.ejb.home.localhome.SimpleLocalInterface;
import org.jboss.as.test.integration.ejb.home.localhome.SimpleStatefulLocalHome;

/**
 * @author Stuart Douglas
 */
@Stateful
@LocalHome(SimpleStatefulLocalHome.class)
public class SimpleStatefulLocalBean {

    @Resource
    private SessionContext sessionContext;

    private String message;

    public void ejbCreateSimple(String message) {
        this.message = message;
    }

    public void ejbCreateComplex(String first, String second) {
        this.message = first + " " + second;
    }

    public String sayHello() {
        return message;
    }

    public String otherMethod() {
        return ((SimpleLocalInterface) sessionContext.getEJBLocalObject()).sayHello();
    }
}
