/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.home.remotehome.annotation;

import java.rmi.RemoteException;

import jakarta.annotation.Resource;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

import org.jboss.as.test.integration.ejb.home.remotehome.SimpleHome;

/**
 * @author Stuart Douglas
 */
@Stateless
@RemoteHome(SimpleHome.class)
public class SimpleStatelessBean {

    @Resource
    private SessionContext sessionContext;

    public String sayHello() {
        return "Hello World";
    }

    public String otherMethod() throws RemoteException {
        return  ((SimpleHome)sessionContext.getEJBHome()).createSimple().sayHello();
    }


}
