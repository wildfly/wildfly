/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.home.localhome.annotation;

import jakarta.annotation.Resource;
import jakarta.ejb.LocalHome;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

import org.jboss.as.test.integration.ejb.home.localhome.SimpleLocalHome;

/**
 * @author Stuart Douglas
 */
@Stateless
@LocalHome(SimpleLocalHome.class)
public class SimpleStatelessLocalBean {

    @Resource
    private SessionContext sessionContext;

    private String message;

    //this should be treated as a post construct method
    public void ejbCreate() {
        message = "Hello World";
    }

    public String sayHello() {
        return message;
    }

    public String otherMethod() {
        return ((SimpleLocalHome) sessionContext.getEJBLocalHome()).createSimple().sayHello();
    }


}
