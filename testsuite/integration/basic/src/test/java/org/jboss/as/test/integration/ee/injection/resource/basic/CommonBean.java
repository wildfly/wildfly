/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.basic;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

/**
 * User: jpai
 */
@Stateless
@LocalBean
public class CommonBean {

    public static final String HELLO_GREETING_PREFIX = "Hello ";

    public String sayHello(String user) {
        return HELLO_GREETING_PREFIX + user;
    }
}
