/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jsf.phaselistener.injectiontarget.bean;

public class SimpleBean {

    public static String MESSAGE = "SimpleBean Injected";

    public String ping(){
        return MESSAGE;
    }

}
