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

import org.jboss.as.connector.adapters.jdbc.spi.ValidConnectionChecker;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Implements check valid connection sql
 *
 * @author <a href="mailto:adrian@jboss.org">Adrian Brock</a>
 * @version $Revision: 71554 $
 */
public class CheckValidConnectionSQL implements ValidConnectionChecker, Serializable {
    private static final long serialVersionUID = -222752863430216887L;

    private String sql;

    /**
     * Constructor
     */
    public CheckValidConnectionSQL() {
    }

    /**
     * Constructor
     *
     * @param sql The SQL string
     */
    public CheckValidConnectionSQL(String sql) {
        this.sql = sql;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SQLException isValidConnection(Connection c) {
        if (sql == null)
            return null;

        try {
            Statement s = c.createStatement();
            try {
                s.execute(sql);
                return null;
            } finally {
                s.close();
            }
        } catch (SQLException e) {
            return e;
        }
    }
}
