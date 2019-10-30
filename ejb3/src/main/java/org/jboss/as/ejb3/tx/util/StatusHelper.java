/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.tx.util;

import javax.transaction.Status;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class StatusHelper {
    /**
     * Transaction Status Strings
     */
    private static final String[] TxStatusStrings =
            {
                    "STATUS_ACTIVE",
                    "STATUS_MARKED_ROLLBACK",
                    "STATUS_PREPARED",
                    "STATUS_COMMITTED",
                    "STATUS_ROLLEDBACK",
                    "STATUS_UNKNOWN",
                    "STATUS_NO_TRANSACTION",
                    "STATUS_PREPARING",
                    "STATUS_COMMITTING",
                    "STATUS_ROLLING_BACK"
            };

    /**
     * Converts a tx Status index to a String
     *
     * @param status the Status index
     * @return status as String or "STATUS_INVALID(value)"
     * @see javax.transaction.Status
     */
    public static String statusAsString(int status) {
        if (status >= Status.STATUS_ACTIVE && status <= Status.STATUS_ROLLING_BACK) {
            return TxStatusStrings[status];
        } else {
            return "STATUS_INVALID(" + status + ")";
        }
    }
}
