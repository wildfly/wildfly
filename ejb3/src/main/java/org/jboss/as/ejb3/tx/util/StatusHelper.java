/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.tx.util;

import jakarta.transaction.Status;

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
     * @see jakarta.transaction.Status
     */
    public static String statusAsString(int status) {
        if (status >= Status.STATUS_ACTIVE && status <= Status.STATUS_ROLLING_BACK) {
            return TxStatusStrings[status];
        } else {
            return "STATUS_INVALID(" + status + ")";
        }
    }
}
