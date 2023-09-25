/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.Serializable;

/**
 * @author Eduardo Martins
 */
public class TestServletRunnable implements Runnable, Serializable {

    private final String moduleName;

    public TestServletRunnable(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public void run() {
        // asserts correct class loader is set
        try {
            Thread.currentThread().getContextClassLoader().loadClass(this.getClass().getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        // asserts correct naming context is set
        final InitialContext initialContext;
        try {
            initialContext = new InitialContext();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        String moduleNameOnJNDI = null;
        try {
            moduleNameOnJNDI = (String) initialContext.lookup("java:module/ModuleName");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        if(!moduleName.equals(moduleNameOnJNDI)) {
            throw new IllegalStateException("the module name " + moduleNameOnJNDI + " is not the expected " + moduleName);
        }
    }
}
