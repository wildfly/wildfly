/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.invokedintf;

/**
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 */
public class TestFailedException extends Exception {
    private static final long serialVersionUID = 1L;

    public TestFailedException(String message) {
        super(message);
    }
}
