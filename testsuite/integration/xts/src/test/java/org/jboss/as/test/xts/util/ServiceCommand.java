/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
