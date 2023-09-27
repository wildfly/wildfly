/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.base;

import java.io.Serializable;

@SuppressWarnings("serial")
public class TestApplicationException extends Exception implements Serializable {

    public TestApplicationException(String message, Throwable e) {
        super(message, e);
    }

    public TestApplicationException(String message) {
        super(message);
    }

}
