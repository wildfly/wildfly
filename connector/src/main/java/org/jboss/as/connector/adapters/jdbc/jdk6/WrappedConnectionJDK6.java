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
import org.jboss.as.connector.adapters.jdbc.WrappedCallableStatement;
import org.jboss.as.connector.adapters.jdbc.WrappedConnection;
import org.jboss.as.connector.adapters.jdbc.WrappedPreparedStatement;
import org.jboss.as.connector.adapters.jdbc.WrappedStatement;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * WrappedConnectionJDK6.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 85945 $
 */
@SuppressWarnings("unchecked")
public class WrappedConnectionJDK6 extends WrappedConnection {
    private static final long serialVersionUID = 1L;

    /**
     * Create a new WrappedConnectionJDK6.
     *
     * @param mc       the managed connection
     * @param spy      The spy value
     * @param jndiName The jndi name
     */
    public WrappedConnectionJDK6(BaseWrapperManagedConnection mc, boolean spy, String jndiName) {
        super(mc, spy, jndiName);
    }

    /**
     * Wrap statement
     *
     * @param statement The statement
     * @param spy       The spy value
     * @param jndiName  The jndi name
     * @return The result
     */
    protected WrappedStatement wrapStatement(Statement statement, boolean spy, String jndiName) {
        return new WrappedStatementJDK6(this, statement, spy, jndiName);
    }

    /**
     * Wrap prepared statement
     *
     * @param statement The statement
     * @param spy       The spy value
     * @param jndiName  The jndi name
     * @return The result
     */
    protected WrappedPreparedStatement wrapPreparedStatement(PreparedStatement statement, boolean spy, String jndiName) {
        return new WrappedPreparedStatementJDK6(this, statement, spy, jndiName);
    }

    /**
     * Wrap callable statement
     *
     * @param statement The statement
     * @param spy       The spy value
     * @param jndiName  The jndi name
     * @return The result
     */
    protected WrappedCallableStatement wrapCallableStatement(CallableStatement statement, boolean spy, String jndiName) {
        return new WrappedCallableStatementJDK6(this, statement, spy, jndiName);
    }
}
