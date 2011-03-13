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

package org.jboss.as.connector.adapters.jdbc.extensions.mssql;

import org.jboss.as.connector.adapters.jdbc.spi.ValidConnectionChecker;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.jboss.logging.Logger;

/**
 * A MSSQLValidConnectionChecker.
 *
 * @author <a href="weston.price@jboss.com">Weston Price</a>
 * @version $Revision: 85945 $
 */
public class MSSQLValidConnectionChecker implements ValidConnectionChecker, Serializable {
    private static final String QUERY = "SELECT 1";
    private static Logger log = Logger.getLogger(MSSQLValidConnectionChecker.class);

    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = 3995516551833725723L;

    /**
     * Constructor
     */
    public MSSQLValidConnectionChecker() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SQLException isValidConnection(final Connection c) {
        SQLException sqe = null;
        Statement stmt = null;

        try {
            stmt = c.createStatement();
            stmt.execute(QUERY);
        } catch (SQLException e) {
            log.warn("warning: connection validation failed for current managed connection.");
            sqe = e;
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                log.warn("simple close failed for managed connection", e);
            }
        }

        return sqe;
    }
}
