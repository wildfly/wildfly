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

package org.jboss.as.connector.adapters.jdbc.jdk6;

import org.jboss.as.connector.adapters.jdbc.BaseWrapperManagedConnection;
import org.jboss.as.connector.adapters.jdbc.CachedCallableStatement;
import org.jboss.as.connector.adapters.jdbc.CachedPreparedStatement;
import org.jboss.as.connector.adapters.jdbc.WrappedConnection;
import org.jboss.as.connector.adapters.jdbc.WrappedConnectionFactory;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * WrappedConnectionFactoryJDK6
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 85945 $
 */
public class WrappedConnectionFactoryJDK6 implements WrappedConnectionFactory {
    /**
     * Constructor
     */
    public WrappedConnectionFactoryJDK6() {
    }

    /**
     * Wrap connection
     *
     * @param mc       The managed connection
     * @param spy      The spy value
     * @param jndiName The jndi name
     * @return The result
     */
    public WrappedConnection createWrappedConnection(BaseWrapperManagedConnection mc, boolean spy, String jndiName) {
        return new WrappedConnectionJDK6(mc, spy, jndiName);
    }

    /**
     * {@inheritDoc}
     */
    public CachedPreparedStatement createCachedPreparedStatement(PreparedStatement ps) throws SQLException {
        return new CachedPreparedStatementJDK6(ps);
    }

    /**
     * {@inheritDoc}
     */
    public CachedCallableStatement createCachedCallableStatement(CallableStatement cs) throws SQLException {
        return new CachedCallableStatementJDK6(cs);
    }
}
