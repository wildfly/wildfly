/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.xts;

/**
 * @author paul.robinson@redhat.com, 2012-02-06
 */
public class XTSException extends Exception {

    public XTSException(String message) {
        super(message);
    }

    public XTSException(String message, Throwable cause) {
        super(message, cause);
    }
}
