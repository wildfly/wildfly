/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.util;

/**
 * A ParserException.
 *
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
public class ParserException extends Exception {

    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create a new ParserException.
     */
    public ParserException() {
        super();
    }

    /**
     * Create a new ParserException.
     *
     * @param message a message
     * @param cause   a cause
     */
    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new ParserException.
     *
     * @param message a message
     */
    public ParserException(String message) {
        super(message);
    }

    /**
     * Create a new ParserException.
     *
     * @param cause a cause
     */
    public ParserException(Throwable cause) {
        super(cause);
    }

}
