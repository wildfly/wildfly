/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.cdi;

import jakarta.inject.Named;

/**
 * @author Stuart Douglas
 */
@Named
public class MyBean {

    public static final String MESSAGE = "Hello World";

    public String getMessage() {
        return MESSAGE;
    }

}
