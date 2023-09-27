/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.config;

/**
 * Exception indicating the required operation is disabled (temporarly or pemanently) and hence coudn't be performed.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class DisabledOperationException extends RuntimeException {

    private static final long serialVersionUID = 1773053642986195568L;

    public DisabledOperationException() {
        super();
    }

    public DisabledOperationException(String message) {
        super(message);
    }

    public DisabledOperationException(String message, Throwable cause) {
        super(message, cause);
    }

}
