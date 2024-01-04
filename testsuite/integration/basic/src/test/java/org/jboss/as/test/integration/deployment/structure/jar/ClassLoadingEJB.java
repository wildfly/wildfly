/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.structure.jar;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * User: jpai
 */
@Stateless
public class ClassLoadingEJB {

    @Resource(lookup = "java:module/ModuleName")
    private String moduleName;

    @Resource(lookup = "java:app/AppName")
    private String appName;

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        if (className == null || className.trim().isEmpty()) {
            throw new RuntimeException("Classname parameter cannot be null or empty");
        }
        return this.getClass().getClassLoader().loadClass(className);
    }

    public String query(String name) throws NamingException {
        return new InitialContext().lookup(name).toString();
    }

    public String getResourceModuleName() {
        return moduleName;
    }

    public String getResourceAppName() {
        return appName;
    }
}
