/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import java.sql.SQLException;

/**
 * A StaleConnectionException.
 *
 * @author <a href="weston.price@jboss.com">Weston Price</a>
 * @version $Revision: 71554 $
 */
public class StaleConnectionException extends SQLException {

    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = -2789276182969659546L;

    /**
     * Constructor
     */
    public StaleConnectionException() {
        super();
    }

    /**
     * Constructor
     *
     * @param reason The reason
     */
    public StaleConnectionException(String reason) {
        super(reason);
    }

    /**
     * Constructor
     *
     * @param e The SQL exception
     */
    public StaleConnectionException(SQLException e) {
        super(e.getMessage(), e.getSQLState(), e.getErrorCode());
    }

    /**
     * Constructor
     *
     * @param reason   The reason
     * @param sqlstate The SQL state
     */
    public StaleConnectionException(String reason, String sqlstate) {
        super(reason, sqlstate);
    }

    /**
     * Constructor
     *
     * @param reason     The reason
     * @param sqlstate   The SQL state
     * @param vendorCode The vendor code
     */
    public StaleConnectionException(String reason, String sqlstate, int vendorCode) {
        super(reason, sqlstate, vendorCode);
    }
}
