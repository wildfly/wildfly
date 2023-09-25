/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.util;

/**
 * Commands used to control controller actions.
 */
public enum ServiceCommand {
    DO_COMMIT,
    VOTE_ROLLBACK,
    VOTE_ROLLBACK_PRE_PREPARE,
    VOTE_READONLY_DURABLE,
    VOTE_READONLY_VOLATILE,
    ROLLBACK_ONLY,
    APPLICATION_EXCEPTION,
    SYSTEM_EXCEPTION_ON_COMPLETE,
    DO_COMPLETE,
    CANNOT_COMPLETE,
    REUSE_BA_PARTICIPANT;

    /**
     * Utility method which just check array on existence of a ServiceCommand
     */
    public static boolean isPresent(ServiceCommand expectedServiceCommand, ServiceCommand[] serviceCommands) {
        for (ServiceCommand foundServiceCommand : serviceCommands) {
            if (foundServiceCommand == expectedServiceCommand) {
                return true;
            }
        }
        return false;
    }
}
