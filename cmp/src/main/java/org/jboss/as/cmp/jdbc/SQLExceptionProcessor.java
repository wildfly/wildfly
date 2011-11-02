/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cmp.jdbc;

import java.sql.SQLException;

/**
 * Default SQLExceptionProcessor.
 *
 * @author <a href="mailto:jeremy@boynes.com">Jeremy Boynes</a>
 * @jmx.mbean
 */
public final class SQLExceptionProcessor {
    private static final String DUPLICATE_CODE = "23000";

    /**
     * Return true if the exception indicates that an operation failed due to a
     * unique constraint violation. This could be from any unique constraint
     * not just the primary key.
     *
     * @param e the SQLException to process
     * @return true if it was caused by a unique constraint violation
     * @jmx.managed-operation
     */
    public boolean isDuplicateKey(SQLException e) {
        return DUPLICATE_CODE.equals(e.getSQLState());
    }
}
