/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
