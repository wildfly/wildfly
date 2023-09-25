/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.home.localhome.descriptor;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;

import org.jboss.as.test.integration.ejb.home.localhome.SimpleLocalHome;

/**
 * @author Stuart Douglas
 */
public class SimpleStatelessLocalBean  {

    @Resource
    private SessionContext sessionContext;

    public String sayHello() {
        return "Hello World";
    }

    public String otherMethod() {
        return  ((SimpleLocalHome)sessionContext.getEJBLocalHome()).createSimple().sayHello();
    }


}
