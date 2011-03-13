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

package org.jboss.as.connector.adapters.jdbc.extensions.db2;

import org.jboss.as.connector.adapters.jdbc.spi.ValidConnectionChecker;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A DB2ValidConnectionChecker.
 *
 * @author <a href="weston.price@jboss.com">Weston Price</a>
 * @version $Revision: 71554 $
 */
public class DB2ValidConnectionChecker implements ValidConnectionChecker, Serializable {
    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = -1256537245822198702L;

    /**
     * The VALID_QUERY
     */
    private static final String VALID_QUERY = "SELECT CURRENT TIMESTAMP FROM SYSIBM.SYSDUMMY1";

    /**
     * Constructor
     */
    public DB2ValidConnectionChecker() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SQLException isValidConnection(final Connection c) {
        SQLException theResult = null;
        Statement s = null;

        try {
            s = c.createStatement();
            s.execute(VALID_QUERY);
        } catch (SQLException e) {
            theResult = e;
        } finally {
            try {
                if (s != null)
                    s.close();
            } catch (SQLException e) {
                // Ignore
            }
        }

        return theResult;
    }
}
