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

package org.jboss.as.connector.adapters.jdbc.extensions.sybase;

import org.jboss.as.connector.adapters.jdbc.spi.ExceptionSorter;

import java.io.Serializable;
import java.sql.SQLException;

/**
 * SybaseExceptionSorter.java
 * <p/>
 * Created: Wed May 12 11:46:23 2003
 *
 * @author <a href="mailto:corby3000 at hotmail.com">Corby Page</a>
 * @author <a href="mailto:an_test@mail.ru">Andrey Demchenko</a>
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 */
public class SybaseExceptionSorter implements ExceptionSorter, Serializable {
    private static final long serialVersionUID = 3539640818722639055L;

    /**
     * Constructor
     */
    public SybaseExceptionSorter() {
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExceptionFatal(SQLException e) {
        boolean result = false;

        String errorText = (e.getMessage()).toUpperCase();

        if ((errorText.indexOf("JZ0C0") > -1) || // ERR_CONNECTION_DEAD
                (errorText.indexOf("JZ0C1") > -1)) // ERR_IOE_KILLED_CONNECTION
        {
            result = true;
        }

        return result;
    }
}
