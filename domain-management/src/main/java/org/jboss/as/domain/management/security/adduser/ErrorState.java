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

package org.jboss.as.domain.management.security.adduser;

import static org.jboss.as.domain.management.security.adduser.AddUser.NEW_LINE;

import org.jboss.as.domain.management.logging.DomainManagementLogger;

/**
 * State to report an error to the user, optionally a nextState can be supplied so the process can continue even though an
 * error has been reported.
 */
public class ErrorState implements State {

    private final State nextState;
    private final String errorMessage;
    private final StateValues stateValues;
    private ConsoleWrapper theConsole;

    public ErrorState(ConsoleWrapper theConsole, String errorMessage) {
        this(theConsole, errorMessage, null, null);
    }

    public ErrorState(ConsoleWrapper theConsole, String errorMessage, State nextState) {
        this(theConsole, errorMessage, nextState, null);
    }

    public ErrorState(ConsoleWrapper theConsole, String errorMessage, State nextState, StateValues stateValues) {
        this.errorMessage = errorMessage;
        this.nextState = nextState;
        this.stateValues = stateValues;
        this.theConsole = theConsole;
    }

    public State execute() {
        boolean direct = theConsole.hasConsole();
        // Errors should be output in all modes.
        printf(NEW_LINE, direct);
        printf(" * ", direct);
        printf(DomainManagementLogger.ROOT_LOGGER.errorHeader(), direct);
        printf(" * ", direct);
        printf(NEW_LINE, direct);
        printf(errorMessage, direct);
        printf(NEW_LINE, direct);
        printf(NEW_LINE, direct);
        // Throw an exception if the mode is non-interactive and there's no next state.
        if ((stateValues != null && !stateValues.isInteractive()) && nextState == null) {
            throw new AddUserFailedException(errorMessage);
        }
        return nextState;
    }

    private void printf(final String message, final boolean direct) {
        if (direct) {
            System.err.print(message);
        } else {
            theConsole.printf(message);
        }
    }

}
