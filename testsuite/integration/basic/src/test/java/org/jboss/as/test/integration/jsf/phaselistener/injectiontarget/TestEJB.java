/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jsf.phaselistener.injectiontarget;

import jakarta.ejb.Stateless;

@Stateless
public class TestEJB {

    public static String MESSAGE = "TestEJB Injected";

    public String ping(){
        return MESSAGE;
    }

}
