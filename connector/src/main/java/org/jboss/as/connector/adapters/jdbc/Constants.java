/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.connector.adapters.jdbc;

/**
 * Constants
 *
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class Constants {
    /**
     * The spy logger category
     */
    public static final String SPY_LOGGER_CATEGORY = "jboss.jdbc.spy";

    /**
     * The spy logger prefix for a connection
     */
    public static final String SPY_LOGGER_PREFIX_CONNECTION = "Connection";

    /**
     * The spy logger prefix for a statement
     */
    public static final String SPY_LOGGER_PREFIX_STATEMENT = "Statement";

    /**
     * The spy logger prefix for a prepared statement
     */
    public static final String SPY_LOGGER_PREFIX_PREPARED_STATEMENT = "PreparedStatement";

    /**
     * The spy logger prefix for a callable statement
     */
    public static final String SPY_LOGGER_PREFIX_CALLABLE_STATEMENT = "CallableStatement";

    /**
     * The spy logger prefix for a result set
     */
    public static final String SPY_LOGGER_PREFIX_RESULTSET = "ResultSet";

    /**
     * Constructor
     */
    private Constants() {
    }
}
