/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.tx;

/**
 * @author Stuart Douglas
 */
public class TimerTransactionRolledBackException extends RuntimeException {

    public TimerTransactionRolledBackException() {
    }

    public TimerTransactionRolledBackException(final Throwable cause) {
        super(cause);
    }

    public TimerTransactionRolledBackException(final String message) {
        super(message);
    }

    public TimerTransactionRolledBackException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
