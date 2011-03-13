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

package org.jboss.as.connector.adapters.jdbc.extensions.oracle;

import org.jboss.as.connector.adapters.jdbc.spi.ExceptionSorter;

import java.io.Serializable;
import java.sql.SQLException;

/**
 * OracleExceptionSorter.java
 * <p/>
 * <p/>
 * Created: Fri Mar 14 21:54:23 2003
 *
 * @author <a href="mailto:an_test@mail.ru">Andrey Demchenko</a>
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @author <a href="mailto:weston.price@jboss.com">Weston Price</a>
 * @version 1.0
 */
public class OracleExceptionSorter implements ExceptionSorter, Serializable {
    private static final long serialVersionUID = 573723525408205079L;

    /**
     * Constructor
     */
    public OracleExceptionSorter() {
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExceptionFatal(final SQLException e) {
        final int errorCode = Math.abs(e.getErrorCode());

        if ((errorCode == 28)      //session has been killed
                || (errorCode == 600)    //Internal oracle error
                || (errorCode == 1012)    //not logged on
                || (errorCode == 1014)    //Oracle shutdown in progress
                || (errorCode == 1033)    //Oracle initialization or shutdown in progress
                || (errorCode == 1034)    //Oracle not available
                || (errorCode == 1035)    //ORACLE only available to users with RESTRICTED SESSION privilege
                || (errorCode == 1089)    //immediate shutdown in progress - no operations are permitted
                || (errorCode == 1090)    //shutdown in progress - connection is not permitted
                || (errorCode == 1092)    //ORACLE instance terminated. Disconnection forced
                || (errorCode == 1094)    //ALTER DATABASE CLOSE in progress. Connections not permitted
                || (errorCode == 2396)    //exceeded maximum idle time, please connect again
                || (errorCode == 3106)    //fatal two-task communication protocol error
                || (errorCode == 3111)    //break received on communication channel
                || (errorCode == 3113)    //end-of-file on communication channel
                || (errorCode == 3114)    //not connected to ORACLE
                || (errorCode >= 12100 && errorCode <= 12299)   // TNS issues
                || (errorCode == 17002)    //connection reset
                || (errorCode == 17008)    //connection closed
                || (errorCode == 17410)    //No more data to read from socket
                || (errorCode == 17447)    //OALL8 is in an inconsistent state
                ) {
            return true;
        }

        final String errorText = (e.getMessage()).toUpperCase();

        // Exclude oracle user defined error codes (20000 through 20999) from consideration when looking for
        // certain strings.

        if ((errorCode < 20000 || errorCode >= 21000) &&
                ((errorText.indexOf("SOCKET") > -1)     //for control socket error
                        || (errorText.indexOf("CONNECTION HAS ALREADY BEEN CLOSED") > -1)
                        || (errorText.indexOf("BROKEN PIPE") > -1))) {
            return true;
        }

        return false;
    }
}
