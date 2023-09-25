/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.decorator;

public class DecoratedResource implements ResourceInterface {

    public String getMessage() {
        return "Hello World!";
    }
}
