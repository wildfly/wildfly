/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.home.remotehome.descriptor;

import java.rmi.RemoteException;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;

import org.jboss.as.test.integration.ejb.home.remotehome.SimpleInterface;

/**
 * @author Stuart Douglas
 */
public class SimpleStatefulBean {

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

    public String otherMethod() throws RemoteException {
        return ((SimpleInterface)sessionContext.getEJBObject()).sayHello();
    }
}
