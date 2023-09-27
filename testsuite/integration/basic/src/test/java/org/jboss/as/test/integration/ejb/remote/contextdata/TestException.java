/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.contextdata;

import java.io.Serializable;

/**
 * @author Brad Maxwell
 *
 */
public class TestException extends Exception implements Serializable {

    /**
     * @param message
     */
    public TestException(String message) {
        super(message);
    }
}