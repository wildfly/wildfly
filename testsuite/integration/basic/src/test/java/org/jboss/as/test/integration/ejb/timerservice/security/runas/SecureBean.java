/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.timerservice.security.runas;

/**
 * @author Tomasz Adamski
 */

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

@Stateless
public class SecureBean {

    private static volatile boolean secureMethodRun = false;

    @Resource
    private SessionContext ctx;

    @RolesAllowed("bob")
    public void secureMethod() {
        secureMethodRun=true;
    }

    public static boolean getSecureMethodRun(){
        return secureMethodRun;
    }
}
