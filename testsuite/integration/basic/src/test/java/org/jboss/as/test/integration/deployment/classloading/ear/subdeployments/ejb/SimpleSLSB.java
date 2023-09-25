/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.ejb;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

/**
 * User: jpai
 */
@Stateless
@Local(EJBBusinessInterface.class)
public class SimpleSLSB implements EJBBusinessInterface {

    @Override
    public String echo(String msg) {
        return msg;
    }

    @Override
    public void loadClass(String className) throws ClassNotFoundException {
        this.getClass().getClassLoader().loadClass(className);
    }
}
