/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.exception;

public class TestException extends Exception {
    private static final long serialVersionUID = 1L;

    public TestException(String message) {
        super(message);
    }
}
