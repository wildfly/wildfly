/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.view.basic;

/**
 * @author Stuart Douglas
 */
public class NoInterfaceSuperclass {

    protected String sayHello() {
        return "Hello";
    }

    String sayGoodbye() {
        return "Goodbye";
    }

}
